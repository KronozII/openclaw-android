package com.champengine.android.agent.primary

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.champengine.android.permission.PermissionVault
import com.champengine.android.permission.ScopeType
import com.champengine.android.storage.db.AuditLogDao
import com.champengine.android.storage.db.ChatDao
import com.champengine.android.storage.models.AuditLogEntry
import com.champengine.android.storage.models.ChatMessageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
    data class GenerationComplete(val full: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    data class PermissionRequired(val scopeType: ScopeType, val resource: String) : AgentEvent()
    data class ThreatDetected(val reason: String) : AgentEvent()
}

enum class OnDeviceModel(val displayName: String, val fileName: String) {
    GEMMA_2B("Gemma 2B (GPU int4)", "gemma-2b-it-gpu-int4.bin"),
    GEMMA_2B_CPU("Gemma 2B (CPU)", "gemma-2b-it-cpu-int4.bin"),
    PHI3_MINI("Phi-3 Mini", "phi-3-mini-int4.bin"),
}

@Singleton
class PrimaryAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
    private val chatDao: ChatDao,
) {
    private val TAG = "PrimaryAgent"
    private val agentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state

    val events = MutableSharedFlow<AgentEvent>(extraBufferCapacity = 64)

    private var llmInference: LlmInference? = null
    private var currentModel: OnDeviceModel? = null
    private var isFrozen = false

    private var onFreezeCallback: ((String) -> Unit)? = null
    private var onUnfreezeCallback: (() -> Unit)? = null

    fun registerFreezeCallbacks(onFreeze: (String) -> Unit, onUnfreeze: () -> Unit) {
        onFreezeCallback = onFreeze
        onUnfreezeCallback = onUnfreeze
    }

    fun loadModel(model: OnDeviceModel) {
        agentScope.launch {
            _state.value = AgentState.LoadingModel
            try {
                val modelPath = context.filesDir.absolutePath + "/models/" + model.fileName
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .build()
                llmInference?.close()
                llmInference = LlmInference.createFromOptions(context, options)
                currentModel = model
                _state.value = AgentState.Ready(model)
                Log.i(TAG, "Model loaded: ${model.displayName}")
            } catch (e: Exception) {
                _state.value = AgentState.Error("Failed to load model: ${e.message}")
                Log.e(TAG, "Model load error", e)
            }
        }
    }

    fun generate(userMessage: String, sessionId: String) {
        if (isFrozen) {
            agentScope.launch { events.emit(AgentEvent.Error("Agent is frozen")) }
            return
        }
        val inference = llmInference ?: run {
            agentScope.launch { events.emit(AgentEvent.Error("No model loaded")) }
            return
        }
        agentScope.launch {
            _state.value = AgentState.Generating("")
            val prompt = buildPrompt(DEFAULT_SYSTEM_PROMPT, userMessage)
            try {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "user",
                        content = userMessage,
                        timestamp = System.currentTimeMillis(),
                    )
                )
                val fullResponse = inference.generateResponse(prompt)
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
                events.emit(AgentEvent.GenerationComplete(fullResponse))
            } catch (e: Exception) {
                _state.value = AgentState.Error(e.message ?: "Generation failed")
                events.emit(AgentEvent.Error(e.message ?: "Unknown error"))
                Log.e(TAG, "Generation error", e)
            }
        }
    }

    fun freeze(reason: String) {
        isFrozen = true
        _state.value = AgentState.Frozen(reason)
        onFreezeCallback?.invoke(reason)
        Log.w(TAG, "Agent FROZEN: $reason")
    }

    fun unfreeze() {
        isFrozen = false
        val model = currentModel
        if (model != null) {
            _state.value = AgentState.Ready(model)
        } else {
            _state.value = AgentState.Idle
        }
        onUnfreezeCallback?.invoke()
        Log.i(TAG, "Agent UNFROZEN")
    }

    suspend fun requestPrivilegedAction(
        scopeType: ScopeType,
        resource: String,
        reason: String,
    ): Boolean {
        val token = vault.requireScope(scopeType, resource, reason, "PrimaryAgent")
        return token != null
    }

    fun close() {
        llmInference?.close()
        agentScope.cancel()
    }

    private fun buildPrompt(system: String, user: String): String {
        return "<start_of_turn>user\n$system\n\n$user<end_of_turn>\n<start_of_turn>model\n"
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are OpenClaw, a helpful on-device AI assistant."
    }
}
