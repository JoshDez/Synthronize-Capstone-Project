package com.example.synthronize.model

import com.google.firebase.Timestamp

data class UserModel(
    var fullName: String = "",
    var createdTimestamp: Timestamp = Timestamp.now(),
    var userID: String = "",
    var description:String = "",
    var username:String = "",
    var birthday:String = "",
    var friendsList:List<String> = listOf(),
    var blockList:List<String> = listOf(),
    var friendRequests:List<String> = listOf(),
    var communityInvitations:Map<String, String> = HashMap()
)