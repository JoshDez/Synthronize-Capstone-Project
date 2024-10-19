package com.example.synthronize.model

import com.google.firebase.Timestamp

data class DeletedForumsModel(
    val forumId: String = "",
    val communityId: String = "",
    val ownerId: String = "",
    var upvoteList: MutableList<String> = mutableListOf(),
    var downvoteList: MutableList<String> = mutableListOf(),
    val contentList: List<String> = listOf(),
    val caption: String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
    val archivedTimestamp: Timestamp = Timestamp.now() // Current timestamp for archival
)
