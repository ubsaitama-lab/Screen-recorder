package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recorded_videos")
data class RecordedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val fps: Int,
    val codec: String,
    val bitrateBps: Int,
    val hasDlss5Applied: Boolean,
    val dlss5QualityFactor: Float = 1.0f,
    val isRealtimeDlss5: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
