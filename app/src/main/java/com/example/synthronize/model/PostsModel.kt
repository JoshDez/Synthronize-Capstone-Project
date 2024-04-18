package com.labactivity.synthronize

data class PostsModel(
    val postId: String,
    val userId: String,
    val content: String,
    val timestamp: Long
)