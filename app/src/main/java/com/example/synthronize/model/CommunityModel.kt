package com.example.synthronize.model

import com.google.firebase.Timestamp

data class CommunityModel(
    var communityId: String = "",
    var communityName: String = "",
    var communityDescription: String = "",
    var communityType: String = "",
    var communityCode: String = "",
    var communityCreatedTimestamp: Timestamp = Timestamp.now(),
    //Map < userID, role >
    var communityMembers:Map<String, String> = HashMap(),
    var communityMedia:Map<String, String> = HashMap(),
    var joinRequestList: List<String> = listOf(),
    var bannedUsers: List<String> = listOf(),
    var communityRules: List<String> = listOf()
)