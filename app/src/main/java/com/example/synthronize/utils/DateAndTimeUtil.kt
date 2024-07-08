package com.example.synthronize.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class DateAndTimeUtil{
    //DATE
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

    //TIME


    fun isCurrentTimestampOlderThanMinutes(timestamp: Timestamp, minutes: Long): Boolean {
        // Get the current Firebase Timestamp
        val currentTimestamp = Timestamp.now()
        val currentTimeInMillis = currentTimestamp.toDate().time
        val timestampInMillis = timestamp.toDate().time

        // Calculate the difference in milliseconds
        val differenceInMillis = currentTimeInMillis - timestampInMillis

        // Check if the difference is greater than the specified number of minutes (in milliseconds)
        return differenceInMillis > TimeUnit.MINUTES.toMillis(minutes)
    }

}