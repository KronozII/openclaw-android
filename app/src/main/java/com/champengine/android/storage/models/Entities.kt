package com.champengine.android.storage.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scope_tokens",
    indices = [
        Index(value = ["scopeType", "resource", "isActive"]),
        Index(value = ["isActive"])
    ]
)
data class ScopeTokenEntity(
    @PrimaryKey val id: String,
    val scopeType: String,
    val resource: String,
    val grantedAt: Long,
    val expiresAt: Long?,
    val grantedBy: String,
    val reason: String,
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val isActive: Boolean = true,
    val hmacSignature: String,
)

@Entity(tableName = "audit_log")
data class AuditLogEntry(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val agentId: String,           // "primary" | "sentinel" | "sandbox"
    val actionType: String,        // "NETWORK_REQUEST" | "FILE_READ" | etc.
    val resource: String,
    val scopeTokenId: String?,     // which token authorized this (null = blocked)
    val outcome: String,           // "ALLOWED" | "BLOCKED" | "THREAT_DETECTED"
    val threatLevel: String = "NONE",  // "NONE" | "LOW" | "HIGH" | "CRITICAL"
    val details: String = "",      // JSON payload summary (truncated, no PII)
    val isTamperSigned: Boolean = false,
    val signature: String = "",
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
)

@Entity(tableName = "chat_messages", indices = [Index("sessionId")])
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,              // "user" | "assistant" | "system" | "sentinel"
    val content: String,
    val timestamp: Long,
    val isError: Boolean = false,
    val metadata: String = "",     // JSON extra info
)

@Entity(tableName = "sandbox_projects")
data class SandboxProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val projectType: String,       // "web" | "game" | "app"
    val createdAt: Long,
    val updatedAt: Long,
    val mainFilePath: String,
    val description: String = "",
)
