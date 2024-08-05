package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityChatroomSettingsBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil

class ChatroomSettings : AppCompatActivity() {

    private lateinit var binding:ActivityChatroomSettingsBinding
    private lateinit var chatroomModel: ChatroomModel
    private var chatroomName = ""
    private var receiverUid = ""
    private var chatroomType = ""
    private var communityId = ""
    private var chatroomId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //chatroom details
        chatroomId = intent.getStringExtra("chatroomId").toString()
        chatroomType = intent.getStringExtra("chatroomType").toString()

        //for direct message
        chatroomName = intent.getStringExtra("chatroomName").toString()
        receiverUid = intent.getStringExtra("userID").toString()

        //for community chats
        communityId = intent.getStringExtra("communityId").toString()

        FirebaseUtil().retrieveChatRoomReference(chatroomId).get().addOnSuccessListener {
            chatroomModel = it.toObject(ChatroomModel::class.java)!!
            bindChatroomDetails()
            binding.backBtn.setOnClickListener {
                val intent = Intent(this, Chatroom::class.java)
                intent.putExtra("chatroomId", chatroomId)
                intent.putExtra("chatroomType", chatroomType)
                intent.putExtra("chatroomName", chatroomName)
                intent.putExtra("communityId", communityId)
                intent.putExtra("userID", receiverUid)
                startActivity(intent)
                this.finish()
            }
        }.addOnFailureListener {
            binding.backBtn.setOnClickListener {
                onBackPressed()
            }
        }



    }

    private fun bindChatroomDetails(){
        when(chatroomType){
            "direct_message" -> {
                AppUtil().setUserProfilePic(this, receiverUid, binding.chatroomCIV)
                binding.chatroomNameTV.text = chatroomName
            }
            "group_chat" -> {
                AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.chatroomCIV)
                binding.chatroomNameTV.text = chatroomModel.chatroomName
            }
            "community_chat" -> {
                if (chatroomModel.chatroomProfileUrl.isEmpty()){
                    AppUtil().setCommunityProfilePic(this, communityId, binding.chatroomCIV)
                } else {
                    AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.chatroomCIV)
                }
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                    val community = it.toObject(CommunityModel::class.java)!!
                    binding.chatroomNameTV.text = "${chatroomModel.chatroomName} | ${community.communityName}"
                }
            }
        }
    }
}