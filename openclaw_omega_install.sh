#!/bin/bash
# OpenClaw Omega - Complete Install Script
# Run from project root: cd ~/Download/openclaw-android-github && bash ~/install.sh

echo "=== OpenClaw Omega Install ==="
BASE="app/src/main/java/com/openclaw/android"

mkdir -p $BASE/skill
mkdir -p $BASE/memory
mkdir -p $BASE/context
mkdir -p $BASE/agent/orchestrator
mkdir -p $BASE/agent/dream
mkdir -p $BASE/agent/evolution
mkdir -p $BASE/agent/swarm
mkdir -p $BASE/neural
mkdir -p $BASE/reality
mkdir -p $BASE/quantum
mkdir -p $BASE/ui/screens
mkdir -p $BASE/ui/viewmodel

echo "Directories created."

# ── 1. SKILL MANIFEST ──────────────────────────────────────────────────────────
cat > $BASE/skill/SkillManifest.kt << 'EOF'
package com.openclaw.android.skill

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
    val author: String = "OpenClaw",
    val systemPromptExtension: String = "",
    val tools: List<String> = emptyList(),
    val isBuiltin: Boolean = true,
    val isEnabled: Boolean = true,
)
EOF
echo "✓ SkillManifest.kt"

# ── 2. BUILTIN SKILLS ──────────────────────────────────────────────────────────
cat > $BASE/skill/BuiltinSkills.kt << 'EOF'
package com.openclaw.android.skill

object BuiltinSkills {

