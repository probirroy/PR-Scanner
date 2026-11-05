package com.example.prscanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.prscanner.data.model.DocumentPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentPageDao {
    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesListForDocument(documentId: Long): List<DocumentPageEntity>

    @Query("SELECT * FROM document_pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: Long): DocumentPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: DocumentPageEntity)

    @Query("DELETE FROM document_pages WHERE id = :pageId")
    suspend fun deletePage(pageId: Long)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)

    @Query("SELECT COUNT(*) FROM document_pages")
    fun getTotalPageCount(): Flow<Int>
}
