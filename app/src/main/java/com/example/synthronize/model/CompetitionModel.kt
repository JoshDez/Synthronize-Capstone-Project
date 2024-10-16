package com.example.synthronize.model

import com.google.firebase.Timestamp

data class CompetitionModel (
    val competitionId:String = "",
    val competitionName:String = "",
    val description:String = "",
    val rewards: String = "",
    val ownerId:String = "",
    //HashMap<instructionNo, instruction and image>
    val instruction:HashMap<String, List<String>> = hashMapOf(),
    val communityId:String = "",
    //HashMap<resultType, winners' userID> - resultTypes are Top/10 and All
    val results:HashMap<String, List<String>> = hashMapOf(),
    val deadline:Timestamp = Timestamp.now(),
    //HashMap<userID, filename of submission>
    val contestants:HashMap<String, String> = hashMapOf(),
    val createdTimestamp: Timestamp = Timestamp.now()
)