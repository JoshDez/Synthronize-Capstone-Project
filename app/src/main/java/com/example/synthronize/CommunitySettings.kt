package com.example.synthronize

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityCommunitySettingsBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

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
        //Common Binds
        binding.communityNameTV.text = communityModel.communityName
        binding.communityCodeEdtTxt.setText(communityModel.communityCode)
        AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.userProfileCIV)

        if (communityModel.communityDescription.isNotEmpty()){
            binding.communityDescriptionTV.visibility = View.VISIBLE
            binding.communityDescriptionTV.text = communityModel.communityDescription
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.leaveCommunityBtn.setOnClickListener {
            val dialogPlusBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(dialogPlusBinding.root))
                .setGravity(Gravity.CENTER)
                .setMargin(50, 800, 50, 800)
                .setCancelable(true)
                .create()

            dialogPlusBinding.titleTV.text = "Warning!"
            dialogPlusBinding.messageTV.text = "Do you want to leave this community?"
            dialogPlusBinding.yesBtn.setOnClickListener {
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                    .update("communityAdmin", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                    .update("communityMembers", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                        leaveAllCommunityChannels()
                        AppUtil().headToMainActivity(this)
                    }
            }
            dialogPlusBinding.NoBtn.setOnClickListener {
                dialogPlus.dismiss()
            }
            dialogPlus.show()
        }

        binding.copyCodeBtn.setOnClickListener {
            val textToCopy = binding.communityCodeEdtTxt.text.toString()
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Copied Text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.viewMembersBtn.setOnClickListener {
            val intent = Intent(applicationContext, Members::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        if (communityModel.communityType == "Private"){
            binding.viewJoinRequestsBtn.visibility = View.VISIBLE
            binding.viewJoinRequestsBtn.setOnClickListener {
                val intent = Intent(this, Requests::class.java)
                intent.putExtra("communityId", communityModel.communityId)
                startActivity(intent)
            }
        }
        if (isUserAdmin){
            binding.navigationLayout.visibility = View.VISIBLE
        }
    }
    private fun leaveAllCommunityChannels() {
        FirebaseUtil().retrieveCommunityDocument(communityModel.communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            if (communityModel.communityChannels.isNotEmpty()){
                for (channel in communityModel.communityChannels){
                    //removes user from community channel chatroom
                    FirebaseUtil().retrieveAllChatRoomReferences().document("${communityModel.communityId}-$channel")
                        .update("userIdList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                }
            }
        }
    }
}