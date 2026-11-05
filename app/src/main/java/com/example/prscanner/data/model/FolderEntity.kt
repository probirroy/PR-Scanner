package com.example.prscanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#2563EB",
    val iconName: String = "folder",
    val createdAt: Long = System.currentTimeMillis()
)
