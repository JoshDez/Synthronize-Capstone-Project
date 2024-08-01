package com.example.synthronize.model

import com.google.firebase.Timestamp

data class UserModel(
    //user ID
    var userID: String = "",
    //Identifies a Student, Teacher, or App Admin
    var userType:String = "",
    //Full name of user
    var fullName: String = "",
    //description of user
    var description:String = "",
    //username of user
    var username:String = "",
    //birthday of user
    var birthday:String = "",
    //date of creation of the user account
    var createdTimestamp: Timestamp = Timestamp.now(),
    //friends list of user that contains userIDs
    var friendsList:List<String> = listOf(),
    //block list of user that contains userIDs
    var blockList:List<String> = listOf(),
    //list of friend requests
    var friendRequests:List<String> = listOf(),
    //community invitations received by the user
    var communityInvitations:Map<String, String> = HashMap(),
    //keys: profile_cover_photo, profile_photo
    var userMedia:Map<String, String> = HashMap(),
    //keys: lastSeen, toggleOffline, isDeactivated
    var currentStatus:Map<String, Any> = HashMap(),
    //key: postID  value: uid-interaction timestamp
    var notifications:Map<String, Map<String, Timestamp>> = HashMap(),
    //fcm token for receiving notifications
    var fcmToken:String = "",
)