package com.example.synthronize

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityCreateCompetitionBinding

class CreateCompetition : AppCompatActivity() {
    private lateinit var binding:ActivityCreateCompetitionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCompetitionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}