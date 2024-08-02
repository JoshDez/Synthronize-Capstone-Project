package com.example.synthronize

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.example.synthronize.adapters.MessageAdapter
import com.example.synthronize.databinding.ActivityChatroomBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.MessageModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import java.lang.Exception

class Chatroom : AppCompatActivity() {
    private lateinit var binding:ActivityChatroomBinding
    private lateinit var chatroomModel: ChatroomModel
    private lateinit var recyclerView:RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var chatroomName = ""
    private var receiverUid = ""
    private var chatroomType = ""
    private var communityId = ""
    private var chatroomID = ""
    private var postId = ""
    private var communityIdOfPost = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //chatroom details
        chatroomName = intent.getStringExtra("chatroomName").toString()
        chatroomType = intent.getStringExtra("chatroomType").toString()

        //for direct message
        receiverUid = intent.getStringExtra("userID").toString()

        //for community chats
        communityId = intent.getStringExtra("communityId").toString()
        chatroomID = intent.getStringExtra("chatroomId").toString()

        //for sending post
        postId = intent.getStringExtra("postId").toString()
        communityIdOfPost = intent.getStringExtra("communityIdOfPost").toString()


        //binding buttons
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.sendMsgBtn.setOnClickListener {
            val message = binding.chatBoxEdtTxt.text.toString()
            if (message.isNotEmpty()){
                sendMessage(message)
                binding.chatBoxEdtTxt.setText("")
                binding.postLayout.visibility = View.GONE
                postId = ""
                communityIdOfPost = ""
            } else if (postId.isNotEmpty() || postId != "null"){
                sendMessage("")
                binding.postLayout.visibility = View.GONE
                postId = ""
                communityIdOfPost = ""
            }
        }

