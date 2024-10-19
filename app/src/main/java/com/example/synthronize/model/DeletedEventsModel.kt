package com.example.synthronize.model

import com.google.firebase.Timestamp

data class DeletedEventsModel(
    val eventId: String = "",
    val eventName: String = "",
    val eventDesc: String = "",
    val eventLocation: String = "",
    val eventOwnerId: String = "",
    val eventImageName: String = "",
    val eventParticipants: List<String> = listOf(),
    val eventDate: Timestamp = Timestamp.now(),
    val communityId: String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
    val archivedTimestamp: Timestamp = Timestamp.now() // Current timestamp for archival
)
