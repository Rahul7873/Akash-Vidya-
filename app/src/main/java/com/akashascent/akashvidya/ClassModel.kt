package com.akashascent.akashvidya

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ClassModel(
    val id: String? = null,
    val name: String? = null,
    val iconUrl: String? = null,
    val createdAt: Long? = null
)
