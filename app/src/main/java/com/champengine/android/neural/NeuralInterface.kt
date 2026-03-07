package com.champengine.android.neural

import android.util.Log
import com.champengine.android.memory.LongTermMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeuralInterface @Inject constructor(
    private val memory: LongTermMemory,
) {
    private val TAG = "NeuralInterface"

    data class Prediction(
        val completedQuery: String,
        val confidence: Float,
        val suggestedSkills: List<String>,
        val preloadedContext: String,
    )

    private val _currentPrediction = MutableStateFlow<Prediction?>(null)
    val currentPrediction: StateFlow<Prediction?> = _currentPrediction

    fun onTyping(partialText: String): Prediction? {
        if (partialText.length < 3) return null
        val prediction = predict(partialText)
        _currentPrediction.value = prediction
        return prediction
    }

    private fun predict(partial: String): Prediction? {
        val lower = partial.lowercase()
        val (completion, skills, confidence) = when {
            lower.startsWith("write") || lower.startsWith("create") ->
                Triple("$partial a detailed and comprehensive version", listOf("skill_creative_writing"), 0.7f)
            lower.startsWith("explain") || lower.startsWith("what is") ->
                Triple("$partial with examples and analogies", listOf("skill_research"), 0.75f)
            lower.startsWith("code") || lower.startsWith("build") || lower.startsWith("implement") ->
                Triple("$partial with full implementation and comments", listOf("skill_python", "skill_kotlin"), 0.8f)
            lower.startsWith("design") ->
                Triple("$partial with detailed specifications", listOf("skill_ui_design", "skill_3d_modeling"), 0.7f)
            lower.startsWith("how") ->
                Triple("$partial step by step", listOf("skill_research"), 0.65f)
            else -> return null
        }
        val memContext = memory.recall(partial, 3).let { mems ->
            if (mems.isNotEmpty()) "Relevant context: " + mems.joinToString("; ") { it.summary } else ""
        }
        return Prediction(completedQuery = completion, confidence = confidence, suggestedSkills = skills, preloadedContext = memContext)
    }

    fun learnFromInteraction(query: String, wasPositive: Boolean) {
        Log.i(TAG, "Learned: ${query.take(30)} → positive=$wasPositive")
    }
}
