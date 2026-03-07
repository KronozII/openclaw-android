package com.champengine.android.skill

enum class SkillCategory {
    PROGRAMMING, THREE_D, ANIMATION, GAME_DEV, PHYSICS,
    MUSIC, WRITING, RESEARCH, SCIENCE, MATH, ENGINEERING,
    MEDICINE, FINANCE, LAW, DESIGN, VIDEO, PRODUCTIVITY,
    SECURITY, NETWORKING, DATABASE, AI_ML, ROBOTICS, CUSTOM
}

data class SkillManifest(
    val id: String,
    val name: String,
    val description: String,
    val category: SkillCategory,
    val version: String = "1.0.0",
    val author: String = "ChampEngine",
    val systemPromptExtension: String = "",
    val tools: List<String> = emptyList(),
    val isBuiltin: Boolean = true,
    val isEnabled: Boolean = true,
)
