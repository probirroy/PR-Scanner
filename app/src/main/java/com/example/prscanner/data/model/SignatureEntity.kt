package com.example.prscanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val imageFilePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
