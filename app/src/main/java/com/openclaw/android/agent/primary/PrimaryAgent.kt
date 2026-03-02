package com.openclaw.android.agent.primary

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.openclaw.android.agent.sentinel.SentinelAgent
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.db.ChatDao
import com.openclaw.android.storage.models.AuditLogEntry
import com.openclaw.android.storage.models.ChatMessageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device model options
 */
enum class OnDeviceModel(val fileName: String, val displayName: String, val ramRequiredMb: Int) {
    GEMMA_2B("gemma-2b-it-gpu-int4.bin", "Gemma 2 2B (recommended)", 2048),
    PHI3_MINI("phi-3-mini-4k-instruct.bin", "Phi-3 Mini (low RAM)", 1024),
    GEMMA_2B_CPU("gemma-2b-it-cpu-int4.bin", "Gemma 2 2B (CPU only)", 2048),
}

sealed class AgentState {
    object Idle : AgentState()
    object LoadingModel : AgentState()
    data class Ready(val model: OnDeviceModel) : AgentState()
    data class Generating(val partial: String) : AgentState()
    data class Frozen(val reason: String) : AgentState()
    data class Error(val message: String) : AgentState()
}

sealed class AgentEvent {
    data class TokenOutput(val token: String) : AgentEvent()
    data class GenerationComplete(val fullResponse: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    data class PermissionRequired(val requestId: String) : AgentEvent()
    data class ThreatDetected(val alertId: String) : AgentEvent()
}

/**
 * PrimaryAgent — orchestrates on-device inference with permission checks.
 *
 * Zero network calls for inference. Every privileged action goes through PermissionVault.
 * Sentinel can freeze this agent at any time via the agentFrozen StateFlow.
 */
@Singleton
class PrimaryAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vault: PermissionVault,
    private val sentinel: SentinelAgent,
    private val chatDao: ChatDao,
    private val auditLogDao: AuditLogDao,
) {
    private val TAG = "PrimaryAgent"

    // Isolated scope for inference — never shares threads with Sentinel
    private val agentScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AgentEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    private var llmInference: LlmInference? = null
    private var currentModel: OnDeviceModel? = null

    // Freeze/unfreeze callbacks registered with Sentinel
    init {
        sentinel.onFreezeRequested = { agentScope.launch { freeze("Sentinel threat detected") } }
        sentinel.onUnfreezeRequested = { agentScope.launch { unfreeze() } }
    }

    /**
     * Load an on-device model from the app's files directory.
     * Model must be placed in /data/data/com.openclaw.android/files/models/
     */
    suspend fun loadModel(model: OnDeviceModel) = withContext(Dispatchers.IO) {
        _state.value = AgentState.LoadingModel
        try {
            val modelPath = File(context.filesDir, "models/${model.fileName}")
            if (!modelPath.exists()) {
                _state.value = AgentState.Error("Model not found: ${model.fileName}. Please download it in Settings.")
                return@withContext
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath.absolutePath)
                .setMaxTokens(2048)
                .setTopK(40)
                .setTemperature(0.8f)
                .setRandomSeed(42)
                .build()

            llmInference?.close()
            llmInference = LlmInference.createFromOptions(context, options)
            currentModel = model
            _state.value = AgentState.Ready(model)
            Log.i(TAG, "Model loaded: ${model.displayName}")
        } catch (e: Exception) {
            _state.value = AgentState.Error("Failed to load model: ${e.message}")
            Log.e(TAG, "Model load failed", e)
        }
    }

    /**
     * Generate a response to a user message.
     *
     * Flow:
     * 1. Scan input for injection
     * 2. Check agent is not frozen
     * 3. Run inference (streaming tokens)
     * 4. Persist to chat DB
     * 5. Log to audit trail
     */
    suspend fun generate(
        sessionId: String,
        userMessage: String,
        systemContext: String = DEFAULT_SYSTEM_PROMPT,
    ) = withContext(Dispatchers.Default) {

        // Step 1: Injection scan
        val scanResult = sentinel.scanForInjection(userMessage)
        if (!scanResult.isSafe) {
            Log.w(TAG, "Injection pattern detected: ${scanResult.patterns}")
            auditLogDao.insert(
                AuditLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    agentId = "primary",
                    actionType = "INPUT_SCAN",
                    resource = "user_input",
                    scopeTokenId = null,
                    outcome = "THREAT_DETECTED",
                    threatLevel = "HIGH",
                    details = "Injection patterns: ${scanResult.patterns}",
                )
            )
            _events.emit(AgentEvent.Error("Input contained patterns that could override system instructions. Message blocked for your safety."))
            return@withContext
        }

        // Step 2: Check frozen state
        if (_state.value is AgentState.Frozen) {
            _events.emit(AgentEvent.Error("Agent is paused — please review the security alert first."))
            return@withContext
        }

        val inference = llmInference
        if (inference == null) {
            _events.emit(AgentEvent.Error("No model loaded. Please load a model in Settings."))
            return@withContext
        }

        // Step 3: Persist user message
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "user",
                content = userMessage,
                timestamp = System.currentTimeMillis(),
            )
        )

        _state.value = AgentState.Generating("")
        val fullPrompt = buildPrompt(systemContext, userMessage)
        val responseBuilder = StringBuilder()

        try {
            val fullResponse = inference.generateResponse(fullPrompt)

            responseBuilder.append(fullResponse)
            _state.value = AgentState.Generating(responseBuilder.toString())
            _events.emit(AgentEvent.TokenOutput(fullResponse))

            chatDao.insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "assistant",
                    content = fullResponse,
                    timestamp = System.currentTimeMillis(),
                )
            )
            chatDao.touchSession(sessionId, System.currentTimeMillis())

            _state.value = AgentState.Ready(currentModel!!)
            _events.emit(AgentEvent.GenerationComplete(fullResponse))
        } catch (e: Exception) {
            _state.value = AgentState.Error(e.message ?: "Generation failed")
            _events.emit(AgentEvent.Error(e.message ?: "Unknown inference error"))
            Log.e(TAG, "Generation error", e)
        }

    /**
     * Request a privileged action on behalf of the model's response.
     * Always goes through PermissionVault — cannot be bypassed.
     */
    suspend fun requestPrivilegedAction(
        scopeType: ScopeType,
        resource: String,
        reason: String,
    ): Boolean {
        val token = vault.requireScope(scopeType, resource, reason, "Agent action")
        return token != null
    }

    private fun freeze(reason: String) {
        _state.value = AgentState.Frozen(reason)
        Log.w(TAG, "Agent FROZEN: $reason")
    }

    private fun unfreeze() {
        val model = currentModel
        if (model != null) {
            _state.value = AgentState.Ready(model)
        } else {
            _state.value = AgentState.Idle
        }
        Log.i(TAG, "Agent UNFROZEN")
    }

    fun close() {
        llmInference?.close()
        agentScope.cancel()
    }

    private fun buildPrompt(system: String, user: String): String {
        // Gemma 2 instruction format
        return "<start_of_turn>user\n$system\n\n$user<end_of_turn>\n<start_of_turn>model\n"
    }

    companion object {
    const val DEFAULT_SYSTEM_PROMPT = "You are OpenClaw, a helpful on-device AI assistant. " +
        "No data leaves the device unless the user explicitly permits it. " +
        "You are honest, helpful, and always ask before accessing any sensitive resource. " +
        "If you need to access the web, files, camera, contacts, or any external resource, say so clearly " +
        "and the system will ask the user for permission before proceeding. " +
        "Keep responses concise and useful."
}
}
