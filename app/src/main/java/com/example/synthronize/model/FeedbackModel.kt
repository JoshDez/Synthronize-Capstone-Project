package com.example.synthronize.model

import com.google.firebase.Timestamp

data class FeedbackModel(
    var feedbackID:String = "",
    var feedbackType:String = "",
    var feedbackMessage:String = "",
    val fbReviewed:Boolean = false,
    var feedbackTimestamp:Timestamp = Timestamp.now(),
)
