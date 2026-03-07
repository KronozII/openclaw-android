package com.champengine.android.agent.swarm

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