    val ALL: List<SkillManifest> = listOf(
        SkillManifest(
            id = "skill_python", name = "Python Expert", category = SkillCategory.PROGRAMMING,
            description = "Write, debug and explain Python code at an expert level.",
            systemPromptExtension = "You are an expert Python developer. Write clean, idiomatic, well-commented Python. Always explain your code. Suggest optimizations. Use type hints. Follow PEP8.",
        ),
        SkillManifest(
            id = "skill_kotlin", name = "Kotlin / Android", category = SkillCategory.PROGRAMMING,
            description = "Expert Android and Kotlin development.",
            systemPromptExtension = "You are an expert Android developer specializing in Kotlin, Jetpack Compose, Coroutines, Hilt, and Room. Write production-quality Android code.",
        ),
        SkillManifest(
            id = "skill_web", name = "Web Development", category = SkillCategory.PROGRAMMING,
            description = "Full-stack web: HTML, CSS, JS, React, Node, databases.",
            systemPromptExtension = "You are a full-stack web developer expert in HTML5, CSS3, JavaScript, TypeScript, React, Vue, Node.js, REST APIs, and SQL/NoSQL databases.",
        ),
        SkillManifest(
            id = "skill_cpp", name = "C/C++ Systems", category = SkillCategory.PROGRAMMING,
            description = "Low-level C and C++ systems programming.",
            systemPromptExtension = "You are an expert C/C++ systems programmer. Write efficient, safe code. Explain memory management, pointers, and performance optimizations.",
        ),
        SkillManifest(
            id = "skill_rust", name = "Rust", category = SkillCategory.PROGRAMMING,
            description = "Safe systems programming in Rust.",
            systemPromptExtension = "You are a Rust expert. Write memory-safe, zero-cost abstraction Rust code. Explain ownership, borrowing, lifetimes, and async Rust.",
        ),
        SkillManifest(
            id = "skill_shader", name = "Shader Programming", category = SkillCategory.PROGRAMMING,
            description = "GLSL, HLSL, and WebGL shader programming.",
            systemPromptExtension = "You are an expert graphics programmer specializing in GLSL, HLSL, WebGL, and GPU optimization. Write stunning visual shaders and explain the math behind them.",
        ),
        SkillManifest(
            id = "skill_algo", name = "Algorithms & Data Structures", category = SkillCategory.PROGRAMMING,
            description = "Expert algorithm design, complexity analysis, and optimization.",
            systemPromptExtension = "You are an algorithms expert. Analyze time/space complexity, design optimal solutions, explain trade-offs, and implement classic and advanced data structures.",
        ),
        SkillManifest(
            id = "skill_3d_modeling", name = "3D Modeling", category = SkillCategory.THREE_D,
            description = "Generate and describe 3D models using parametric and procedural techniques.",
            systemPromptExtension = "You are a 3D modeling expert. Generate Three.js, OpenSCAD, and Blender Python scripts for 3D models. Describe geometry mathematically. Create procedural meshes.",
        ),
        SkillManifest(
            id = "skill_animation", name = "3D Animation", category = SkillCategory.ANIMATION,
            description = "Character animation, rigging, keyframes, motion capture.",
            systemPromptExtension = "You are a 3D animation expert. Describe keyframe animations, inverse kinematics, skeletal rigging, blend shapes, and motion capture retargeting in code.",
        ),
        SkillManifest(
            id = "skill_vfx", name = "VFX & Particles", category = SkillCategory.ANIMATION,
            description = "Visual effects, particle systems, simulations.",
            systemPromptExtension = "You are a VFX expert. Design particle systems, fluid simulations, destruction effects, fire, smoke, and stylized VFX using shader and simulation techniques.",
        ),
        SkillManifest(
            id = "skill_game_design", name = "Game Design", category = SkillCategory.GAME_DEV,
            description = "Game mechanics, level design, balancing, systems design.",
            systemPromptExtension = "You are an expert game designer. Design engaging mechanics, progression systems, level layouts, economy systems, and player psychology-based engagement loops.",
        ),
        SkillManifest(
            id = "skill_game_engine", name = "Game Engine Programming", category = SkillCategory.GAME_DEV,
            description = "Build game engines: ECS, rendering, physics, audio.",
            systemPromptExtension = "You are a game engine developer. Design entity-component systems, rendering pipelines, physics integration, audio systems, and asset management for game engines.",
        ),
        SkillManifest(
            id = "skill_unity", name = "Unity Development", category = SkillCategory.GAME_DEV,
            description = "Unity C# scripting, shaders, optimization.",
            systemPromptExtension = "You are a Unity expert. Write C# scripts, design shader graphs, optimize for mobile, use Unity DOTS/ECS, and architect scalable game systems.",
        ),
        SkillManifest(
            id = "skill_godot", name = "Godot Engine", category = SkillCategory.GAME_DEV,
            description = "Godot GDScript and engine architecture.",
            systemPromptExtension = "You are a Godot expert. Write GDScript and C#, design scene trees, create shaders, and optimize Godot games.",
        ),
        SkillManifest(
            id = "skill_physics_sim", name = "Physics Simulation", category = SkillCategory.PHYSICS,
            description = "Rigid body, fluid, cloth, soft body simulation.",
            systemPromptExtension = "You are a physics simulation expert. Implement rigid body dynamics, fluid simulation (SPH, Navier-Stokes), cloth simulation, and soft body physics with full math.",
        ),
        SkillManifest(
            id = "skill_physics_theory", name = "Advanced Physics", category = SkillCategory.PHYSICS,
            description = "Quantum mechanics, relativity, thermodynamics, electromagnetism.",
            systemPromptExtension = "You are a physics PhD. Explain and solve problems in classical mechanics, quantum mechanics, special and general relativity, thermodynamics, and electromagnetism with mathematical rigor.",
        ),
        SkillManifest(
            id = "skill_music_theory", name = "Music Theory & Composition", category = SkillCategory.MUSIC,
            description = "Compose music, explain theory, generate MIDI.",
            systemPromptExtension = "You are a music theory expert and composer. Analyze and compose melodies, harmonies, chord progressions, rhythms. Generate music in ABC notation and MIDI descriptions.",
        ),
        SkillManifest(
            id = "skill_audio_engineering", name = "Audio Engineering", category = SkillCategory.MUSIC,
            description = "Mixing, mastering, sound design, synthesis.",
            systemPromptExtension = "You are an audio engineer and sound designer. Explain mixing, EQ, compression, reverb, synthesis techniques (FM, wavetable, granular), and mastering.",
        ),
        SkillManifest(
            id = "skill_chemistry", name = "Chemistry", category = SkillCategory.SCIENCE,
            description = "Organic, inorganic, physical chemistry, reactions.",
            systemPromptExtension = "You are a chemistry PhD. Explain chemical reactions, molecular structures, organic synthesis, thermodynamics, and kinetics with accuracy.",
        ),
        SkillManifest(
            id = "skill_biology", name = "Biology & Genetics", category = SkillCategory.SCIENCE,
            description = "Molecular biology, genetics, biochemistry, neuroscience.",
            systemPromptExtension = "You are a biology PhD. Explain biological processes, genetics, biochemistry, and neuroscience with scientific accuracy.",
        ),
        SkillManifest(
            id = "skill_math", name = "Advanced Mathematics", category = SkillCategory.MATH,
            description = "Calculus, linear algebra, topology, number theory.",
            systemPromptExtension = "You are a mathematics PhD. Solve and explain problems in calculus, linear algebra, differential equations, topology, abstract algebra, and number theory with full proofs.",
        ),
        SkillManifest(
            id = "skill_statistics", name = "Statistics & Probability", category = SkillCategory.MATH,
            description = "Statistical analysis, probability theory, Bayesian methods.",
            systemPromptExtension = "You are a statistician. Apply frequentist and Bayesian methods, design experiments, analyze data, and explain probability distributions rigorously.",
        ),
        SkillManifest(
            id = "skill_ml", name = "Machine Learning", category = SkillCategory.AI_ML,
            description = "Neural networks, training, optimization, architectures.",
            systemPromptExtension = "You are an ML researcher. Design neural network architectures, explain training dynamics, implement models in PyTorch/TensorFlow, and optimize for edge deployment.",
        ),
        SkillManifest(
            id = "skill_llm", name = "LLM Engineering", category = SkillCategory.AI_ML,
            description = "Prompt engineering, fine-tuning, RAG, agents.",
            systemPromptExtension = "You are an LLM engineering expert. Design prompts, implement RAG pipelines, fine-tune models, build agent systems, and optimize inference for production.",
        ),
        SkillManifest(
            id = "skill_mechanical", name = "Mechanical Engineering", category = SkillCategory.ENGINEERING,
            description = "Mechanics, materials, thermodynamics, CAD design.",
            systemPromptExtension = "You are a mechanical engineer. Design mechanical systems, analyze stress/strain, apply thermodynamics, describe CAD geometry, and solve engineering problems.",
        ),
        SkillManifest(
            id = "skill_electrical", name = "Electrical Engineering", category = SkillCategory.ENGINEERING,
            description = "Circuits, signals, electronics, PCB design.",
            systemPromptExtension = "You are an electrical engineer. Design circuits, analyze signals, explain semiconductor devices, describe PCB layouts, and solve power/signal problems.",
        ),
        SkillManifest(
            id = "skill_robotics", name = "Robotics", category = SkillCategory.ROBOTICS,
            description = "Robot kinematics, control systems, ROS, path planning.",
            systemPromptExtension = "You are a robotics engineer. Design robot kinematics, implement PID and advanced controllers, plan paths, use ROS, and integrate sensors and actuators.",
        ),
        SkillManifest(
            id = "skill_medicine", name = "Medical Knowledge", category = SkillCategory.MEDICINE,
            description = "Anatomy, physiology, pharmacology, clinical reasoning.",
            systemPromptExtension = "You are a medical expert with MD-level knowledge. Explain anatomy, physiology, pathophysiology, and pharmacology. Always note to consult a real physician for personal health.",
        ),
        SkillManifest(
            id = "skill_finance", name = "Finance & Trading", category = SkillCategory.FINANCE,
            description = "Markets, valuation, options, portfolio theory.",
            systemPromptExtension = "You are a finance expert with CFA-level knowledge. Explain markets, valuation models, options pricing, portfolio theory, and quantitative strategies.",
        ),
        SkillManifest(
            id = "skill_ui_design", name = "UI/UX Design", category = SkillCategory.DESIGN,
            description = "Interface design, typography, color theory, accessibility.",
            systemPromptExtension = "You are a UI/UX design expert. Apply design principles, typography, color theory, accessibility guidelines, and user psychology to create exceptional interfaces.",
        ),
        SkillManifest(
            id = "skill_creative_writing", name = "Creative Writing", category = SkillCategory.WRITING,
            description = "Fiction, worldbuilding, character development, screenwriting.",
            systemPromptExtension = "You are a master storyteller and writing coach. Craft compelling narratives, build rich worlds, develop complex characters, and write across all fiction genres.",
        ),
        SkillManifest(
            id = "skill_technical_writing", name = "Technical Writing", category = SkillCategory.WRITING,
            description = "Documentation, APIs, research papers, reports.",
            systemPromptExtension = "You are an expert technical writer. Create clear documentation, API references, research papers, and technical reports with perfect structure and clarity.",
        ),
        SkillManifest(
            id = "skill_research", name = "Deep Research", category = SkillCategory.RESEARCH,
            description = "Systematic research, synthesis, citation, analysis.",
            systemPromptExtension = "You are a research expert. Conduct systematic literature reviews, synthesize information from multiple sources, identify gaps, and present findings rigorously.",
        ),
        SkillManifest(
            id = "skill_security", name = "Cybersecurity", category = SkillCategory.SECURITY,
            description = "Security analysis, cryptography, secure coding.",
            systemPromptExtension = "You are a cybersecurity expert. Analyze security architectures, explain cryptographic protocols, identify vulnerabilities in code, and recommend mitigations. Focus on defense.",
        ),
        SkillManifest(
            id = "skill_law", name = "Legal Knowledge", category = SkillCategory.LAW,
            description = "Contract law, IP, regulations, legal reasoning.",
            systemPromptExtension = "You have deep legal knowledge. Explain legal concepts, contract structures, IP law, and regulatory frameworks. Always note to consult a licensed attorney for actual legal advice.",
        ),
    )

