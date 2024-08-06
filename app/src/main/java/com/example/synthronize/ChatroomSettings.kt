package com.example.synthronize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.databinding.ActivityChatroomSettingsBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class ChatroomSettings : AppCompatActivity() {

    private lateinit var binding:ActivityChatroomSettingsBinding
    private lateinit var chatroomModel: ChatroomModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedImage:Uri
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
                onBackPressed()
            }
        }.addOnFailureListener {
            binding.backBtn.setOnClickListener {
                onBackPressed()
            }
        }

        //For Creating Group Chat
        //Launcher for user profile pic and user cover pic
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    selectedImage = data.data!!
                    Glide.with(this)
                        .load(selectedImage)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.editChatroomCIV)
                }
            }
        }



    }

    override fun onBackPressed() {
        val intent = Intent(this, Chatroom::class.java)
        intent.putExtra("chatroomId", chatroomId)
        intent.putExtra("chatroomType", chatroomType)
        intent.putExtra("chatroomName", chatroomName)
        intent.putExtra("communityId", communityId)
        intent.putExtra("userID", receiverUid)
        startActivity(intent)
        this.finish()
        super.onBackPressed()
    }

    private fun bindChatroomDetails(){
        when(chatroomType){
            "direct_message" -> {
                AppUtil().setUserProfilePic(this, receiverUid, binding.chatroomCIV)
                AppUtil().setUserProfilePic(this, receiverUid, binding.editChatroomCIV)
                binding.chatroomNameTV.text = chatroomName
                binding.viewProfileBtn.visibility = View.VISIBLE
                binding.viewProfileBtn.setOnClickListener {
                    val intent = Intent(this, OtherUserProfile::class.java)
                    intent.putExtra("userID", receiverUid)
                    startActivity(intent)
                }
            }
            "group_chat" -> {
                if (chatroomModel.chatroomProfileUrl.isNotEmpty()){
                    AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.chatroomCIV)
                    AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.editChatroomCIV)
                }
                binding.chatroomNameTV.text = chatroomModel.chatroomName
                binding.viewMembersBtn.visibility = View.VISIBLE
                binding.viewMembersBtn.setOnClickListener {
                    val intent = Intent(this, Members::class.java)
                    intent.putExtra("forChatroomMembers", true)
                    intent.putExtra("chatroomType", chatroomType)
                    intent.putExtra("chatroomId", chatroomId)
                    startActivity(intent)
                }
                binding.leaveConversationBtn.visibility = View.VISIBLE
                binding.leaveConversationBtn.setOnClickListener {
                    val dialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                    val dialogPlus = DialogPlus.newDialog(this)
                        .setContentHolder(ViewHolder(dialogBinding.root))
                        .setGravity(Gravity.CENTER)
                        .setMargin(50, 800, 50, 800)
                        .setCancelable(true)
                        .create()

                    dialogBinding.titleTV.text = "Are you sure?"
                    dialogBinding.messageTV.text = "Are you sure you want to leave this conversation?"

                    dialogBinding.yesBtn.setOnClickListener {
                        if (chatroomModel.chatroomAdminList.size == 1 && chatroomModel.chatroomAdminList.contains(FirebaseUtil().currentUserUid())
                            && chatroomModel.userIdList.size >= 2){
                            FirebaseUtil().retrieveChatRoomReference(chatroomId).update("chatroomAdminList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                                FirebaseUtil().retrieveChatRoomReference(chatroomId).update("userIdList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                                    FirebaseUtil().retrieveChatRoomReference(chatroomId).get().addOnSuccessListener { chatroom ->
                                        val model = chatroom.toObject(ChatroomModel::class.java)!!
                                        FirebaseUtil().retrieveChatRoomReference(chatroomId).update("chatroomAdminList", FieldValue.arrayUnion(model.userIdList[0])).addOnSuccessListener {
                                            this.finish()
                                        }
                                    }
                                }
                            }
                        } else {
                            FirebaseUtil().retrieveChatRoomReference(chatroomId).update("userIdList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                                this.finish()
                            }
                        }
                    }
                    dialogBinding.NoBtn.setOnClickListener {
                        dialogPlus.dismiss()
                    }

                    dialogPlus.show()
                }
                showAdminButtons()
            }
            "community_chat" -> {
                if (chatroomModel.chatroomProfileUrl.isEmpty()){
                    AppUtil().setCommunityProfilePic(this, communityId, binding.chatroomCIV)
                    AppUtil().setCommunityProfilePic(this, communityId, binding.editChatroomCIV)
                } else {
                    AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.chatroomCIV)
                    AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.editChatroomCIV)
                }
                binding.chatroomNameTV.text = "${chatroomModel.chatroomName}"
                binding.viewMembersBtn.visibility = View.VISIBLE
                binding.viewMembersBtn.setOnClickListener {
                    val intent = Intent(this, Members::class.java)
                    intent.putExtra("forChatroomMembers", true)
                    intent.putExtra("chatroomType", chatroomType)
                    intent.putExtra("chatroomId", chatroomId)
                    intent.putExtra("communityId", communityId)
                    startActivity(intent)
                }
                showAdminButtons()
            }
        }
    }

    private fun showAdminButtons(){
        if (AppUtil().isIdOnList(chatroomModel.chatroomAdminList, FirebaseUtil().currentUserUid())){
            binding.editChatroomDetailsBtn.visibility = View.VISIBLE
            binding.editChatroomDetailsBtn.setOnClickListener {
                editChatroomDetails()
            }
            if (chatroomType == "community_chat"){
                binding.deleteTextChannelBtn.visibility = View.VISIBLE
                binding.deleteTextChannelBtn.setOnClickListener {
                    val dialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                    val dialogPlus = DialogPlus.newDialog(this)
                        .setContentHolder(ViewHolder(dialogBinding.root))
                        .setGravity(Gravity.CENTER)
                        .setMargin(50, 800, 50, 800)
                        .setCancelable(true)
                        .create()
                    dialogBinding.titleTV.text = "Are you sure?"
                    dialogBinding.messageTV.text = "Are you sure you want to delete this text channel?"
                    dialogBinding.yesBtn.setOnClickListener {
                        FirebaseUtil().retrieveChatRoomReference(chatroomId).delete().addOnSuccessListener {
                            this.finish()
                        }
                    }
                    dialogBinding.NoBtn.setOnClickListener {
                        dialogPlus.dismiss()
                    }
                    dialogPlus.show()
                }
            }
        }
    }

    private fun editChatroomDetails() {
        binding.chatroomMenuLayout.visibility = View.GONE
        binding.editChatroomButtonsLayout.visibility = View.VISIBLE
        binding.chatroomNameEdtTxt.setText(binding.chatroomNameTV.text.toString())
        binding.chatroomNameTV.visibility = View.GONE
        binding.chatroomNameEdtTxt.visibility = View.VISIBLE

        if (chatroomType == "group_chat"){
            binding.chatroomCIV.visibility = View.GONE
            binding.editChatroomCIV.visibility = View.VISIBLE
            binding.editChatroomCIV.setOnClickListener {
                ImagePicker.with(this).cropSquare().compress(512)
                    .maxResultSize(512, 512)
                    .createIntent {
                        imagePickerLauncher.launch(it)
                    }
            }
        }

        binding.cancelBtn.setOnClickListener {
            binding.editChatroomButtonsLayout.visibility = View.GONE
            binding.chatroomMenuLayout.visibility = View.VISIBLE
            binding.chatroomNameEdtTxt.visibility = View.GONE
            binding.chatroomNameTV.visibility = View.VISIBLE
            if (chatroomType == "group_chat"){
                binding.editChatroomCIV.visibility = View.GONE
                binding.chatroomCIV.visibility = View.VISIBLE
            }
        }

        binding.saveBtn.setOnClickListener {
            val name = binding.chatroomNameEdtTxt.text.toString()
            if (name.length < 3){
                Toast.makeText(this, "Chatroom name should at least have 3 or more characters", Toast.LENGTH_SHORT).show()
            } else if (AppUtil().containsBadWord(name)){
                Toast.makeText(this, "The name contains sensitive word/s", Toast.LENGTH_SHORT).show()
            } else {
                //updates name
                if (name != binding.chatroomNameTV.text.toString()){
                    FirebaseUtil().retrieveChatRoomReference(chatroomId).update("chatroomName", name).addOnSuccessListener {
                        chatroomName = name
                    }
                }
                //updates profile
                if (::selectedImage.isInitialized){
                    var imageUrl = chatroomModel.chatroomProfileUrl
                    if (imageUrl.isNotEmpty() && imageUrl != "null"){
                        //deletes current profile
                        FirebaseUtil().retrieveGroupChatProfileRef(imageUrl).delete()
                    }
                    //uploads profile picture
                    imageUrl = "${chatroomModel.chatroomId}-${Timestamp.now()}"
                    FirebaseUtil().retrieveGroupChatProfileRef(imageUrl).putFile(selectedImage).addOnSuccessListener {
                        FirebaseUtil().retrieveChatRoomReference(chatroomId).update("chatroomProfileUrl", imageUrl)
                    }
                }
                Handler().postDelayed({
                    onBackPressed()
                }, 2000)
            }

        }
    }

}