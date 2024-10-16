package com.example.synthronize.model

import com.google.firebase.Timestamp

data class ChatroomModel(
    var chatroomId:String = "",
    var chatroomType:String = "",
    var userIdList:List<String> = listOf(),
    var lastMsgTimestamp:Timestamp = Timestamp.now(),
    var lastMessage:String = "",
    var lastMessageUserId:String = "",
    var chatroomName:String = "",
    var chatroomProfileUrl:String = "",
    var chatroomAdminList:List<String> = listOf(),
    var communityId:String = "",
    var usersMute:List<String> = listOf(),
    var usersSeen:List<String> = listOf()
)
