package com.openclaw.android.network

import android.util.Log
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.models.AuditLogEntry
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllowListInterceptor @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) : Interceptor {

    private val TAG = "AllowListInterceptor"
    private val permanentAllowList = setOf("localhost", "127.0.0.1")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val url = request.url.toString()

        if (host in permanentAllowList) return chain.proceed(request)

        val isGranted = runBlocking {
            vault.isGranted(ScopeType.NETWORK, host) ||
            vault.isGranted(ScopeType.NETWORK, "*")
        }

        return if (isGranted) {
            logAccess(host, url, allowed = true)
            chain.proceed(request)
        } else {
            Log.w(TAG, "BLOCKED: $host")
            logAccess(host, url, allowed = false)
            okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(403)
                .message("Blocked by OpenClaw")
                .body("{}".toResponseBody())
                .build()
        }
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
}
