package com.champengine.android.context

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextIntelligence @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "ContextIntelligence"
    private val profileFile = File(context.filesDir, "context/user_profile.json")

    enum class DetailLevel { BRIEF, MEDIUM, DETAILED, EXHAUSTIVE }
    enum class VocabularyLevel { CASUAL, INTERMEDIATE, TECHNICAL, EXPERT }

    data class UserProfile(
        val preferredDetailLevel: DetailLevel = DetailLevel.MEDIUM,
        val vocabularyLevel: VocabularyLevel = VocabularyLevel.TECHNICAL,
        val prefersBulletPoints: Boolean = false,
        val prefersExamples: Boolean = true,
        val domainExpertise: MutableMap<String, Float> = mutableMapOf(),
        val mostActiveHour: Int = 20,
        val avgSessionMinutes: Int = 15,
        val totalSessions: Int = 0,
        val totalMessages: Int = 0,
        val recurringTopics: MutableList<String> = mutableListOf(),
        val unfinishedGoals: MutableList<String> = mutableListOf(),
        val usesCasualLanguage: Boolean = false,
    )

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile
    private var sessionStartTime = System.currentTimeMillis()

    init { loadProfile() }

    fun onUserMessage(message: String) {
        val profile = _profile.value
        val technicalWords = countTechnicalWords(message)
        val newVocabLevel = when {
            technicalWords > 5 -> VocabularyLevel.EXPERT
            technicalWords > 2 -> VocabularyLevel.TECHNICAL
            message.length > 200 -> VocabularyLevel.INTERMEDIATE
            else -> VocabularyLevel.CASUAL
        }
        val domains = detectDomains(message)
        val isExpertQuestion = technicalWords > 3 && message.length > 100
        domains.forEach { domain ->
            val current = profile.domainExpertise[domain] ?: 0.3f
            profile.domainExpertise[domain] = (current + if (isExpertQuestion) 0.05f else -0.02f).coerceIn(0f, 1f)
        }
        _profile.value = profile.copy(
            vocabularyLevel = newVocabLevel,
            totalMessages = profile.totalMessages + 1,
        )
        saveProfile()
    }

    fun buildPersonalizedSystemPrompt(base: String): String {
        val p = _profile.value
        return buildString {
            append(base)
            append("\n\n## PERSONALIZATION\n")
            append("Vocabulary level: ${p.vocabularyLevel.name.lowercase()}. ")
            append("Detail preference: ${p.preferredDetailLevel.name.lowercase()}.\n")
            if (p.domainExpertise.isNotEmpty()) {
                val top = p.domainExpertise.entries.sortedByDescending { it.value }.take(5)
                append("Domain expertise: ")
                append(top.joinToString(", ") { (d, v) -> "$d (${expertiseLabel(v)})" })
                append(". Calibrate depth accordingly.\n")
            }
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            append("Time context: ${timeContext(hour)}.\n")
            if (p.recurringTopics.isNotEmpty()) {
                append("Recurring interests: ${p.recurringTopics.take(5).joinToString(", ")}.\n")
            }
        }
    }

    fun onSessionEnd() {
        val duration = ((System.currentTimeMillis() - sessionStartTime) / 60_000).toInt()
        val p = _profile.value
        _profile.value = p.copy(
            totalSessions = p.totalSessions + 1,
            avgSessionMinutes = ((p.avgSessionMinutes * p.totalSessions) + duration) / (p.totalSessions + 1),
            mostActiveHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        )
        sessionStartTime = System.currentTimeMillis()
        saveProfile()
    }

    private fun expertiseLabel(level: Float) = when {
        level > 0.8f -> "expert"; level > 0.6f -> "proficient"
        level > 0.4f -> "intermediate"; level > 0.2f -> "beginner"; else -> "novice"
    }

    private fun timeContext(hour: Int) = when (hour) {
        in 5..8 -> "early morning — be energizing and clear"
        in 9..11 -> "morning — user likely in productive mode"
        in 12..13 -> "midday — keep it efficient"
        in 14..17 -> "afternoon — deep work time"
        in 18..21 -> "evening — exploring or winding down"
        else -> "late night — be concise"
    }

    private fun countTechnicalWords(message: String): Int {
        val technical = setOf("function","class","interface","algorithm","complexity","async",
            "coroutine","tensor","gradient","neural","protocol","encryption","recursive",
            "polymorphism","quaternion","eigenvalue","manifold","topology","derivative",
            "integral","stochastic","heuristic")
        return message.lowercase().split(Regex("\\W+")).count { it in technical }
    }

    private fun detectDomains(message: String): List<String> {
        val domains = mutableListOf<String>()
        val m = message.lowercase()
        if (m.contains(Regex("python|kotlin|java|code|function|class|debug"))) domains.add("programming")
        if (m.contains(Regex("3d|mesh|render|shader|blender|model"))) domains.add("3d_graphics")
        if (m.contains(Regex("game|unity|godot|physics|collision"))) domains.add("game_dev")
        if (m.contains(Regex("math|calculus|equation|proof|theorem"))) domains.add("mathematics")
        if (m.contains(Regex("machine learning|neural|model|training|ai"))) domains.add("ai_ml")
        if (m.contains(Regex("music|chord|melody|rhythm|compose"))) domains.add("music")
        if (m.contains(Regex("design|ui|ux|layout|typography"))) domains.add("design")
        if (m.contains(Regex("security|encrypt|vulnerability|hack"))) domains.add("security")
        return domains.distinct()
    }

    private fun loadProfile() {
        try {
            profileFile.parentFile?.mkdirs()
            if (!profileFile.exists()) return
            val json = JSONObject(profileFile.readText())
            val expertise = mutableMapOf<String, Float>()
            json.optJSONObject("domainExpertise")?.let { obj ->
                obj.keys().forEach { key -> expertise[key] = obj.getDouble(key).toFloat() }
            }
            _profile.value = _profile.value.copy(
                totalSessions = json.optInt("totalSessions", 0),
                totalMessages = json.optInt("totalMessages", 0),
                avgSessionMinutes = json.optInt("avgSessionMinutes", 15),
                mostActiveHour = json.optInt("mostActiveHour", 20),
                domainExpertise = expertise,
            )
        } catch (e: Exception) { Log.e(TAG, "Error loading profile", e) }
    }

    private fun saveProfile() {
        try {
            val p = _profile.value
            val json = JSONObject().apply {
                put("totalSessions", p.totalSessions); put("totalMessages", p.totalMessages)
                put("avgSessionMinutes", p.avgSessionMinutes); put("mostActiveHour", p.mostActiveHour)
                put("domainExpertise", JSONObject().apply { p.domainExpertise.forEach { (k, v) -> put(k, v) } })
            }
            profileFile.parentFile?.mkdirs()
            profileFile.writeText(json.toString(2))
        } catch (e: Exception) { Log.e(TAG, "Error saving profile", e) }
    }
}
