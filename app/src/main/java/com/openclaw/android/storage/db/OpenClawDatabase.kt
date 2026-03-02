package com.openclaw.android.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.openclaw.android.storage.models.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        ScopeTokenEntity::class,
        AuditLogEntry::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SandboxProjectEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class OpenClawDatabase : RoomDatabase() {
    abstract fun scopeTokenDao(): ScopeTokenDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun chatDao(): ChatDao
    abstract fun sandboxDao(): SandboxDao

    companion object {
        const val DB_NAME = "openclaw_vault.db"

        fun create(context: Context, passphrase: ByteArray): OpenClawDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context, OpenClawDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
