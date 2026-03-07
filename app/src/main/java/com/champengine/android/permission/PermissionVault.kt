package com.champengine.android.permission

import android.content.Context
import android.util.Log
import com.champengine.android.storage.db.ChampEngineDatabase
import com.champengine.android.storage.models.ScopeTokenEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PermissionVault — the single source of truth for all scope grants.
 *
 * Design principles:
 * - Only the vault can mint ScopeTokens (agents cannot create them)
 * - Every token is HMAC-signed against the Android Keystore
 * - Pending requests are surfaced to UI via StateFlow
 * - Denied/blocked scopes are remembered and never re-prompted
 */
@Singleton
class PermissionVault @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ChampEngineDatabase,
) {
    private val TAG = "PermissionVault"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Pending requests waiting for user decision — shown as bottom sheet
    private val _pendingRequests = MutableStateFlow<List<PermissionRequest>>(emptyList())
    val pendingRequests: StateFlow<List<PermissionRequest>> = _pendingRequests.asStateFlow()

    // Emits each time a decision is made — agents await this
    private val _decisions = MutableSharedFlow<Pair<String, PermissionDecision>>(replay = 0)
    val decisions: SharedFlow<Pair<String, PermissionDecision>> = _decisions.asSharedFlow()

    // Blocked scopes — never prompt again
    private val blockedScopes = mutableSetOf<String>()

    // Live list of all active tokens for the dashboard
    fun allTokens(): Flow<List<ScopeToken>> =
        db.scopeTokenDao().observeAll().map { entities ->
            entities.map { it.toScopeToken() }
        }

    /**
     * Called by agents before any privileged action.
     * Returns a ScopeToken if already granted, otherwise suspends until user decides.
     */
    suspend fun requireScope(
        scopeType: ScopeType,
        resource: String,
        reason: String,
        agentContext: String = "",
    ): ScopeToken? = withContext(Dispatchers.IO) {
        val scopeKey = "${scopeType.prefix}:$resource"

        // Check blocked list first
        if (scopeKey in blockedScopes) {
            Log.w(TAG, "Scope $scopeKey is blocked — returning null")
            return@withContext null
        }

        // Check existing valid token
        val existing = db.scopeTokenDao().findActive(scopeType.prefix, resource)
        if (existing != null && existing.isActive) {
            val token = existing.toScopeToken()
            if (token.isValid() && ScopeTokenSigner.verify(token.canonicalString(), token.hmacSignature)) {
                // Update usage stats
                db.scopeTokenDao().incrementUsage(token.id, System.currentTimeMillis())
                return@withContext token
            }
        }

        // No valid token — enqueue a request for the user
        val requestId = UUID.randomUUID().toString()
        val request = PermissionRequest(
            requestId = requestId,
            scopeType = scopeType,
            resource = resource,
            reason = reason,
            agentContext = agentContext,
        )

        _pendingRequests.value = _pendingRequests.value + request

        // Wait for user decision (suspends this coroutine)
        val decision = awaitDecision(requestId)

        return@withContext when (decision) {
            is PermissionDecision.AllowPermanent -> mintToken(scopeType, resource, reason, permanent = true)
            is PermissionDecision.AllowOnce -> mintToken(scopeType, resource, reason, permanent = false)
            is PermissionDecision.DenyAndBlock -> {
                blockedScopes.add(scopeKey)
                null
            }
            is PermissionDecision.Deny -> null
            else -> null
        }
    }

    /**
     * Suspends until the user makes a decision about requestId
     */
    private suspend fun awaitDecision(requestId: String): PermissionDecision {
        // Collect decisions until we find ours
        val result = kotlinx.coroutines.CompletableDeferred<PermissionDecision>()
        val job = scope.launch {
            decisions.collect { (id, decision) ->
                if (id == requestId) {
                    result.complete(decision)
                    return@collect
                }
            }
        }
        return result.await().also { job.cancel() }
    }

    /**
     * Called by UI when user makes a decision — dispatches to the awaiting agent coroutine
     */
    fun resolveRequest(requestId: String, decision: PermissionDecision) {
        _pendingRequests.value = _pendingRequests.value.filter { it.requestId != requestId }
        scope.launch {
            _decisions.emit(requestId to decision)
        }
    }

    /**
     * Mint a new HMAC-signed ScopeToken and persist it
     */
    private suspend fun mintToken(
        scopeType: ScopeType,
        resource: String,
        reason: String,
        permanent: Boolean,
    ): ScopeToken {
        val id = UUID.randomUUID().toString()
        val grantedAt = System.currentTimeMillis()
        val expiresAt = if (permanent) null else grantedAt + (24 * 60 * 60 * 1000L) // 24h for once

        val partial = ScopeToken(
            id = id,
            scopeType = scopeType.prefix,
            resource = resource,
            grantedAt = grantedAt,
            expiresAt = expiresAt,
            reason = reason,
        )
        val signature = ScopeTokenSigner.sign(partial.canonicalString())
        val token = partial.copy(hmacSignature = signature)

        db.scopeTokenDao().insert(token.toEntity())
        Log.i(TAG, "Minted token: ${token.id} for ${token.scopeType}:${token.resource}")
        return token
    }

    /**
     * Revoke a specific token — called from the user dashboard
     */
    suspend fun revokeToken(tokenId: String) = withContext(Dispatchers.IO) {
        db.scopeTokenDao().revoke(tokenId, System.currentTimeMillis())
        Log.i(TAG, "Revoked token: $tokenId")
    }

    /**
     * Revoke all tokens of a given type — "Remove all network permissions"
     */
    suspend fun revokeByType(scopeType: ScopeType) = withContext(Dispatchers.IO) {
        db.scopeTokenDao().revokeByType(scopeType.prefix, System.currentTimeMillis())
    }

    /**
     * Check if a scope is currently granted (used by Sentinel to detect violations)
     */
    suspend fun isGranted(scopeType: ScopeType, resource: String): Boolean = withContext(Dispatchers.IO) {
        val entity = db.scopeTokenDao().findActive(scopeType.prefix, resource)
        entity != null && entity.isActive
    }

    /**
     * Emergency revoke all — called by Sentinel on critical threat
     */
    suspend fun revokeAllOnThreat() = withContext(Dispatchers.IO) {
        db.scopeTokenDao().revokeAll(System.currentTimeMillis())
        Log.w(TAG, "SENTINEL: All permissions revoked due to threat detection")
    }
}

// Extension functions for entity conversion
fun ScopeToken.toEntity() = ScopeTokenEntity(
    id = id,
    scopeType = scopeType,
    resource = resource,
    grantedAt = grantedAt,
    expiresAt = expiresAt,
    grantedBy = grantedBy,
    reason = reason,
    usageCount = usageCount,
    lastUsed = lastUsed,
    isActive = isActive,
    hmacSignature = hmacSignature,
)

fun ScopeTokenEntity.toScopeToken() = ScopeToken(
    id = id,
    scopeType = scopeType,
    resource = resource,
    grantedAt = grantedAt,
    expiresAt = expiresAt,
    grantedBy = grantedBy,
    reason = reason,
    usageCount = usageCount,
    lastUsed = lastUsed,
    isActive = isActive,
    hmacSignature = hmacSignature,
)
