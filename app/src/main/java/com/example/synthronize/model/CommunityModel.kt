package com.example.synthronize.model

data class CommunityModel(
    val communityId: String = "",
    val communityName: String = "",
    val communityDescription: String = "",
    val communityType: String = "",
    val communityCode: String = "",
    val communityMembers: List<String> = listOf(),
    val communityAdmin: List<String> = listOf()
    // Add other properties as needed
)