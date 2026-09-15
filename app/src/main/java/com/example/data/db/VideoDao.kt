package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM recorded_videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<RecordedVideoEntity>>

    @Query("SELECT * FROM recorded_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): RecordedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: RecordedVideoEntity): Long

    @Update
    suspend fun updateVideo(video: RecordedVideoEntity)

    @Delete
    suspend fun deleteVideo(video: RecordedVideoEntity)

    @Query("DELETE FROM recorded_videos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recorded_videos SET hasDlss5Applied = 1, width = :newWidth, height = :newHeight, fps = :newFps, fileSizeBytes = :newSize WHERE id = :id")
    suspend fun markDlss5Applied(id: Long, newWidth: Int, newHeight: Int, newFps: Int, newSize: Long)
}
