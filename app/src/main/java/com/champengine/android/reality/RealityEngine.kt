package com.champengine.android.reality

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealityEngine @Inject constructor() {
    private val TAG = "RealityEngine"

    data class RealityContext(
        val detectedObjects: List<String> = emptyList(),
        val detectedText: String = "",
        val sceneDescription: String = "",
        val suggestedActions: List<String> = emptyList(),
    )

    private val _realityContext = MutableStateFlow(RealityContext())
    val realityContext: StateFlow<RealityContext> = _realityContext

    fun analyzeScene(description: String, detectedText: String = "") {
        _realityContext.value = RealityContext(
            sceneDescription = description,
            detectedText = detectedText,
            suggestedActions = generateActions(description, detectedText),
        )
    }

    private fun generateActions(scene: String, text: String): List<String> {
        val combined = (scene + " " + text).lowercase()
        val actions = mutableListOf<String>()
        if (combined.contains(Regex("book|title|author"))) actions.add("Look up this book")
        if (combined.contains(Regex("plant|flower|tree"))) actions.add("Identify this plant")
        if (combined.contains(Regex("food|dish|meal"))) actions.add("Get recipe/nutrition info")
        if (combined.contains(Regex("code|error|terminal"))) actions.add("Debug this code")
        if (combined.contains(Regex("math|equation|formula"))) actions.add("Solve this equation")
        if (combined.contains(Regex("circuit|component|pcb"))) actions.add("Identify components")
        return actions.take(4)
    }

    fun buildVisionPrompt(userQuery: String): String {
        val ctx = _realityContext.value
        return buildString {
            if (ctx.sceneDescription.isNotBlank()) append("VISUAL CONTEXT: ${ctx.sceneDescription}\n")
            if (ctx.detectedText.isNotBlank()) append("TEXT IN IMAGE: ${ctx.detectedText}\n")
            append("\nUser question: $userQuery")
        }
    }
}