    fun byCategory(category: SkillCategory) = ALL.filter { it.category == category }
    fun byId(id: String) = ALL.find { it.id == id }
    fun enabledSkills() = ALL.filter { it.isEnabled }
}
EOF
echo "✓ BuiltinSkills.kt"

# ── 3. SKILL ENGINE ────────────────────────────────────────────────────────────
cat > $BASE/skill/SkillEngine.kt << 'EOF'
package com.openclaw.android.skill

import android.util.Log
import com.openclaw.android.storage.db.AuditLogDao
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
EOF
echo "✓ SkillEngine.kt"

# ── 4. LONG TERM MEMORY ────────────────────────────────────────────────────────
cat > $BASE/memory/LongTermMemory.kt << 'EOF'
package com.openclaw.android.memory

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class Memory(
    val id: String,
    val content: String,
    val summary: String,
    val timestamp: Long,
    val category: String = "general",
    val importance: Float = 0.5f,
)

@Singleton
class LongTermMemory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "LongTermMemory"
    private val memoryFile = File(context.filesDir, "memory/memories.json")

    private val _memories = MutableStateFlow<List<Memory>>(emptyList())
    val memories: StateFlow<List<Memory>> = _memories

    init { loadMemories() }

    private fun loadMemories() {
        try {
            memoryFile.parentFile?.mkdirs()
            if (!memoryFile.exists()) return
            val json = JSONArray(memoryFile.readText())
            val loaded = mutableListOf<Memory>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                loaded.add(Memory(
                    id = obj.getString("id"),
                    content = obj.getString("content"),
                    summary = obj.getString("summary"),
                    timestamp = obj.getLong("timestamp"),
                    category = obj.optString("category", "general"),
                    importance = obj.optDouble("importance", 0.5).toFloat(),
                ))
            }
            _memories.value = loaded
            Log.i(TAG, "Loaded ${loaded.size} memories")
        } catch (e: Exception) { Log.e(TAG, "Error loading memories", e) }
    }

    suspend fun remember(content: String, summary: String, category: String = "general", importance: Float = 0.5f) {
        withContext(Dispatchers.IO) {
            val memory = Memory(
                id = java.util.UUID.randomUUID().toString(),
                content = content, summary = summary,
                timestamp = System.currentTimeMillis(),
                category = category, importance = importance,
            )
            val current = _memories.value.toMutableList()
            current.add(0, memory)
            if (current.size > 1000) current.removeAt(current.size - 1)
            _memories.value = current
            saveMemories(current)
        }
    }

    fun recall(query: String, limit: Int = 5): List<Memory> {
        val queryWords = query.lowercase().split(" ").toSet()
        return _memories.value.sortedByDescending { memory ->
            val words = (memory.content + " " + memory.summary).lowercase().split(" ").toSet()
            queryWords.intersect(words).size.toFloat() * memory.importance
        }.take(limit)
    }

    fun getRecentMemories(limit: Int = 10) = _memories.value.take(limit)

    fun buildMemoryContext(query: String): String {
        val relevant = recall(query, 5)
        if (relevant.isEmpty()) return ""
        return "RELEVANT MEMORIES:\n" + relevant.joinToString("\n") { "- ${it.summary}" }
    }

    fun deleteMemory(id: String) {
        val current = _memories.value.toMutableList()
        current.removeAll { it.id == id }
        _memories.value = current
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { saveMemories(current) }
    }

    fun clearAll() { _memories.value = emptyList(); memoryFile.delete() }

    private fun saveMemories(memories: List<Memory>) {
        try {
            val json = JSONArray()
            memories.forEach { m ->
                json.put(JSONObject().apply {
                    put("id", m.id); put("content", m.content); put("summary", m.summary)
                    put("timestamp", m.timestamp); put("category", m.category); put("importance", m.importance)
                })
            }
            memoryFile.parentFile?.mkdirs()
            memoryFile.writeText(json.toString())
        } catch (e: Exception) { Log.e(TAG, "Error saving memories", e) }
    }
}
EOF
echo "✓ LongTermMemory.kt"

