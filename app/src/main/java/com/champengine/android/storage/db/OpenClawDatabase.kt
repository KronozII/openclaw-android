package com.champengine.android.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.champengine.android.storage.models.*

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
        fun create(context: Context): OpenClawDatabase {
            return Room.databaseBuilder(
                context,
                OpenClawDatabase::class.java,
                "openclaw_vault.db"
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
