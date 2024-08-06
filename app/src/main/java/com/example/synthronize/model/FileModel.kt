package com.example.synthronize.model

import com.google.firebase.Timestamp

data class FileModel (
    val fileId:String = "",
    val fileName:String = "",
    val fileUrl:String = "",
    val fileOwnerId:String = "",
    val shareFile:Boolean = true,
    val caption:String = "",
    val communityId:String = "",
    val uploadTimestamp:Timestamp = Timestamp.now()
)