package com.champengine.android.storage.db

import androidx.room.*
import com.champengine.android.storage.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScopeTokenDao {
    @Query("SELECT * FROM scope_tokens WHERE isActive = 1 ORDER BY grantedAt DESC")
    fun observeAll(): Flow<List<ScopeTokenEntity>>

    @Query("SELECT * FROM scope_tokens WHERE scopeType = :type AND resource = :resource AND isActive = 1 LIMIT 1")
    suspend fun findActive(type: String, resource: String): ScopeTokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: ScopeTokenEntity)

    @Query("UPDATE scope_tokens SET isActive = 0, lastUsed = :revokedAt WHERE id = :id")
    suspend fun revoke(id: String, revokedAt: Long)

    @Query("UPDATE scope_tokens SET isActive = 0, lastUsed = :revokedAt WHERE scopeType = :type")
    suspend fun revokeByType(type: String, revokedAt: Long)

    @Query("UPDATE scope_tokens SET isActive = 0, lastUsed = :revokedAt")
    suspend fun revokeAll(revokedAt: Long)

    @Query("UPDATE scope_tokens SET usageCount = usageCount + 1, lastUsed = :ts WHERE id = :id")
    suspend fun incrementUsage(id: String, ts: Long)

    @Query("SELECT COUNT(*) FROM scope_tokens WHERE isActive = 1")
    suspend fun countActive(): Int
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<AuditLogEntry>>

    @Query("SELECT * FROM audit_log WHERE threatLevel != 'NONE' ORDER BY timestamp DESC")
    fun observeThreats(): Flow<List<AuditLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditLogEntry)

    @Query("SELECT * FROM audit_log WHERE agentId = :agentId ORDER BY timestamp DESC LIMIT 50")
    suspend fun byAgent(agentId: String): List<AuditLogEntry>

    @Query("DELETE FROM audit_log WHERE timestamp < :before")
    suspend fun pruneOlderThan(before: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET updatedAt = :ts, messageCount = messageCount + 1 WHERE id = :id")
    suspend fun touchSession(id: String, ts: Long)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :id")
    suspend fun deleteMessages(id: String)
}

@Dao
interface SandboxDao {
    @Query("SELECT * FROM sandbox_projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SandboxProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: SandboxProjectEntity)

    @Query("DELETE FROM sandbox_projects WHERE id = :id")
    suspend fun delete(id: String)
}
