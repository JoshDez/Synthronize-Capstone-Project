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
    var chatroomProfileUrl:String = ""
)
