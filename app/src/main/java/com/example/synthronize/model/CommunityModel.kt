package com.example.synthronize.model

import com.google.firebase.Timestamp

data class CommunityModel(
    val communityId: String = "",
    val communityName: String = "",
    val communityDescription: String = "",
    val communityType: String = "",
    val communityCode: String = "",
    val communityCreatedTimestamp: Timestamp = Timestamp.now(),
    val communityMembers: List<String> = listOf(),
    val communityAdmin: List<String> = listOf(),
    val joinRequestList: List<String> = listOf(),
    val blockList: List<String> = listOf()
)