package com.example.synthronize.model

import com.google.firebase.Timestamp

data class ReportModel (
    //id of report
    val reportId: String = "",
    //type of report (eg Community, User, Post, File, Product, Forum)
    val reportType: String = "",
    //id of the one who filed the report
    val ownerId: String = "",
    //id of the reported user
    val reportedId: String = "",
    //state of the report if its reviewed
    val reviewed : Boolean = false,
    //reason for the report
    val reason:String = "",
    //time when the report is created
    val createdTimestamp: Timestamp = Timestamp.now(),
)