package com.example.akashvidya

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class VideoModel(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val serial: String? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null,
    val thumbnailPath: String? = null,
    val videoPath: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
