package com.champengine.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.champengine.android.agent.primary.AgentState
import com.champengine.android.agent.primary.OnDeviceModel
import com.champengine.android.agent.primary.PrimaryAgent
import com.champengine.android.agent.sandbox.ProjectType
import com.champengine.android.agent.sandbox.SandboxBuilderAgent
import com.champengine.android.agent.sentinel.SentinelAgent
import com.champengine.android.agent.sentinel.ThreatAlert
import com.champengine.android.permission.PermissionDecision
import com.champengine.android.permission.PermissionRequest
import com.champengine.android.permission.PermissionVault
import com.champengine.android.storage.db.AuditLogDao
import com.champengine.android.storage.db.ChatDao
import com.champengine.android.storage.models.ChatMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─── Chat ──────────────────────────────────────────────────────────────────

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val primaryAgent: PrimaryAgent,
    private val sentinel: SentinelAgent,
    private val vault: PermissionVault,
    private val chatDao: ChatDao,
) : ViewModel() {

    val agentState: StateFlow<AgentState> = primaryAgent.state
    val pendingRequests: StateFlow<List<PermissionRequest>> = vault.pendingRequests
    val activeAlerts: StateFlow<List<ThreatAlert>> = sentinel.activeAlerts

    private val currentSessionId = UUID.randomUUID().toString()

    val messages: StateFlow<List<ChatMessageEntity>> =
        chatDao.observeMessages(currentSessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        sentinel.startMonitoring()
        viewModelScope.launch {
            chatDao.insertSession(
                com.champengine.android.storage.models.ChatSessionEntity(
                    id = currentSessionId,
                    title = "New Chat",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun loadDefaultModel() {
        viewModelScope.launch {
            primaryAgent.loadModel(OnDeviceModel.GEMMA_2B)
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            primaryAgent.generate(
                sessionId = currentSessionId,
                userMessage = text,
            )
        }
    }

    fun resolvePermission(requestId: String, decision: PermissionDecision) {
        vault.resolveRequest(requestId, decision)
    }

    fun reviewThreat(alert: ThreatAlert) {
        // Navigate to threat review screen — handled by navigation
        // For now: acknowledge and unfreeze
        sentinel.userAcknowledgedThreat(alert.id, allowResume = true)
    }

    override fun onCleared() {
        primaryAgent.close()
    }
}

// ─── Permissions ───────────────────────────────────────────────────────────

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) : ViewModel() {

    val allTokens = vault.allTokens()
    val auditLog = auditLogDao.observeRecent()

    fun revokeToken(tokenId: String) {
        viewModelScope.launch {
            vault.revokeToken(tokenId)
        }
    }

    fun revokeAll() {
        viewModelScope.launch {
            vault.revokeAllOnThreat()
        }
    }
}

// ─── Sandbox ───────────────────────────────────────────────────────────────

@HiltViewModel
class SandboxViewModel @Inject constructor(
    private val sandboxAgent: SandboxBuilderAgent,
) : ViewModel() {

    val activeProject = sandboxAgent.activeProject
    val buildLog = sandboxAgent.buildLog
    val allProjects = sandboxAgent.allProjects

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun createProject(name: String, type: ProjectType, description: String) {
        viewModelScope.launch {
            sandboxAgent.createProject(name, type, description)
        }
    }

    fun updateFile(filename: String, content: String) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            sandboxAgent.updateFile(project, filename, content)
        }
    }

    fun generateCode(prompt: String, filename: String) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val generated = sandboxAgent.generateCode(project, prompt, filename)
                sandboxAgent.updateFile(project, filename, generated)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun exportProject() {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            sandboxAgent.exportAsZip(project)
            // Trigger share sheet via event — handled in UI layer
        }
    }
}

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val skillEngine: com.champengine.android.skill.SkillEngine,
) : ViewModel() {
    val activeSkills = skillEngine.activeSkills
    fun toggleSkill(id: String) {
        if (activeSkills.value.any { it.id == id }) skillEngine.disableSkill(id)
        else skillEngine.enableSkill(id)
    }
    fun buildCustomSkill(name: String, description: String, prompt: String) {
        val skill = skillEngine.buildSkillFromDescription(name, description, com.champengine.android.skill.SkillCategory.CUSTOM, prompt)
        skillEngine.installCustomSkill(skill)
    }
}

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memory: com.champengine.android.memory.LongTermMemory,
) : ViewModel() {
    val memories = memory.memories
    fun deleteMemory(id: String) = memory.deleteMemory(id)
    fun clearAll() = memory.clearAll()
}
