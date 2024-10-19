package com.example.synthronize.model

import com.google.firebase.Timestamp

data class DeletedProductModel(
    val productId: String = "",
    val productName: String = "",
    val productDesc: String = "",
    val communityId: String = "",
    val price: Long = 0,
    val imageList: List<String> = listOf(),
    val available: Boolean = true,
    val ownerId: String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
    val archivedTimestamp: Timestamp = Timestamp.now() // Current timestamp for archival
)
