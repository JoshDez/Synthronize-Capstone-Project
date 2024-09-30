package com.example.synthronize.model

import com.google.firebase.Timestamp

data class ThreadModel(
    var threadId: String= "",
    var commentOwnerId: String = "",
    var comment: String = "",
    var commentTimestamp: Timestamp = Timestamp.now(),
    val contentList: List<String> = listOf(),
    val communityId: String = "",
    var upvoteList: MutableList<String> = mutableListOf(),  // Change to MutableList
    var downvoteList: MutableList<String> = mutableListOf(),  // Change to MutableList
)