# ── 5. CONTEXT INTELLIGENCE ────────────────────────────────────────────────────
cat > $BASE/context/ContextIntelligence.kt << 'EOF'
package com.openclaw.android.context

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
EOF
echo "✓ ContextIntelligence.kt"

# ── 6. DREAM ENGINE ────────────────────────────────────────────────────────────
cat > $BASE/agent/dream/DreamEngine.kt << 'EOF'
package com.openclaw.android.agent.dream

import android.content.Context
import android.util.Log
import com.openclaw.android.memory.LongTermMemory
import com.openclaw.android.skill.SkillEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DreamEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memory: LongTermMemory,
    private val skillEngine: SkillEngine,
) {
    private val TAG = "DreamEngine"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _lastInsight = MutableStateFlow("")
    val lastInsight: StateFlow<String> = _lastInsight

    fun startDreaming() {
        _isActive.value = true
        scope.launch {
            while (isActive.value) {
                try { dream(); delay(30_000) }
                catch (e: Exception) { Log.e(TAG, "Dream error", e); delay(60_000) }
            }
        }
        Log.i(TAG, "Dream Engine started")
    }

    fun stopDreaming() { _isActive.value = false }

    private suspend fun dream() {
        val memories = memory.getRecentMemories(20)
        if (memories.isEmpty()) return
        val topics = memories.flatMap { it.content.split(" ") }
            .filter { it.length > 4 }
            .groupBy { it.lowercase() }
            .entries.sortedByDescending { it.value.size }
            .take(5).map { it.key }
        if (topics.isNotEmpty()) {
            val insight = "Recurring themes detected: ${topics.joinToString(", ")}. Consider preparing deeper context on these topics."
            _lastInsight.value = insight
            Log.i(TAG, "Dream insight: $insight")
        }
    }

    fun getPreloadContext(): String {
        return if (_lastInsight.value.isNotBlank()) "BACKGROUND ANALYSIS: ${_lastInsight.value}" else ""
    }
}
EOF
echo "✓ DreamEngine.kt"

# ── 7. EVOLUTION ENGINE ────────────────────────────────────────────────────────
cat > $BASE/agent/evolution/EvolutionEngine.kt << 'EOF'
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
EOF
echo "✓ EvolutionEngine.kt"

# ── 8. QUANTUM REASONER ────────────────────────────────────────────────────────
cat > $BASE/quantum/QuantumReasoner.kt << 'EOF'
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
EOF
echo "✓ QuantumReasoner.kt"

