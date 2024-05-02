package com.example.synthronize.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class DateUtil{

    fun formatTimestampToDate(firebaseTimestamp: Timestamp): String{
        val date: Date = firebaseTimestamp.toDate()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        return dateFormat.format(date)
    }

}