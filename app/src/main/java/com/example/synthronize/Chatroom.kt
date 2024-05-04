package com.example.synthronize

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
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



        //binding buttons
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.sendMsgBtn.setOnClickListener {
            val message = binding.chatBoxEdtTxt.text.toString()
            if (message.isNotEmpty()){
                sendMessage(message)
                binding.chatBoxEdtTxt.setText("")
            }
        }

        getChatroomID()
        bindChatroomDetails()
        createOrRetrieveChatroomModel()
        setupChatRV()
    }

    private fun getChatroomID() {
        if (chatroomType == "direct_message"){
            //Chatroom ID for DM
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
                if (!it.result.exists() && chatroomType == "direct_message"){
                    //First chat in DM
                    chatroomModel = ChatroomModel(chatroomID,
                        "direct_message",
                        listOf(FirebaseUtil().currentUserUid(), receiverUid),
                        Timestamp.now(),
                        ""
                    )
                    FirebaseUtil().retrieveChatRoomReference(chatroomID).set(chatroomModel)
                } else if (!it.result.exists() && chatroomType == "group_chat"){
                    //First chat in Group
                    //TODO to be implemented

                } else if (!it.result.exists() && chatroomType == "community_chat"){
                    FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener { result ->
                        val communityModel = result.toObject(CommunityModel::class.java)!!

                        chatroomModel = ChatroomModel(
                            chatroomName = chatroomName,
                            chatroomId = chatroomID,
                            chatroomType = "community_chat",
                            userIdList = communityModel.communityMembers,
                            lastMsgTimestamp = Timestamp.now(),
                            lastMessage = "",
                            lastMessageUserId = FirebaseUtil().currentUserUid()
                        )
                        FirebaseUtil().retrieveChatRoomReference(chatroomID).set(chatroomModel)
                    }
                } else {
                    chatroomModel = it.result.toObject(ChatroomModel::class.java)!!
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
        val messageModel = MessageModel(message, FirebaseUtil().currentUserUid(), Timestamp.now())
        chatroomModel.lastMessageUserId = FirebaseUtil().currentUserUid()
        chatroomModel.lastMessage = message
        chatroomModel.lastMsgTimestamp = Timestamp.now()

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