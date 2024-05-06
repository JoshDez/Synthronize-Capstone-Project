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

    fun formatBirthDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("M/d/yy", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

}