package com.example.synthronize.model

import android.net.Uri
import com.google.firebase.Timestamp

data class FeedsModel(
    val feedId: String = "",
    val feedOwnerId: String = "",
    val feedCaption: String = "",
    val feedImages: List<String> = listOf(),
    val feedVideos: List<String> = listOf(),
    val feedTimestamp: Timestamp = Timestamp.now(),
    val communityIdOfOrigin: String = "",
    //id of user who loves the posts
    val usersLoves: List<String> = listOf(),
    //id of users who repost the feed
    val usersReposts: List<String> = listOf(),
    val contentList: List<String> = listOf()
)