package com.example.synthronize.model

import com.google.firebase.Timestamp

data class FileModel (
    val fileId:String = "",
    val fileName:String = "",
    val fileUrl:String = "",
    val ownerId:String = "",
    val shareFile:Boolean = true,
    val caption:String = "",
    val communityId:String = "",
    val createdTimestamp:Timestamp = Timestamp.now()
)