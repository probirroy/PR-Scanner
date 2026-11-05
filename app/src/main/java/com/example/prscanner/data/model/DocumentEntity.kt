package com.example.prscanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folderId: Long? = null,
    val tags: String = "", // Comma-separated tags
    val pageCount: Int = 1,
    val thumbnailUri: String = "",
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val docType: String = DocType.GENERAL.name,
    val ocrSummary: String = "",
    val fullOcrText: String = "",
    val passwordProtected: Boolean = false,
    val syncStatus: String = SyncStatus.LOCAL_ONLY.name,
    val fileSizeEstimateBytes: Long = 0L
)