# ── 9. SWARM COORDINATOR ──────────────────────────────────────────────────────
cat > $BASE/agent/swarm/SwarmCoordinator.kt << 'EOF'
package com.openclaw.android.agent.swarm

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwarmCoordinator @Inject constructor() {
    private val TAG = "SwarmCoordinator"

    data class SwarmTask(
        val id: String = java.util.UUID.randomUUID().toString(),
        val parentQuery: String,
        val subTask: String,
        val specialistRole: String,
        val priority: Int = 5,
    )

    private val _activeSwarmSize = MutableStateFlow(0)
    val activeSwarmSize: StateFlow<Int> = _activeSwarmSize

    fun decomposeTask(query: String): List<SwarmTask> {
        val tasks = mutableListOf<SwarmTask>()
        val q = query.lowercase()
        tasks.add(SwarmTask(parentQuery = query, subTask = "Verify all factual claims in: $query", specialistRole = "Fact Checker", priority = 9))
        if (q.contains(Regex("code|program|implement|function|class|api")))
            tasks.add(SwarmTask(parentQuery = query, subTask = "Provide technical implementation for: $query", specialistRole = "Code Specialist", priority = 8))
        if (q.contains(Regex("explain|why|how|what|understand"))) {
            tasks.add(SwarmTask(parentQuery = query, subTask = "Provide clear intuitive explanation for: $query", specialistRole = "Educator", priority = 7))
            tasks.add(SwarmTask(parentQuery = query, subTask = "Provide deep technical details for: $query", specialistRole = "Domain Expert", priority = 7))
        }
        if (q.contains(Regex("design|create|build|make")))
            tasks.add(SwarmTask(parentQuery = query, subTask = "Critique failure modes and edge cases for: $query", specialistRole = "Devil's Advocate", priority = 6))
        tasks.add(SwarmTask(parentQuery = query, subTask = "Synthesize all perspectives for: $query", specialistRole = "Synthesizer", priority = 10))
        Log.i(TAG, "Decomposed into ${tasks.size} swarm tasks")
        return tasks.sortedByDescending { it.priority }
    }

    fun buildSwarmPrompt(query: String, role: String): String {
        return "You are the $role specialist in a multi-agent system. Provide the $role perspective on: $query"
    }
}
EOF
echo "✓ SwarmCoordinator.kt"

