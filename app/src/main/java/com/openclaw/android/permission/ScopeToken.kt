package com.openclaw.android.permission

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Scope types — every possible permission category
 */
enum class ScopeType(val prefix: String, val displayName: String, val icon: String) {
    NETWORK("NET", "Network Access", "🌐"),
    FILE_READ("FILE_R", "File Read", "📄"),
    FILE_WRITE("FILE_W", "File Write", "✏️"),
    CAMERA("CAM", "Camera", "📷"),
    MICROPHONE("MIC", "Microphone", "🎙️"),
    CONTACTS("CONTACTS", "Contacts", "👥"),
    LOCATION("LOC", "Location", "📍"),
    NOTIFICATIONS("NOTIF", "Send Notifications", "🔔"),
    CLIPBOARD("CLIP", "Clipboard", "📋"),
    MEDIA_READ("MEDIA_R", "Read Media", "🖼️"),
    SANDBOX_NET("SBX_NET", "Sandbox Network", "🧪"),
    SOCIAL_POST("SOCIAL", "Social Post", "📢"),
    BACKGROUND("BG", "Background Operation", "⚡"),
}

/**
 * A single granted scope — HMAC-signed, stored in PermissionVault
 */
@Serializable
data class ScopeToken(
    val id: String,                    // uuid
    val scopeType: String,             // ScopeType.prefix
    val resource: String,              // domain, path, or "*"
    val grantedAt: Long,               // epoch millis
    val expiresAt: Long?,              // null = permanent until revoked
    val grantedBy: String = "user",    // always "user" — agents cannot self-grant
    val reason: String,                // human-readable why this was needed
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val isActive: Boolean = true,
    val hmacSignature: String = ""     // HMAC-SHA256 over canonical fields
) {
    /**
     * Canonical string for signing — order matters, cannot be manipulated
     */
    fun canonicalString(): String = "$id|$scopeType|$resource|$grantedAt|$grantedBy"

    fun isExpired(): Boolean = expiresAt != null && System.currentTimeMillis() > expiresAt

    fun isValid(): Boolean = isActive && !isExpired()

    fun displayLabel(): String {
        val type = ScopeType.entries.firstOrNull { it.prefix == scopeType }
        return "${type?.icon ?: "🔑"} ${type?.displayName ?: scopeType}: $resource"
    }
}

/**
 * Permission request — raised by an agent, resolved by user
 */
data class PermissionRequest(
    val requestId: String,
    val scopeType: ScopeType,
    val resource: String,
    val reason: String,                // "I need this to check the weather"
    val agentContext: String,          // what the agent was doing
    val allowOnce: Boolean = false,    // user can grant single-use only
)

/**
 * User's decision on a permission request
 */
sealed class PermissionDecision {
    data class AllowPermanent(val requestId: String) : PermissionDecision()
    data class AllowOnce(val requestId: String) : PermissionDecision()
    data class Deny(val requestId: String) : PermissionDecision()
    data class DenyAndBlock(val requestId: String) : PermissionDecision() // never ask again
}

/**
 * HMAC signer using Android Keystore — tokens cannot be forged by agent code
 */
object ScopeTokenSigner {
    private const val KEYSTORE_ALIAS = "openclaw_scope_signing_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setUserAuthenticationRequired(false) // scopes don't require biometric, vault access does
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun sign(canonical: String): String {
        val key = getOrCreateKey()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        val bytes = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verify(canonical: String, signature: String): Boolean {
        return try {
            val expected = sign(canonical)
            expected == signature
        } catch (e: Exception) {
            false
        }
    }
}
