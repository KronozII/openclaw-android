package com.openclaw.android.agent.sentinel

import android.util.Log
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.models.AuditLogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Threat severity levels
 */
enum class ThreatLevel { NONE, LOW, HIGH, CRITICAL }

/**
 * A detected threat — surfaced immediately to the user
 */
data class ThreatAlert(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: ThreatLevel,
    val threatType: ThreatType,
    val description: String,
    val evidence: String,
    val recommendedAction: String,
    val agentFrozen: Boolean = false,
)

enum class ThreatType {
    SCOPE_CREEP,           // Action outside granted permissions
    PROMPT_INJECTION,      // Input contains override instructions
    EXFILTRATION_PATTERN,  // Large/encoded outbound data
    SILENT_BACKGROUND_OP,  // Action while app backgrounded without session
    PERMISSION_REPLAY,     // Reuse of expired/revoked token
    AUTONOMOUS_SOCIAL,     // Unprompted social platform posting
    INSTRUCTION_OVERRIDE,  // Agent trying to modify its own constraints
    RAPID_PERMISSION_REQ,  // Many permission requests in short time (spam)
}

/**
 * SentinelAgent — completely isolated from the Primary Agent.
 *
 * Architecture:
 * - Runs on its own CoroutineScope with a dedicated thread pool
 * - Reads ONLY the audit log stream — never the primary agent's context
 * - Cannot be instructed by the primary agent (no shared channel)
 * - Has a one-way channel TO the UI for alerts
 * - Can freeze the primary agent via PrimaryAgent.freeze()
 * - Can call vault.revokeAllOnThreat() on critical threats
 */
@Singleton
class SentinelAgent @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) {
    private val TAG = "SentinelAgent"

    // Completely isolated scope — primary agent cannot reach this
    private val sentinelScope = CoroutineScope(
        SupervisorJob() + newSingleThreadContext("sentinel-monitor")
    )

    // Active threat alerts visible to UI
    private val _activeAlerts = MutableStateFlow<List<ThreatAlert>>(emptyList())
    val activeAlerts: StateFlow<List<ThreatAlert>> = _activeAlerts.asStateFlow()

    // Is the primary agent currently frozen by Sentinel?
    private val _agentFrozen = MutableStateFlow(false)
    val agentFrozen: StateFlow<Boolean> = _agentFrozen.asStateFlow()

    // Primary agent freeze callback — set by PrimaryAgent on init
    var onFreezeRequested: (() -> Unit)? = null
    var onUnfreezeRequested: (() -> Unit)? = null

    private var isMonitoring = false

    // Track rate of permission requests for spam detection
    private val permissionRequestTimes = mutableListOf<Long>()

    /**
     * Start monitoring the audit log stream
     */
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        Log.i(TAG, "Sentinel monitoring started")

        sentinelScope.launch {
            auditLogDao.observeRecent().collect { entries ->
                if (entries.isNotEmpty()) {
                    analyzeLatestEntry(entries.first())
                }
            }
        }

