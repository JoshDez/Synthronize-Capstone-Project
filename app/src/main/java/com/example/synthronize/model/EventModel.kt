package com.example.synthronize.model

import com.google.firebase.Timestamp

class EventModel (
    val eventId:String = "",
    val eventName:String = "",
    val eventDesc:String = "",
    val eventLocation:String = "",
    val eventOwnerId:String = "",
    val eventImageList: List<String> = listOf(),
    val eventParticipants:List<String> = listOf(),
    val eventDate:Timestamp = Timestamp.now(),
    val communityId:String = "",
    val createdTimestamp: Timestamp = Timestamp.now()
)