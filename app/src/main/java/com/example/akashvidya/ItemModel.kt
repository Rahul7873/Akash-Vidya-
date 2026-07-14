package com.example.akashvidya

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ItemModel(
    val playlistId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val price: String? = null,
    val thumbnailUrl: String? = null,
    val `class`: String? = null,
    val createdAt: Long? = null
)
