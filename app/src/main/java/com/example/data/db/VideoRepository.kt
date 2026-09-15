package com.example.data.db

import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao) {
    val allVideos: Flow<List<RecordedVideoEntity>> = videoDao.getAllVideos()

    suspend fun getVideoById(id: Long): RecordedVideoEntity? = videoDao.getVideoById(id)

    suspend fun insertVideo(video: RecordedVideoEntity): Long = videoDao.insertVideo(video)

    suspend fun updateVideo(video: RecordedVideoEntity) = videoDao.updateVideo(video)

    suspend fun deleteVideo(video: RecordedVideoEntity) = videoDao.deleteVideo(video)

    suspend fun deleteById(id: Long) = videoDao.deleteById(id)

    suspend fun markDlss5Applied(id: Long, newWidth: Int, newHeight: Int, newFps: Int, newSize: Long) =
        videoDao.markDlss5Applied(id, newWidth, newHeight, newFps, newSize)
}
