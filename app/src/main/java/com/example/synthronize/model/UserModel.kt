package com.example.synthronize.model

import com.google.firebase.Timestamp

data class UserModel(

    var userID: String = "",
    var userType:String = "",
    var fullName: String = "",
    var description:String = "",
    var username:String = "",
    var birthday:String = "",
    var createdTimestamp: Timestamp = Timestamp.now(),
    var friendsList:List<String> = listOf(),
    var blockList:List<String> = listOf(),
    var friendRequests:List<String> = listOf(),
    var communityInvitations:Map<String, String> = HashMap(),
    //keys: profile_cover_photo, profile_photo
    var userMedia:Map<String, String> = HashMap(),
    //keys: lastSeen, toggleOffline, isDeactivated
    var currentStatus:Map<String, Any> = HashMap(),
    //key: postID  value: uid-interaction timestamp
    var notifications:Map<String, Map<String, Timestamp>> = HashMap()
)