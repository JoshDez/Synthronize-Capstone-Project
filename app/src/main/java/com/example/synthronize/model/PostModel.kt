package com.example.synthronize.model

import com.google.firebase.Timestamp

data class PostModel(
    val postId: String = "",
    val communityId: String = "",
    val ownerId: String = "",
    val repostId:String = "",
    val repostOwnerId:String = "",
    val repostList: List<String> = listOf(),
    //id of user who loves the posts
    val loveList: List<String> = listOf(),
    //id of users who repost the feed
    val contentList: List<String> = listOf(),
    val caption: String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
)
