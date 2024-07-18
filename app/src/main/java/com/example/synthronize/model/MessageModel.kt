package com.example.synthronize.model

import com.google.firebase.Timestamp

data class MessageModel(
    var message:String = "",
    var postID:String = "",
    var senderID:String = "",
    var timestamp: Timestamp = Timestamp.now()
)
