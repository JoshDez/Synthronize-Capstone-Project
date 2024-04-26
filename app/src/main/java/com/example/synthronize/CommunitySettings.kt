package com.example.synthronize

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityCommunitySettingsBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.FirebaseUtil

class CommunitySettings : AppCompatActivity() {
    private lateinit var binding:ActivityCommunitySettingsBinding
    private lateinit var communityModel: CommunityModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val communityId = intent.getStringExtra("communityId").toString()
        val isUserAdmin = intent.getBooleanExtra("isUserAdmin", false)

        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            communityModel = it.toObject(CommunityModel::class.java)!!
            bindCommunitySettings(isUserAdmin)
        }

    }

    private fun bindCommunitySettings(isUserAdmin:Boolean) {
        if (isUserAdmin){
            Toast.makeText(this, "Welcome to settings admin", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Welcome to settings user", Toast.LENGTH_SHORT).show()
        }
    }
}