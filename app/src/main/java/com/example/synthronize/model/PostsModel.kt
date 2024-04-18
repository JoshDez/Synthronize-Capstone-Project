package com.example.synthronize.model

data class PostsModel(
    val postId: String,
    val userId: String,
    val content: String,
    val timestamp: Long
)