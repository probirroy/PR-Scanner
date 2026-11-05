package com.example.prscanner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.prscanner.data.model.AuditLogEntity
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.model.DocumentPageEntity
import com.example.prscanner.data.model.FolderEntity
import com.example.prscanner.data.model.SignatureEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentPageEntity::class,
        FolderEntity::class,
        SignatureEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrScannerDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun documentPageDao(): DocumentPageDao
    abstract fun folderDao(): FolderDao
    abstract fun signatureDao(): SignatureDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: PrScannerDatabase? = null

        fun getDatabase(context: Context): PrScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrScannerDatabase::class.java,
                    "pr_scanner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
