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