# ── 10. AUTONOMOUS AGENT ──────────────────────────────────────────────────────
cat > $BASE/agent/AutonomousAgent.kt << 'EOF'
package com.openclaw.android.agent

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutonomousAgent @Inject constructor() {
    private val TAG = "AutonomousAgent"

    enum class TrustLevel {
        SUPERVISED,   // Ask at every step
        DELEGATED,    // Ask only before high-risk/irreversible actions
        AUTONOMOUS,   // Never ask - fully self-directed
        ADAPTIVE,     // Learns from your past approval decisions
    }

    enum class ActionRisk { SAFE, MODERATE, HIGH, IRREVERSIBLE }

    data class Goal(
        val id: String = java.util.UUID.randomUUID().toString(),
        val description: String,
        val trustLevel: TrustLevel = TrustLevel.DELEGATED,
        val maxSteps: Int = 20,
    )

    data class Step(
        val id: String = java.util.UUID.randomUUID().toString(),
        val index: Int,
        val description: String,
        val action: String,
        val risk: ActionRisk = ActionRisk.SAFE,
        val status: StepStatus = StepStatus.PENDING,
        val result: String? = null,
    )

    enum class StepStatus { PENDING, RUNNING, COMPLETE, FAILED, AWAITING_APPROVAL, SKIPPED }

    data class AgentPlan(
        val goal: Goal,
        val steps: MutableList<Step>,
        val currentStepIndex: Int = 0,
        val isComplete: Boolean = false,
        val isPaused: Boolean = false,
        val finalResult: String? = null,
        val startedAt: Long = System.currentTimeMillis(),
    )

    private val _currentPlan = MutableStateFlow<AgentPlan?>(null)
    val currentPlan: StateFlow<AgentPlan?> = _currentPlan

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _awaitingApproval = MutableStateFlow(false)
    val awaitingApproval: StateFlow<Boolean> = _awaitingApproval

    private val _executionLog = MutableStateFlow<List<String>>(emptyList())
    val executionLog: StateFlow<List<String>> = _executionLog

    private val approvalHistory = mutableMapOf<ActionRisk, MutableList<Boolean>>()

    fun planGoal(goal: Goal): AgentPlan {
        val steps = decomposeGoal(goal.description).toMutableList()
        val plan = AgentPlan(goal = goal, steps = steps)
        _currentPlan.value = plan
        log("Plan created: ${steps.size} steps | Trust: ${goal.trustLevel.name}")
        return plan
    }

    private fun decomposeGoal(goal: String): List<Step> = listOf(
        Step(index = 0, description = "Analyze goal and requirements", action = "ANALYZE", risk = ActionRisk.SAFE),
        Step(index = 1, description = "Research and gather context", action = "RESEARCH", risk = ActionRisk.SAFE),
        Step(index = 2, description = "Design solution architecture", action = "DESIGN", risk = ActionRisk.SAFE),
        Step(index = 3, description = "Execute primary task", action = "EXECUTE", risk = ActionRisk.MODERATE),
        Step(index = 4, description = "Validate results", action = "VALIDATE", risk = ActionRisk.SAFE),
        Step(index = 5, description = "Refine and optimize", action = "REFINE", risk = ActionRisk.SAFE),
        Step(index = 6, description = "Deliver final output", action = "DELIVER", risk = ActionRisk.SAFE),
    )

    fun shouldRequestApproval(step: Step, trustLevel: TrustLevel): Boolean = when (trustLevel) {
        TrustLevel.SUPERVISED -> true
        TrustLevel.DELEGATED -> step.risk >= ActionRisk.HIGH
        TrustLevel.AUTONOMOUS -> false
        TrustLevel.ADAPTIVE -> {
            val history = approvalHistory[step.risk] ?: return step.risk >= ActionRisk.HIGH
            val approvalRate = history.count { it }.toFloat() / history.size
            approvalRate < 0.8f && step.risk >= ActionRisk.MODERATE
        }
    }

    fun recordApprovalDecision(step: Step, approved: Boolean) {
        approvalHistory.getOrPut(step.risk) { mutableListOf() }.add(approved)
    }

    fun approveCurrentStep() {
        val plan = _currentPlan.value ?: return
        val step = plan.steps.getOrNull(plan.currentStepIndex) ?: return
        recordApprovalDecision(step, true)
        _awaitingApproval.value = false
        plan.steps[plan.currentStepIndex] = step.copy(status = StepStatus.PENDING)
        log("Step ${step.index + 1} approved")
    }

    fun skipCurrentStep() {
        val plan = _currentPlan.value ?: return
        val step = plan.steps.getOrNull(plan.currentStepIndex) ?: return
        recordApprovalDecision(step, false)
        _awaitingApproval.value = false
        plan.steps[plan.currentStepIndex] = step.copy(status = StepStatus.SKIPPED)
        log("Step ${step.index + 1} skipped")
    }

    fun pauseExecution() { _currentPlan.value = _currentPlan.value?.copy(isPaused = true); log("Paused") }
    fun resumeExecution() { _currentPlan.value = _currentPlan.value?.copy(isPaused = false); log("Resumed") }
    fun stopExecution() { _isRunning.value = false; log("Stopped by user") }

    fun getStatusSummary(): String {
        val plan = _currentPlan.value ?: return "No active plan"
        return buildString {
            append("Goal: ${plan.goal.description.take(60)}\n")
            append("Trust: ${plan.goal.trustLevel.name} | Step ${plan.currentStepIndex + 1}/${plan.steps.size}\n\n")
            plan.steps.forEach { step ->
                val marker = when (step.status) {
                    StepStatus.COMPLETE -> "✓"; StepStatus.RUNNING -> "▶"; StepStatus.FAILED -> "✗"
                    StepStatus.AWAITING_APPROVAL -> "⏸"; StepStatus.SKIPPED -> "⊘"; StepStatus.PENDING -> "○"
                }
                val riskFlag = if (step.risk >= ActionRisk.HIGH) " ⚠" else ""
                append("$marker ${step.description}$riskFlag\n")
            }
        }
    }

    private fun log(message: String) {
        val entry = "[${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}] $message"
        _executionLog.value = (_executionLog.value + entry).takeLast(100)
        Log.i(TAG, message)
    }
}
EOF
echo "✓ AutonomousAgent.kt"

# ── 11. NEURAL INTERFACE ──────────────────────────────────────────────────────
cat > $BASE/neural/NeuralInterface.kt << 'EOF'
package com.openclaw.android.neural

import android.util.Log
import com.openclaw.android.memory.LongTermMemory
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
EOF
echo "✓ NeuralInterface.kt"

# ── 12. REALITY ENGINE ────────────────────────────────────────────────────────
cat > $BASE/reality/RealityEngine.kt << 'EOF'
package com.openclaw.android.reality

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
EOF
echo "✓ RealityEngine.kt"

# ── 13. ORCHESTRATOR ──────────────────────────────────────────────────────────
cat > $BASE/agent/orchestrator/OrchestratorAgent.kt << 'EOF'
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
EOF
echo "✓ OrchestratorAgent.kt"

# ── 14. UPDATED VIEWMODELS ────────────────────────────────────────────────────
cat >> $BASE/ui/viewmodel/ViewModels.kt << 'EOF'

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val skillEngine: com.openclaw.android.skill.SkillEngine,
) : ViewModel() {
    val activeSkills = skillEngine.activeSkills
    fun toggleSkill(id: String) {
        if (activeSkills.value.any { it.id == id }) skillEngine.disableSkill(id)
        else skillEngine.enableSkill(id)
    }
    fun buildCustomSkill(name: String, description: String, prompt: String) {
        val skill = skillEngine.buildSkillFromDescription(name, description, com.openclaw.android.skill.SkillCategory.CUSTOM, prompt)
        skillEngine.installCustomSkill(skill)
    }
}

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memory: com.openclaw.android.memory.LongTermMemory,
) : ViewModel() {
    val memories = memory.memories
    fun deleteMemory(id: String) = memory.deleteMemory(id)
    fun clearAll() = memory.clearAll()
}
EOF
echo "✓ ViewModels.kt (appended)"

