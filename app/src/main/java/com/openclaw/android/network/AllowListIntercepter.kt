package com.openclaw.android.network

import android.util.Log
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.models.AuditLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllowListInterceptor @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) : Interceptor {

    private val TAG = "AllowListInterceptor"

    private val permanentAllowList = setOf(
        "localhost",
        "127.0.0.1",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host

        if (host in permanentAllowList) {
            return chain.proceed(request)
        }

        val isGranted = runBlocking {
            vault.hasActiveScope(ScopeType.NETWORK, host)
        }

        return if (isGranted) {
            logAccess(host, url, allowed = true)
            val response = chain.proceed(request)
            inspectResponseForExfiltration(request, response)
        } else {
            Log.w(TAG, "BLOCKED unauthorized request to: $host")
            logAccess(host, url, allowed = false)
            GlobalScope.launch(Dispatchers.IO) {
                vault.requireScope(
                    scopeType = ScopeType.NETWORK,
                    resource = host,
                    reason = "Network request intercepted",
                    agentContext = "AllowListInterceptor",
                )
            }
            okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(403)
                .message("Blocked by OpenClaw AllowList")
                .body("Blocked".toResponseBody())
                .build()
        }
    }

    private fun inspectResponseForExfiltration(request: okhttp3.Request, response: Response): Response {
        val contentLength = response.body?.contentLength() ?: 0
        if (contentLength > 50 * 1024) {
            Log.w(TAG, "LARGE response from ${request.url.host}: ${contentLength} bytes")
            GlobalScope.launch(Dispatchers.IO) {
                auditLogDao.insert(
                    AuditLogEntry(
                        agentId = "AllowListInterceptor",
                        actionType = "EXFILTRATION_RISK",
                        resource = request.url.host,
                        outcome = "FLAGGED",
                        threatLevel = "HIGH",
                        details = "Large outbound payload: $contentLength bytes",
                    )
                )
            }
        }
        return response
    }

    private fun logAccess(host: String, url: String, allowed: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            auditLogDao.insert(
                AuditLogEntry(
                    agentId = "AllowListInterceptor",
                    actionType = "NETWORK_REQUEST",
                    resource = host,
                    outcome = if (allowed) "ALLOWED" else "BLOCKED",
                    threatLevel = "NONE",
                    details = url,
                )
            )
        }
    }
}
