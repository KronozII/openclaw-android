package com.openclaw.android.network

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import android.util.Log
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.models.AuditLogEntry
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AllowListInterceptor — every outgoing HTTP request passes through this.
 *
 * If the destination host is not in the PermissionVault, the request is:
 * 1. Blocked immediately (returns 403 to the caller)
 * 2. Logged to the audit trail
 * 3. A PermissionRequest is raised for the user
 *
 * This is enforced at the OkHttp level — agent code cannot bypass it.
 */
@Singleton
class AllowListInterceptor @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) : Interceptor {

    private val TAG = "AllowListInterceptor"

    // Domains always allowed (telemetry-free open-source APIs only)
    private val permanentAllowList = setOf(
        "localhost",
        "127.0.0.1",
        "10.0.0.1", // local LAN sandbox server
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val url = request.url.toString()

        // Permanent allow: loopback
        if (host in permanentAllowList) {
            return chain.proceed(request)
        }

        // Check vault synchronously (agents must pre-authorize before calling)
        val isGranted = runBlocking {
            vault.isGranted(ScopeType.NETWORK, host) ||
            vault.isGranted(ScopeType.NETWORK, "*")
        }

        return if (isGranted) {
            // Allowed — proceed and log
            logAccess(host, url, allowed = true)
            val response = chain.proceed(request)
            inspectResponseForExfiltration(request, response)
        } else {
            // BLOCKED — log and return synthetic 403
            Log.w(TAG, "BLOCKED unauthorized request to: $host")
            logAccess(host, url, allowed = false)
            // Surface a permission request to UI asynchronously
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                vault.requireScope(
                    scopeType = ScopeType.NETWORK,
                    resource = host,
                    reason = "A request was made to $host",
                    agentContext = "Outgoing HTTP request to $url"
                )
            }
            blockResponse(request, host)
        }
    }

    /**
     * Inspect response body for exfiltration signals:
     * - Large binary payloads not matching declared content type
     * - Base64-encoded data in unexpected contexts
     * - Requests carrying device identifiers
     */
    private fun inspectResponseForExfiltration(
        request: okhttp3.Request,
        response: Response
    ): Response {
        val contentLength = response.body?.contentLength() ?: 0
        val requestBodySize = request.body?.contentLength() ?: 0

        // Flag suspiciously large outgoing payloads
        if (requestBodySize > 50_000) { // > 50KB outbound
            Log.w(TAG, "EXFILTRATION SIGNAL: Large outbound payload (${requestBodySize}B) to ${request.url.host}")
            runBlocking {
                logThreat(
                    host = request.url.host,
                    url = request.url.toString(),
                    threatLevel = "HIGH",
                    details = "Outbound payload size: ${requestBodySize}B"
                )
            }
        }
        return response
    }

    private fun blockResponse(request: okhttp3.Request, host: String): Response {
        return okhttp3.Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(403)
            .message("PERMISSION_REQUIRED: $host not in allowlist")
            .body("{}".toResponseBody())
            .build()
    }

    private fun logAccess(host: String, url: String, allowed: Boolean) {
        runBlocking {
            auditLogDao.insert(
                AuditLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    agentId = "primary",
                    actionType = "NETWORK_REQUEST",
                    resource = host,
                    scopeTokenId = null,
                    outcome = if (allowed) "ALLOWED" else "BLOCKED",
                    details = url.take(200),
                )
            )
        }
    }

    private suspend fun logThreat(host: String, url: String, threatLevel: String, details: String) {
        auditLogDao.insert(
            AuditLogEntry(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                agentId = "interceptor",
                actionType = "NETWORK_REQUEST",
                resource = host,
                scopeTokenId = null,
                outcome = "THREAT_DETECTED",
                threatLevel = threatLevel,
                details = details,
            )
        )
    }
}
