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
        return OpenClawDatabase.create(context)
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
