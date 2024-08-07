package com.example.synthronize

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityCreateEventBinding
import java.util.Calendar

class CreateEvent : AppCompatActivity() {
    private lateinit var binding:ActivityCreateEventBinding
    private val events = listOf(
        Event(getDateInMillis(2024, 8, 7), "Meeting"),
        Event(getDateInMillis(2024, 8, 14), "Conference")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //TODO
        //binding.calendarView.add


    }

    private fun getDateInMillis(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return calendar.timeInMillis
    }

    data class Event(val date: Long, val title: String)
}