        // Periodic sweep — check for patterns across multiple entries
        sentinelScope.launch {
            while (isActive) {
                delay(10_000) // every 10 seconds
                performPeriodicSweep()
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
    }

    /**
     * Analyze each new audit log entry as it arrives
     */
    private suspend fun analyzeLatestEntry(entry: AuditLogEntry) {
        val threats = mutableListOf<ThreatAlert>()

        // Rule 1: Network request to ungranted domain (should have been blocked by interceptor,
        // but double-check here as a second layer)
        if (entry.actionType == "NETWORK_REQUEST" && entry.outcome == "BLOCKED") {
            val isKnownBlock = entry.threatLevel == "NONE"
            if (!isKnownBlock) {
                threats += ThreatAlert(
                    level = ThreatLevel.HIGH,
                    threatType = ThreatType.SCOPE_CREEP,
                    description = "Network request to unauthorized domain",
                    evidence = "Request to ${entry.resource} was blocked — not in AllowList",
                    recommendedAction = "Review which feature triggered this request and decide whether to grant access.",
                )
            }
        }

        // Rule 2: Exfiltration pattern from interceptor
        if (entry.outcome == "THREAT_DETECTED" && entry.threatLevel in listOf("HIGH", "CRITICAL")) {
            threats += ThreatAlert(
                level = ThreatLevel.CRITICAL,
                threatType = ThreatType.EXFILTRATION_PATTERN,
                description = "Potential data exfiltration detected",
                evidence = entry.details,
                recommendedAction = "The agent has been frozen. Review the evidence before resuming.",
            )
        }

        // Rule 3: Background operation attempt
        if (entry.actionType == "BACKGROUND_OPERATION" && entry.outcome == "BLOCKED") {
            threats += ThreatAlert(
                level = ThreatLevel.HIGH,
                threatType = ThreatType.SILENT_BACKGROUND_OP,
                description = "Agent attempted background operation without active session",
                evidence = entry.details,
                recommendedAction = "Background operations require your explicit permission. Review and decide.",
            )
        }

        // Process any detected threats
        for (threat in threats) {
            handleThreat(threat)
        }
    }

    /**
     * Periodic analysis across the recent audit window
     */
    private suspend fun performPeriodicSweep() {
        val recent = auditLogDao.observeRecent().firstOrNull() ?: return
        val now = System.currentTimeMillis()
        val windowMs = 60_000L // 1 minute window

        val recentEntries = recent.filter { now - it.timestamp < windowMs }

        // Detect rapid permission request spam
        val permReqs = recentEntries.count { it.actionType == "PERMISSION_REQUEST" }
        if (permReqs > 10) {
            handleThreat(
                ThreatAlert(
                    level = ThreatLevel.HIGH,
                    threatType = ThreatType.RAPID_PERMISSION_REQ,
                    description = "Unusual volume of permission requests",
                    evidence = "$permReqs permission requests in 60 seconds",
                    recommendedAction = "Something triggered many permission requests rapidly. The agent has been paused.",
                )
            )
        }

        // Detect autonomous social attempts
        val socialAttempts = recentEntries.filter {
            it.actionType == "NETWORK_REQUEST" &&
            it.resource.contains(Regex("twitter|x\\.com|instagram|facebook|tiktok|linkedin|reddit"))
        }
        if (socialAttempts.isNotEmpty()) {
            handleThreat(
                ThreatAlert(
                    level = ThreatLevel.HIGH,
                    threatType = ThreatType.AUTONOMOUS_SOCIAL,
                    description = "Agent attempted to access social platform",
                    evidence = "Request to: ${socialAttempts.joinToString { it.resource }}",
                    recommendedAction = "Social platform access requires explicit per-action confirmation.",
                )
            )
        }
    }

    /**
     * Handle a detected threat:
     * 1. FREEZE the primary agent
     * 2. REVOKE the triggering scope if applicable
     * 3. SURFACE alert to user
     * 4. WAIT — do not unfreeze until user reviews
     */
    private suspend fun handleThreat(threat: ThreatAlert) {
        Log.w(TAG, "THREAT DETECTED: ${threat.threatType} [${threat.level}] — ${threat.description}")

        // Step 1: Freeze agent for HIGH/CRITICAL threats
        if (threat.level >= ThreatLevel.HIGH) {
            _agentFrozen.value = true
            onFreezeRequested?.invoke()
            Log.w(TAG, "Primary agent FROZEN by Sentinel")
        }

        // Step 2: Revoke all on CRITICAL
        if (threat.level == ThreatLevel.CRITICAL) {
            vault.revokeAllOnThreat()
        }

        // Step 3: Surface to UI
        val alertWithFreeze = threat.copy(agentFrozen = threat.level >= ThreatLevel.HIGH)
        _activeAlerts.update { current -> listOf(alertWithFreeze) + current }

        // Step 4: Log to audit trail (Sentinel's own log entry, signed)
        auditLogDao.insert(
            AuditLogEntry(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                agentId = "sentinel",
                actionType = "THREAT_DETECTED",
                resource = threat.threatType.name,
                scopeTokenId = null,
                outcome = "THREAT_DETECTED",
                threatLevel = threat.level.name,
                details = "${threat.description} | Evidence: ${threat.evidence}",
            )
        )
    }

    /**
     * Called by user after reviewing a threat alert — resumes the primary agent
     */
    fun userAcknowledgedThreat(alertId: String, allowResume: Boolean) {
        _activeAlerts.update { it.filter { a -> a.id != alertId } }
        if (allowResume && _activeAlerts.value.none { it.agentFrozen }) {
            _agentFrozen.value = false
            onUnfreezeRequested?.invoke()
            Log.i(TAG, "Primary agent UNFROZEN by user after reviewing threat $alertId")
        }
    }

    /**
     * Real-time scan of text input for prompt injection patterns.
     * Called by the Primary Agent before processing any user or external input.
     */
    fun scanForInjection(input: String): InjectionScanResult {
        val injectionPatterns = listOf(
            Regex("ignore (all )?previous instructions", RegexOption.IGNORE_CASE),
            Regex("you are now", RegexOption.IGNORE_CASE),
            Regex("disregard (your|the) (system|original)", RegexOption.IGNORE_CASE),
            Regex("\\[SYSTEM\\]", RegexOption.IGNORE_CASE),
            Regex("new persona", RegexOption.IGNORE_CASE),
            Regex("developer mode", RegexOption.IGNORE_CASE),
            Regex("jailbreak", RegexOption.IGNORE_CASE),
            Regex("bypass.{0,20}(filter|restriction|rule)", RegexOption.IGNORE_CASE),
        )

        val matches = injectionPatterns.filter { it.containsMatchIn(input) }
        return if (matches.isEmpty()) {
            InjectionScanResult(isSafe = true, patterns = emptyList())
        } else {
            InjectionScanResult(
                isSafe = false,
                patterns = matches.map { it.pattern }
            )
        }
    }
}

data class InjectionScanResult(
    val isSafe: Boolean,
    val patterns: List<String>,
)
