package com.example.synthronize.model

import com.google.firebase.Timestamp

data class CompetitionModel (
    val competitionId:String = "",
    val competitionName:String = "",
    val description:String = "",
    val instruction:String = "",
    val rewards: String = "",
    val ownerId:String = "",
    val contentList:List<String> = listOf(),
    val deadline:Timestamp = Timestamp.now(),
    //HashMap<userID, filename of submission>
    val contestants:HashMap<String, String> = hashMapOf(),
    val createdTimestamp: Timestamp = Timestamp.now()
)