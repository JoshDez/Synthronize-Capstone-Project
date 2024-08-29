package com.example.synthronize.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class DateAndTimeUtil{

    //Time ago since timestamp
    fun getTimeAgo(timestamp: Timestamp, isShortened:Boolean = true): String {
        val now = System.currentTimeMillis()
        val past = timestamp.toDate().time

        val diff = now - past

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        if (isShortened){
            return when {
                seconds < 60 -> "${seconds}s"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                weeks < 4 -> "${weeks}w"
                months < 12 -> "${months}mo"
                else -> "${years}yr"
            }
        } else {
            return when {
                seconds < 60 -> "$seconds seconds ago"
                minutes < 60 -> "$minutes months ago"
                hours < 24 -> "$hours hours ago"
                days < 7 -> "$days days ago"
                weeks < 4 -> "$weeks weeks ago"
                months < 12 -> "$months months ago"
                else -> "$years years ago"
            }
        }
    }

    //DATE
    fun formatTimestampToDate(firebaseTimestamp: Timestamp): String{
        val date: Date = firebaseTimestamp.toDate()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        return dateFormat.format(date)
    }

    fun convertDateToTimestamp(dateString:String): Timestamp{
        // Define the date format
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        // Parse the string to a Date object
        val date: Date? = sdf.parse(dateString)

        // Convert Date to Firebase Timestamp
        return date?.let { Timestamp(it) }!!
    }

    fun formatBirthDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("M/d/yy", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    //Checks if a given Firebase Timestamp is due compared to the current time.
    fun isTimestampDue(firebaseTimestamp: Timestamp, callback: (Boolean, Long) -> Unit) {
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Get the provided timestamp time in milliseconds
        val providedTime = firebaseTimestamp.toDate().time

        // Calculate the time difference in milliseconds
        val timeDifference = providedTime - currentTime

        // Convert time difference to days
        val daysLeft = TimeUnit.MILLISECONDS.toDays(timeDifference)

        // Determine if the timestamp is due
        val isDue = timeDifference <= 0

        // Invoke the callback with the result
        callback(isDue, daysLeft)
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