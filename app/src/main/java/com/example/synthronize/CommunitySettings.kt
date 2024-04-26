package com.example.synthronize

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityCommunitySettingsBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
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

    override fun onBackPressed() {
        super.onBackPressed()
        //TODO: To be implemented
    }

    private fun bindCommunitySettings(isUserAdmin:Boolean) {
        //Common Binds
        binding.communityNameTV.text = communityModel.communityName
        binding.communityDescriptionTV.text = communityModel.communityDescription
        binding.communityCodeEdtTxt.setText(communityModel.communityCode)
        AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.userProfileCIV)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.copyCodeBtn.setOnClickListener {
            val textToCopy = binding.communityCodeEdtTxt.text.toString()
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Copied Text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        if (isUserAdmin){
            binding.navigationLayout.visibility = View.VISIBLE
            Toast.makeText(this, "Welcome to settings admin", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Welcome to settings user", Toast.LENGTH_SHORT).show()
        }
    }
}