package com.example.synthronize.model

import com.google.firebase.Timestamp

data class PostModel(
    val postId: String = "",
    val communityId: String = "",
    val ownerId: String = "",
    //id of users who send the post
    val sendPostList: List<String> = listOf(),
    //id of users who loves the posts
    val loveList: List<String> = listOf(),
    //id of content to access firebase storage
    val contentList: List<String> = listOf(),
    val caption: String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
)
