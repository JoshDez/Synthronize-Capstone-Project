package com.example.synthronize.model

import com.google.firebase.Timestamp

data class DeletedPostModel(
    val postId: String = "",
    val communityId: String = "",
    val ownerId: String = "",
    val sendPostList: List<String> = listOf(),
    val loveList: List<String> = listOf(),
    val contentList: List<String> = listOf(),
    val caption: String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
    val archivedTimestamp: Timestamp = Timestamp.now()  // to keep track of when it was archived
)
