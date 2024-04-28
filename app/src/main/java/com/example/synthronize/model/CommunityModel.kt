package com.example.synthronize.model

import com.google.firebase.Timestamp

data class CommunityModel(
    var communityId: String = "",
    var communityName: String = "",
    var communityDescription: String = "",
    var communityType: String = "",
    var communityCode: String = "",
    var communityCreatedTimestamp: Timestamp = Timestamp.now(),
    var communityMembers: List<String> = listOf(),
    var communityAdmin: List<String> = listOf(),
    var joinRequestList: List<String> = listOf(),
    var blockList: List<String> = listOf()
)