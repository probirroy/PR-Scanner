package com.example.prscanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.prscanner.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE isTrashed = 0 ORDER BY updatedAt DESC")
    fun getAllActiveDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrashed = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrashed = 0 AND folderId = :folderId ORDER BY updatedAt DESC")
    fun getDocumentsByFolder(folderId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedDocuments(): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents 
        WHERE isTrashed = 0 AND (
            title LIKE '%' || :query || '%' 
            OR tags LIKE '%' || :query || '%' 
            OR fullOcrText LIKE '%' || :query || '%'
        ) 
        ORDER BY updatedAt DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun observeDocumentById(id: Long): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isTrashed = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isTrashed = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM documents WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM documents WHERE isTrashed = 0")
    fun countActiveDocuments(): Flow<Int>

    @Query("SELECT SUM(fileSizeEstimateBytes) FROM documents WHERE isTrashed = 0")
    fun getTotalStorageUsedBytes(): Flow<Long?>
}
