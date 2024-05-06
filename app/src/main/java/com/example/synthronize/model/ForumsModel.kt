package com.example.synthronize.model

import com.google.firebase.Timestamp

data class ForumsModel(
    val postId: String = "",
    val communityId: String = "",
    val ownerId: String = "",
    val repostId: String = "",
    val repostOwnerId: String = "",
    val repostList: List<String> = listOf(),

    val loveList: List<String> = listOf(),
    var upvoteList: MutableList<String> = mutableListOf(),  // Change to MutableList
    var downvoteList: MutableList<String> = mutableListOf(),  // Change to MutableList
    val contentList: List<String> = listOf(),
    val caption: String = "",
    val createdTimestamp: Timestamp = Timestamp.now()
)
