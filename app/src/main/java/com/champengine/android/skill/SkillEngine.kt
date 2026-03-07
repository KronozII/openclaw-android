package com.champengine.android.skill

import android.util.Log
import com.champengine.android.storage.db.AuditLogDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillEngine @Inject constructor(
    private val auditLogDao: AuditLogDao,
) {
    private val TAG = "SkillEngine"

    private val _activeSkills = MutableStateFlow<List<SkillManifest>>(BuiltinSkills.enabledSkills())
    val activeSkills: StateFlow<List<SkillManifest>> = _activeSkills

    private val _customSkills = MutableStateFlow<List<SkillManifest>>(emptyList())
    val customSkills: StateFlow<List<SkillManifest>> = _customSkills

    fun getActiveSystemPrompt(basePrompt: String): String {
        val extensions = _activeSkills.value
            .filter { it.systemPromptExtension.isNotBlank() }
            .joinToString("\n\n") { "## ${it.name} Skill\n${it.systemPromptExtension}" }
        return if (extensions.isBlank()) basePrompt
        else "$basePrompt\n\n---\n\nACTIVE SKILL MODULES:\n\n$extensions"
    }

    fun enableSkill(id: String) {
        val all = BuiltinSkills.ALL + _customSkills.value
        val skill = all.find { it.id == id } ?: return
        val current = _activeSkills.value.toMutableList()
        if (current.none { it.id == id }) {
            current.add(skill)
            _activeSkills.value = current
            Log.i(TAG, "Skill enabled: ${skill.name}")
        }
    }

    fun disableSkill(id: String) {
        _activeSkills.value = _activeSkills.value.filter { it.id != id }
        Log.i(TAG, "Skill disabled: $id")
    }

    fun installCustomSkill(skill: SkillManifest) {
        val current = _customSkills.value.toMutableList()
        current.removeAll { it.id == skill.id }
        current.add(skill)
        _customSkills.value = current
        enableSkill(skill.id)
        Log.i(TAG, "Custom skill installed: ${skill.name}")
    }

    fun buildSkillFromDescription(
        name: String,
        description: String,
        category: SkillCategory,
        systemPrompt: String,
    ): SkillManifest {
        val id = "skill_custom_${name.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}"
        return SkillManifest(
            id = id, name = name, description = description,
            category = category, systemPromptExtension = systemPrompt,
            author = "Self-Built", isBuiltin = false,
        )
    }

    fun getSkillsByCategory(): Map<SkillCategory, List<SkillManifest>> {
        return (BuiltinSkills.ALL + _customSkills.value).groupBy { it.category }
    }

    fun detectAndActivateSkills(query: String) {
        val p = query.lowercase()
        val skillMap = mapOf(
            "skill_python" to Regex("python|java|kotlin|code|function|class|algorithm|debug"),
            "skill_3d_modeling" to Regex("3d|mesh|model|geometry|vertex|polygon|blender"),
            "skill_animation" to Regex("animate|animation|keyframe|rig|bone"),
            "skill_game_engine" to Regex("game|unity|godot|level|player|collision"),
            "skill_physics_sim" to Regex("physics|simulation|rigid body|fluid|collision"),
            "skill_music_theory" to Regex("music|melody|chord|rhythm|compose|midi"),
            "skill_math" to Regex("math|calculus|equation|proof|algebra|topology"),
            "skill_ml" to Regex("machine learning|neural|train|model|inference|pytorch"),
            "skill_security" to Regex("security|encrypt|vulnerability|attack|defense"),
            "skill_ui_design" to Regex("design|ui|ux|interface|layout|typography"),
            "skill_research" to Regex("research|study|analyze|investigate|review"),
            "skill_creative_writing" to Regex("story|write|fiction|character|plot|novel"),
        )
        skillMap.forEach { (skillId, pattern) ->
            if (p.contains(pattern)) enableSkill(skillId)
        }
    }
}
