package com.example.synthronize.model

import com.google.firebase.Timestamp

data class CommentModel(
    var commentId:String = "",
    var commentOwnerId: String = "",
    var comment: String = "",
    var commentTimestamp: Timestamp = Timestamp.now()
)