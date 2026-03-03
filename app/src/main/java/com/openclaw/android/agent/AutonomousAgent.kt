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
