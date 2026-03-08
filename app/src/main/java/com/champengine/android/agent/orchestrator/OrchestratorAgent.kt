package com.champengine.android.agent.orchestrator

import android.util.Log
import com.champengine.android.context.ContextIntelligence
import com.champengine.android.memory.LongTermMemory
import com.champengine.android.quantum.QuantumReasoner
import com.champengine.android.skill.SkillEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrchestratorAgent @Inject constructor(
    private val skillEngine: SkillEngine,
    private val memory: LongTermMemory,
    private val context: ContextIntelligence,
    private val quantumReasoner: QuantumReasoner,
) {
    private val TAG = "OrchestratorAgent"

    fun buildEnhancedPrompt(query: String, sessionId: String): String {
        skillEngine.detectAndActivateSkills(query)
        val withPersonalization = context.buildPersonalizedSystemPrompt(DEFAULT_SYSTEM_PROMPT)
        val withSkills = skillEngine.getActiveSystemPrompt(withPersonalization)
        val withReasoning = withSkills + quantumReasoner.getReasoningModifier(query)
        val memoryContext = memory.buildMemoryContext(query)
        return buildString {
            append(withReasoning)
            if (memoryContext.isNotBlank()) append("\n\n$memoryContext")
            append("\n\nUser: $query")
        }
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are ChampEngine, a powerful private AI assistant. " +
            "You run on private infrastructure. No data is stored or shared. " +
            "Be direct, helpful, and precise."
    }
}
