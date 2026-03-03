package com.openclaw.android.agent.evolution

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvolutionEngine @Inject constructor() {
    private val TAG = "EvolutionEngine"

    enum class Signal { POSITIVE, NEGATIVE, NEUTRAL, REGENERATE, SAVE }

    data class FeedbackSignal(val sessionId: String, val signal: Signal, val context: String = "")

    private val _generationNumber = MutableStateFlow(0)
    val generationNumber: StateFlow<Int> = _generationNumber

    private val _fitnessScore = MutableStateFlow(0.5f)
    val fitnessScore: StateFlow<Float> = _fitnessScore

    private val feedbackHistory = mutableListOf<FeedbackSignal>()

    fun recordFeedback(signal: FeedbackSignal) {
        feedbackHistory.add(signal)
        _fitnessScore.value = when (signal.signal) {
            Signal.POSITIVE, Signal.SAVE -> minOf(1.0f, _fitnessScore.value + 0.02f)
            Signal.NEGATIVE, Signal.REGENERATE -> maxOf(0.0f, _fitnessScore.value - 0.03f)
            Signal.NEUTRAL -> _fitnessScore.value
        }
        Log.i(TAG, "Feedback: ${signal.signal}, fitness: ${_fitnessScore.value}")
    }

    fun evolve(): String {
        _generationNumber.value++
        val positive = feedbackHistory.count { it.signal == Signal.POSITIVE || it.signal == Signal.SAVE }
        val total = feedbackHistory.size
        return if (total == 0) "Gen ${_generationNumber.value}: No feedback yet. Rate responses to begin evolving."
        else {
            val rate = (positive.toFloat() / total * 100).toInt()
            "Gen ${_generationNumber.value}: $rate% success rate. Fitness: ${"%.2f".format(_fitnessScore.value)}."
        }
    }

    fun getAdaptiveSystemPrompt(base: String): String {
        val adaptation = when {
            _fitnessScore.value > 0.8f -> "\n\nYour responses have been highly rated. Maintain quality: be precise and deeply insightful."
            _fitnessScore.value < 0.3f -> "\n\nFocus on improvement: give clearer, more detailed responses. Double-check your reasoning."
            else -> ""
        }
        return base + adaptation
    }
}