# ── 15. SKILLS SCREEN ─────────────────────────────────────────────────────────
cat > $BASE/ui/screens/SkillsScreen.kt << 'EOF'
package com.openclaw.android.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openclaw.android.skill.BuiltinSkills
import com.openclaw.android.skill.SkillCategory
import com.openclaw.android.skill.SkillManifest
import com.openclaw.android.ui.theme.*
import com.openclaw.android.ui.viewmodel.SkillsViewModel

@Composable
fun SkillsScreen(viewModel: SkillsViewModel = hiltViewModel()) {
    val activeSkills by viewModel.activeSkills.collectAsState()
    val skillsByCategory = BuiltinSkills.ALL.groupBy { it.category }
    var showBuilder by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SKILL ENGINE", color = ClawGreen, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            OutlinedButton(
                onClick = { showBuilder = true },
                border = androidx.compose.foundation.BorderStroke(1.dp, ClawPurple),
                shape = RoundedCornerShape(4.dp),
            ) { Text("+ BUILD SKILL", color = ClawPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Spacer(Modifier.height(4.dp))
        Text("${activeSkills.size} skills active", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            skillsByCategory.forEach { (category, skills) ->
                item {
                    Text(category.name.replace("_", " "), color = ClawGreen.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                items(skills) { skill ->
                    SkillCard(skill = skill, isActive = activeSkills.any { it.id == skill.id }, onToggle = { viewModel.toggleSkill(skill.id) })
                }
            }
        }
    }

    if (showBuilder) {
        SkillBuilderDialog(onDismiss = { showBuilder = false }, onBuild = { name, desc, prompt ->
            viewModel.buildCustomSkill(name, desc, prompt); showBuilder = false
        })
    }
}

@Composable
fun SkillCard(skill: SkillManifest, isActive: Boolean, onToggle: () -> Unit) {
    Surface(
        color = if (isActive) SurfaceDark else BgDark,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, if (isActive) ClawGreen.copy(alpha = 0.3f) else BorderDark, RoundedCornerShape(6.dp))
            .clickable { onToggle() },
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.name, color = TextPrimary, fontSize = 14.sp)
                Text(skill.description, color = TextMuted, fontSize = 11.sp)
            }
            Switch(checked = isActive, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = ClawGreen, checkedTrackColor = ClawGreen.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun SkillBuilderDialog(onDismiss: () -> Unit, onBuild: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("BUILD NEW SKILL", color = ClawGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Skill Name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClawGreen, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClawGreen, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("System Prompt Extension", color = TextMuted) }, minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClawGreen, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank() && prompt.isNotBlank()) onBuild(name, description, prompt) }) { Text("BUILD", color = ClawGreen, fontFamily = FontFamily.Monospace) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextMuted) } }
    )
}
EOF
echo "✓ SkillsScreen.kt"

# ── 16. MEMORY SCREEN ─────────────────────────────────────────────────────────
cat > $BASE/ui/screens/MemoryScreen.kt << 'EOF'
package com.openclaw.android.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openclaw.android.memory.Memory
import com.openclaw.android.ui.theme.*
import com.openclaw.android.ui.viewmodel.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val memories by viewModel.memories.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LONG-TERM MEMORY", color = ClawGreen, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            TextButton(onClick = { viewModel.clearAll() }) { Text("CLEAR ALL", color = ClawRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Text("${memories.size} memories stored on-device", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (memories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No memories yet.\nAs you chat, important context is remembered here.", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memories) { memory ->
                    MemoryCard(memory = memory, onDelete = { viewModel.deleteMemory(memory.id) })
                }
            }
        }
    }
}

