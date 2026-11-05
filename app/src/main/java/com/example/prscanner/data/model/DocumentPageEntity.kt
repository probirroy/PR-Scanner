package com.example.prscanner.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageNumber: Int,
    val imageFilePath: String,
    val originalFilePath: String = "",
    val filterType: String = FilterType.ORIGINAL.name,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val rotationDegrees: Int = 0,
    val ocrText: String = ""
)
