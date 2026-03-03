package com.openclaw.android.quantum

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuantumReasoner @Inject constructor() {
    private val TAG = "QuantumReasoner"

    data class ReasoningPath(
        val approach: String,
        val systemPromptModifier: String,
        val expectedQuality: Float,
    )

    private val strategies = listOf(
        ReasoningPath("First Principles",
            "Reason from absolute first principles. Break everything to its most fundamental components. Build up from axioms.",
            0.9f),
        ReasoningPath("Analogical",
            "Use powerful analogies and metaphors. Find the deepest structural similarities between this problem and solved problems.",
            0.8f),
        ReasoningPath("Adversarial",
            "Steel-man every position. Find the strongest objections to your own reasoning. Identify and destroy weak points.",
            0.85f),
        ReasoningPath("Synthetic",
            "Synthesize insights from multiple domains. Look for cross-domain patterns. Combine disparate fields in novel ways.",
            0.95f),
        ReasoningPath("Socratic",
            "Use the Socratic method. Ask probing questions. Expose hidden assumptions. Guide toward truth through systematic inquiry.",
            0.75f),
    )

    fun getReasoningModifier(query: String): String {
        val selected = selectBestStrategy(query)
        Log.i(TAG, "Selected strategy: ${selected.approach}")
        return "\n\nREASONING STRATEGY: ${selected.approach}\n${selected.systemPromptModifier}"
    }

    private fun selectBestStrategy(query: String): ReasoningPath {
        val q = query.lowercase()
        return when {
            q.contains(Regex("why|explain|understand")) -> strategies[0]
            q.contains(Regex("create|design|build|invent")) -> strategies[3]
            q.contains(Regex("solve|fix|debug|error")) -> strategies[0]
            q.contains(Regex("compare|difference|vs|better")) -> strategies[2]
            else -> strategies.maxByOrNull { it.expectedQuality }!!
        }
    }
}
