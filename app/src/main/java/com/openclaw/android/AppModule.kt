package com.openclaw.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openclaw.android.network.AllowListInterceptor
import com.openclaw.android.storage.db.OpenClawDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Database passphrase — derived from Android Keystore master key.
     * This ensures the DB is encrypted and tied to the device.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): OpenClawDatabase {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Use EncryptedSharedPreferences to store the DB passphrase
        val prefs = EncryptedSharedPreferences.create(
            context,
            "openclaw_vault_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        val passphraseKey = "db_passphrase"
        val passphrase = prefs.getString(passphraseKey, null) ?: run {
            // Generate a new random 32-byte passphrase on first install
            val bytes = java.security.SecureRandom().generateSeed(32)
            val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            prefs.edit().putString(passphraseKey, encoded).apply()
            encoded
        }

        return OpenClawDatabase.create(context, passphrase.toByteArray(Charsets.UTF_8))
    }

    @Provides fun provideScopeTokenDao(db: OpenClawDatabase) = db.scopeTokenDao()
    @Provides fun provideAuditLogDao(db: OpenClawDatabase) = db.auditLogDao()
    @Provides fun provideChatDao(db: OpenClawDatabase) = db.chatDao()
    @Provides fun provideSandboxDao(db: OpenClawDatabase) = db.sandboxDao()

    /**
     * OkHttp client — AllowList interceptor installed as a NETWORK interceptor
     * so it fires on every real network call, not just app-level calls.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        allowListInterceptor: AllowListInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(allowListInterceptor)  // network-level — cannot be bypassed
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false) // don't silently follow redirects to unexpected domains
        .build()
}