        getChatroomID()
        bindChatroomDetails()
        createOrRetrieveChatroomModel()
        setupChatRV()
        bindPostToBeSent()

    }

    private fun bindPostToBeSent() {
        if (postId != "null" && postId.isNotEmpty()){
            try {
                FirebaseUtil().retrieveCommunityFeedsCollection(communityIdOfPost).document(postId).get().addOnSuccessListener {
                    val postModel = it.toObject(PostModel::class.java)!!
                    FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener { user ->
                        val userModel = user.toObject(UserModel::class.java)!!
                        binding.postLayout.visibility = View.VISIBLE
                        //bind post preview
                        AppUtil().setUserProfilePic(this, userModel.userID, binding.postOwnerProfileCIV)
                        binding.postOwnerUsernameTV.text = userModel.username
                        binding.postCaptionTV.text = postModel.caption

                        binding.cancelPostBtn.setOnClickListener {
                            postId = ""
                            communityIdOfPost = ""
                            binding.postLayout.visibility = View.GONE
                        }
                    }
                }
            } catch (e:Exception){

            }
        }
    }

    private fun checkIfItsBlockedByUser(){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener { sender ->
            FirebaseUtil().targetUserDetails(receiverUid).get().addOnSuccessListener {receiver ->
                val senderModel = sender.toObject(UserModel::class.java)!!
                val receiverModel = receiver.toObject(UserModel::class.java)!!
                if (AppUtil().isIdOnList(senderModel.blockList, receiverModel.userID)
                    || AppUtil().isIdOnList(receiverModel.blockList, senderModel.userID)){
                    //removes chatbox
                    binding.chatBoxEdtTxt.visibility = View.GONE
                    binding.sendMsgBtn.visibility = View.GONE
                    binding.chatroomNotAvailableTV.visibility = View.VISIBLE

                }
            }
        }
    }

    private fun getChatroomID() {
        if (chatroomType == "direct_message"){
            //Chatroom ID for DM
            checkIfItsBlockedByUser()
            if (FirebaseUtil().currentUserUid().hashCode() < receiverUid.hashCode()){
               chatroomID = "${FirebaseUtil().currentUserUid()}-$receiverUid"
            } else {
               chatroomID = "$receiverUid-${FirebaseUtil().currentUserUid()}"
            }
        } else if (chatroomType == "group_chat"){
            //TODO: To be implemented
        }
    }

    private fun bindChatroomDetails() {
        when(chatroomType){
            "direct_message" -> {
                binding.chatRoomNameTV.text = chatroomName
                AppUtil().setUserProfilePic(this, receiverUid, binding.chatroomCircleIV)
            }
            "community_chat" -> {
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                    val community = it.toObject(CommunityModel::class.java)!!
                    binding.chatRoomNameTV.text = "$chatroomName | ${community.communityName}"
                }.addOnFailureListener {
                    binding.chatRoomNameTV.text = chatroomName
                }
                AppUtil().setCommunityProfilePic(this, communityId, binding.chatroomCircleIV)
            }
        }
    }

    private fun createOrRetrieveChatroomModel(){
        FirebaseUtil().retrieveChatRoomReference(chatroomID).get().addOnCompleteListener {
            if (it.isSuccessful){
                if (it.result?.exists() == false && chatroomType == "direct_message"){
                    //First chat in DM
                    chatroomModel = ChatroomModel(chatroomID,
                        "direct_message",
                        listOf(FirebaseUtil().currentUserUid(), receiverUid),
                        Timestamp.now(),
                        ""
                    )
                    FirebaseUtil().retrieveChatRoomReference(chatroomID).set(chatroomModel)
                } else if (it.result?.exists() == false && chatroomType == "group_chat"){
                    //First chat in Group
                    //TODO to be implemented

                } else if (it.result?.exists() == false && chatroomType == "community_chat"){
                    FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener { result ->
                        val communityModel = result.toObject(CommunityModel::class.java)!!

                        chatroomModel = ChatroomModel(
                            chatroomName = chatroomName,
                            chatroomId = chatroomID,
                            chatroomType = "community_chat",
                            userIdList = communityModel.communityMembers.keys.toList(),
                            lastMsgTimestamp = Timestamp.now(),
                            lastMessage = "",
                            lastMessageUserId = FirebaseUtil().currentUserUid()
                        )
                        FirebaseUtil().retrieveChatRoomReference(chatroomID).set(chatroomModel)
                    }
                } else {
                    chatroomModel = it.result!!.toObject(ChatroomModel::class.java)!!
                }
            }
        }
    }

    private fun setupChatRV() {
        if (chatroomID.isNotEmpty()){
            val myQuery: Query = FirebaseUtil().retrieveChatsFromChatroom(chatroomID)
                .orderBy("timestamp", Query.Direction.ASCENDING)

            val options: FirestoreRecyclerOptions<MessageModel> =
                FirestoreRecyclerOptions.Builder<MessageModel>().setQuery(myQuery, MessageModel::class.java).build()

            recyclerView = binding.chatRV
            linearLayoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = linearLayoutManager
            messageAdapter = MessageAdapter(this, options)
            recyclerView.adapter = messageAdapter
            messageAdapter.startListening()

            Handler().postDelayed({
                recyclerView.smoothScrollToPosition(messageAdapter.getMessageCount())
            }, 1000)
        }
    }

    private fun sendMessage(message:String) {

        val messageModel = MessageModel(
            message = message, postID = postId, communityIdOfPost = communityIdOfPost,
            senderID =  FirebaseUtil().currentUserUid(), timestamp =  Timestamp.now()
        )

        chatroomModel.lastMessageUserId = FirebaseUtil().currentUserUid()
        chatroomModel.lastMsgTimestamp = Timestamp.now()

        if (message.isEmpty() && postId.isNotEmpty() && postId != "null"){
            //"sent a post" as the last message
            chatroomModel.lastMessage = "Sent a post"
        } else {
            //send the actual message
            chatroomModel.lastMessage = message
        }

        //update lastMessageUserID and lastMsgTimestamp
        FirebaseUtil().retrieveChatRoomReference(chatroomID).set(chatroomModel)

        //add message to chatroom
        FirebaseUtil().retrieveChatsFromChatroom(chatroomID).add(messageModel)

        Handler().postDelayed({
            recyclerView.smoothScrollToPosition(messageAdapter.getMessageCount())
        }, 1000)

    }

    override fun onStart() {
        super.onStart()
        if (::messageAdapter.isInitialized)
            messageAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (::messageAdapter.isInitialized)
            messageAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::messageAdapter.isInitialized)
            messageAdapter.stopListening()
    }
}