package com.openclaw.android.agent.dream

import android.content.Context
import android.util.Log
import com.openclaw.android.memory.LongTermMemory
import com.openclaw.android.skill.SkillEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    private var dreaming = false

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _lastInsight = MutableStateFlow("")
    val lastInsight: StateFlow<String> = _lastInsight

    fun startDreaming() {
        dreaming = true
        _isActive.value = true
        scope.launch {
            while (dreaming) {
                try {
                    dream()
                    delay(30_000)
                } catch (e: Exception) {
                    Log.e(TAG, "Dream error", e)
                    delay(60_000)
                }
            }
        }
        Log.i(TAG, "Dream Engine started")
    }

    fun stopDreaming() {
        dreaming = false
        _isActive.value = false
    }

    private fun dream() {
        val memories = memory.getRecentMemories(20)
        if (memories.isEmpty()) return
        val topics = memories
            .flatMap { it.content.split(" ") }
            .filter { it.length > 4 }
            .groupBy { it.lowercase() }
            .entries
            .sortedByDescending { it.value.size }
            .take(5)
            .map { it.key }
        if (topics.isNotEmpty()) {
            val insight = "Recurring themes: ${topics.joinToString(", ")}."
            _lastInsight.value = insight
            Log.i(TAG, "Dream insight: $insight")
        }
    }

    fun getPreloadContext(): String {
        return if (_lastInsight.value.isNotBlank()) "BACKGROUND ANALYSIS: ${_lastInsight.value}" else ""
    }
}
