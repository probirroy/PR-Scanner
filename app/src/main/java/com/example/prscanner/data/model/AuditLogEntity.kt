package com.example.prscanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String,
    val targetName: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
