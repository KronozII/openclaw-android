package com.openclaw.android.agent.orchestrator

import android.util.Log
import com.openclaw.android.agent.primary.PrimaryAgent
import com.openclaw.android.context.ContextIntelligence
import com.openclaw.android.memory.LongTermMemory
import com.openclaw.android.quantum.QuantumReasoner
import com.openclaw.android.skill.SkillEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrchestratorAgent @Inject constructor(
    private val primaryAgent: PrimaryAgent,
    private val skillEngine: SkillEngine,
    private val memory: LongTermMemory,
    private val context: ContextIntelligence,
    private val quantumReasoner: QuantumReasoner,
) {
    private val TAG = "OrchestratorAgent"

    fun buildEnhancedPrompt(query: String, sessionId: String): String {
        skillEngine.detectAndActivateSkills(query)
        val basePrompt = PrimaryAgent.DEFAULT_SYSTEM_PROMPT
        val withPersonalization = context.buildPersonalizedSystemPrompt(basePrompt)
        val withSkills = skillEngine.getActiveSystemPrompt(withPersonalization)
        val withReasoning = withSkills + quantumReasoner.getReasoningModifier(query)
        val memoryContext = memory.buildMemoryContext(query)
        return buildString {
            append(withReasoning)
            if (memoryContext.isNotBlank()) append("\n\n$memoryContext")
            append("\n\nUser: $query")
        }
    }
}
