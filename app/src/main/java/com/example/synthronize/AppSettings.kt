package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityAppSettingsBinding

class AppSettings : AppCompatActivity() {
    private lateinit var binding: ActivityAppSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.reportsFiledBtn.setOnClickListener {
            val intent = Intent(this, Reports::class.java)
            intent.putExtra("isPersonalReport", true)
            startActivity(intent)
        }
    }
}