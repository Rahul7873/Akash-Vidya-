package com.example.akashvidya

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PreferenceModel(
    val id: String? = null,
    val name: String? = null,
    val createdAt: Long? = null,
    val classes: Map<String, Any>? = null
)
