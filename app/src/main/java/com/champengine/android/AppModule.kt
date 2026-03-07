package com.champengine.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.champengine.android.network.AllowListInterceptor
import com.champengine.android.storage.db.ChampEngineDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
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
    ): ChampEngineDatabase {
        return ChampEngineDatabase.create(context)
}

    @Provides fun provideScopeTokenDao(db: ChampEngineDatabase) = db.scopeTokenDao()
    @Provides fun provideAuditLogDao(db: ChampEngineDatabase) = db.auditLogDao()
    @Provides fun provideChatDao(db: ChampEngineDatabase) = db.chatDao()
    @Provides fun provideSandboxDao(db: ChampEngineDatabase) = db.sandboxDao()

    /**
     * OkHttp client — AllowList interceptor installed as a NETWORK interceptor
     * so it fires on every real network call, not just app-level calls.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        allowListInterceptor: AllowListInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .addNetworkInterceptor(allowListInterceptor)  // network-level — cannot be bypassed
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        //.followRedirects(false) // don't silently follow redirects to unexpected domains
        .build()
}
