package com.akashascent.akashvidya

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

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
    val localPath: String? = null,
    
    @get:PropertyName("isDemo")
    @set:PropertyName("isDemo")
    var isDemo: Boolean = false,

    val playlistId: String? = null,
    val playlistName: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