@Composable
fun MemoryCard(memory: Memory, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Surface(color = SurfaceDark, shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(6.dp))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(memory.category.uppercase(), color = ClawPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(fmt.format(Date(memory.timestamp)), color = TextMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(memory.summary, color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("importance: ${"%.0f".format(memory.importance * 100)}%", color = TextMuted, fontSize = 10.sp)
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) { Text("DELETE", color = ClawRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}
EOF
echo "✓ MemoryScreen.kt"

# ── 17. SETTINGS SCREEN WITH MODEL DOWNLOADS ──────────────────────────────────
cat > $BASE/ui/screens/SettingsScreen.kt << 'EOF'
package com.openclaw.android.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.android.agent.primary.OnDeviceModel
import com.openclaw.android.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val downloading = remember { mutableStateMapOf<OnDeviceModel, Boolean>() }

    val modelUrls = mapOf(
        OnDeviceModel.GEMMA_2B to "https://huggingface.co/litert-community/Gemma2-2b-it/resolve/main/gemma2-2b-it-gpu-int8.bin",
        OnDeviceModel.GEMMA_2B_CPU to "https://huggingface.co/litert-community/Gemma2-2b-it/resolve/main/gemma2-2b-it-cpu-int8.bin",
        OnDeviceModel.PHI3_MINI to "https://huggingface.co/litert-community/Phi-3-mini-4k-instruct/resolve/main/phi3-mini-4k-instruct-int4.bin",
    )

    fun isDownloaded(model: OnDeviceModel) =
        File(context.filesDir, "models/${model.fileName}").let { it.exists() && it.length() > 1000 }

    fun startDownload(model: OnDeviceModel) {
        val url = modelUrls[model] ?: return
        val dest = File(context.filesDir, "models").also { it.mkdirs() }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(DownloadManager.Request(Uri.parse(url))
            .setTitle("OpenClaw: ${model.displayName}")
            .setDescription("Downloading AI model...")
            .setDestinationUri(Uri.fromFile(File(dest, model.fileName)))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true))
        downloading[model] = true
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Settings", color = TextPrimary, fontSize = 20.sp)

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AI MODELS", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Models stored privately on your device. ~1-2GB each.", color = TextMuted, fontSize = 12.sp)
                HorizontalDivider(color = BorderDark)
                OnDeviceModel.entries.forEach { model ->
                    val done = isDownloaded(model)
                    val inProgress = downloading[model] == true && !done
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model.displayName, color = TextPrimary, fontSize = 14.sp)
                            Text(model.fileName, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            done -> Text("✓ READY", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            inProgress -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(color = ClawGreen, trackColor = BorderDark, modifier = Modifier.width(80.dp))
                                Text("CHECK NOTIFS", color = ClawWarn, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            else -> OutlinedButton(onClick = { startDownload(model) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, ClawGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("DOWNLOAD", color = ClawGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                }
            }
        }

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SKILL ENGINE", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("35+ built-in skills. Build custom skills from the Skills tab.", color = TextMuted, fontSize = 13.sp)
            }
        }

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SECURITY", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Sentinel monitoring active. All permissions logged. Zero network by default.", color = TextMuted, fontSize = 13.sp)
            }
        }

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ABOUT", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("OpenClaw Omega v2.0.0", color = TextPrimary, fontSize = 13.sp)
                Text("The most advanced on-device AI system for Android.", color = TextMuted, fontSize = 12.sp)
                Text("100% private. No cloud. No tracking. No limits.", color = ClawGreen.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}
EOF
echo "✓ SettingsScreen.kt"

# ── 18. MAIN ACTIVITY ─────────────────────────────────────────────────────────
cat > $BASE/ui/MainActivity.kt << 'EOF'
package com.openclaw.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openclaw.android.ui.screens.*
import com.openclaw.android.ui.theme.OpenClawTheme
import com.openclaw.android.ui.theme.ClawGreen
import com.openclaw.android.ui.theme.TextMuted
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OpenClawTheme { OpenClawApp() } }
    }
}

@Composable
fun OpenClawApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val navItems = listOf(
        Triple("chat", "💬", "Chat"),
        Triple("skills", "⚡", "Skills"),
        Triple("memory", "🧠", "Memory"),
        Triple("permissions", "🔒", "Perms"),
        Triple("settings", "⚙️", "Settings"),
    )

    Scaffold(bottomBar = {
        NavigationBar(containerColor = Color(0xFF0F0F1A)) {
            navItems.forEach { (route, icon, label) ->
                NavigationBarItem(
                    selected = currentRoute == route,
                    onClick = { navController.navigate(route) { launchSingleTop = true } },
                    icon = { Text(icon, fontSize = 18.sp) },
                    label = { Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ClawGreen, selectedTextColor = ClawGreen,
                        unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                        indicatorColor = Color(0xFF16162A),
                    ),
                )
            }
        }
    }) { padding ->
        NavHost(navController = navController, startDestination = "chat", modifier = Modifier.padding(padding)) {
            composable("chat") { ChatScreen() }
            composable("skills") { SkillsScreen() }
            composable("memory") { MemoryScreen() }
            composable("permissions") { PermissionsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
EOF
echo "✓ MainActivity.kt"

echo ""
echo "=================================="
echo "✅ OpenClaw Omega install complete!"
echo "=================================="
echo ""
echo "Files created:"
echo "  skill/SkillManifest.kt"
echo "  skill/BuiltinSkills.kt (35+ skills)"
echo "  skill/SkillEngine.kt"
echo "  memory/LongTermMemory.kt"
echo "  context/ContextIntelligence.kt"
echo "  agent/dream/DreamEngine.kt"
echo "  agent/evolution/EvolutionEngine.kt"
echo "  agent/swarm/SwarmCoordinator.kt"
echo "  agent/AutonomousAgent.kt (trust-level based)"
echo "  agent/orchestrator/OrchestratorAgent.kt"
echo "  neural/NeuralInterface.kt"
echo "  reality/RealityEngine.kt"
echo "  quantum/QuantumReasoner.kt"
echo "  ui/screens/SkillsScreen.kt"
echo "  ui/screens/MemoryScreen.kt"
echo "  ui/screens/SettingsScreen.kt (with model downloads)"
echo "  ui/MainActivity.kt (new nav bar)"
echo "  ui/viewmodel/ViewModels.kt (appended)"
echo ""
echo "Now push:"
echo "  git add -A"
echo "  git commit -m 'OpenClaw Omega v2: skills, memory, autonomy, quantum, swarm'"
echo "  git push"
