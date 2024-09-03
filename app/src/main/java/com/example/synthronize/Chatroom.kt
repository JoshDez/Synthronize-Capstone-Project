package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
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
    private var chatroomId = ""
    private var postId = ""
    private var productId = ""
    private var communityIdOfPost = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //chatroom details
        chatroomId = intent.getStringExtra("chatroomId").toString()
        chatroomType = intent.getStringExtra("chatroomType").toString()

        //for direct message
        chatroomName = intent.getStringExtra("chatroomName").toString()
        receiverUid = intent.getStringExtra("userID").toString()

        //for community chats
        communityId = intent.getStringExtra("communityId").toString()


        //for sending post or product
        postId = intent.getStringExtra("postId").toString()
        productId = intent.getStringExtra("productId").toString()
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
            } else if (productId.isNotEmpty() || productId != "null"){
                sendMessage("")
                binding.postLayout.visibility = View.GONE
                productId = ""
                communityIdOfPost = ""
            }
        }

        getChatroomIdForDM()
        createOrRetrieveChatroomModel()
        bindPostOrProductToBeSent()
    }

    private fun getChatroomIdForDM(){
        if(chatroomType == "direct_message"){
            if (FirebaseUtil().currentUserUid().hashCode() < receiverUid.hashCode()){
                chatroomId = "${FirebaseUtil().currentUserUid()}-$receiverUid"
            } else {
                chatroomId = "$receiverUid-${FirebaseUtil().currentUserUid()}"
            }
        }
    }

    private fun bindPostOrProductToBeSent() {
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
                Log.d("error", e.message.toString())
            }
        } else if (productId != "null" && productId.isNotEmpty()){
            try {
                FirebaseUtil().retrieveCommunityMarketCollection(communityIdOfPost).document(productId).get().addOnSuccessListener {
                    val productModel = it.toObject(ProductModel::class.java)!!
                    FirebaseUtil().targetUserDetails(productModel.ownerId).get().addOnSuccessListener { user ->
                        val userModel = user.toObject(UserModel::class.java)!!
                        binding.postLayout.visibility = View.VISIBLE
                        //bind post preview
                        AppUtil().setUserProfilePic(this, userModel.userID, binding.postOwnerProfileCIV)
                        binding.postOwnerUsernameTV.text = userModel.username
                        binding.postCaptionTV.text = productModel.productName

                        binding.cancelPostBtn.setOnClickListener {
                            productId = ""
                            communityIdOfPost = ""
                            binding.postLayout.visibility = View.GONE
                        }
                    }
                }
            } catch (e:Exception){
                Log.d("error", e.message.toString())
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

    private fun createOrRetrieveChatroomModel(){
        FirebaseUtil().retrieveChatRoomReference(chatroomId).get().addOnCompleteListener {
            if (it.isSuccessful){
                if (!it.result.exists() && chatroomType == "direct_message"){
                    //get chatroom Id for DM
                    checkIfItsBlockedByUser()
                    //First chat in DM
                    chatroomModel = ChatroomModel(chatroomId,
                        "direct_message",
                        listOf(FirebaseUtil().currentUserUid(), receiverUid),
                        Timestamp.now(),
                        ""
                    )
                    FirebaseUtil().retrieveChatRoomReference(chatroomId).set(chatroomModel)
                    bindChatroomDetails(chatroomModel.chatroomType)

                } else if (!it.result.exists() && chatroomType == "community_chat"){
                    FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener { result ->
                        val communityModel = result.toObject(CommunityModel::class.java)!!
                        chatroomModel = ChatroomModel(
                            chatroomName = chatroomName,
                            chatroomId = chatroomId,
                            chatroomType = "community_chat",
                            userIdList = communityModel.communityMembers.keys.toList(),
                            lastMsgTimestamp = Timestamp.now(),
                            lastMessage = "",
                            lastMessageUserId = FirebaseUtil().currentUserUid()
                        )
                        FirebaseUtil().retrieveChatRoomReference(chatroomId).set(chatroomModel)
                        bindChatroomDetails(chatroomModel.chatroomType)
                    }
                } else {
                    chatroomModel = it.result!!.toObject(ChatroomModel::class.java)!!
                    bindChatroomDetails(chatroomModel.chatroomType)
                }
            }
        }
    }

    private fun bindChatroomDetails(chatroomType:String) {
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
            "group_chat" -> {
                if (chatroomModel.chatroomProfileUrl.isNotEmpty()){
                    AppUtil().setGroupChatProfilePic(this, chatroomModel.chatroomProfileUrl, binding.chatroomCircleIV)
                }
                binding.chatRoomNameTV.text = chatroomModel.chatroomName
            }
        }
        bindChatroomSettings()
        setupChatRV()
    }

    private fun bindChatroomSettings(){
        binding.kebabMenuBtn.setOnClickListener {
            val intent = Intent(this, ChatroomSettings::class.java)
            intent.putExtra("communityId", communityId)
            intent.putExtra("chatroomId", chatroomId)
            intent.putExtra("chatroomType", chatroomType)
            intent.putExtra("userID", receiverUid)
            intent.putExtra("chatroomName", chatroomName)
            startActivity(intent)
            this.finish()
        }
    }

    private fun setupChatRV() {
        if (chatroomId.isNotEmpty()){
            val myQuery: Query = FirebaseUtil().retrieveChatsFromChatroom(chatroomId)
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
            message = message, postID = postId, productID = productId, communityIdOfPost = communityIdOfPost,
            senderID =  FirebaseUtil().currentUserUid(), timestamp =  Timestamp.now()
        )

        chatroomModel.lastMessageUserId = FirebaseUtil().currentUserUid()
        chatroomModel.lastMsgTimestamp = Timestamp.now()

        if (message.isEmpty() && postId.isNotEmpty() && postId != "null"){
            //"sent a post" as the last message
            chatroomModel.lastMessage = "Sent a post"
        } else if (message.isEmpty() && productId.isNotEmpty() && productId != "null"){
            //"sent a post" as the last message
            chatroomModel.lastMessage = "Sent a product"
        } else {
            //send the actual message
            chatroomModel.lastMessage = message
        }

        //update lastMessageUserID and lastMsgTimestamp
        FirebaseUtil().retrieveChatRoomReference(chatroomId).set(chatroomModel)

        //add message to chatroom
        FirebaseUtil().retrieveChatsFromChatroom(chatroomId).add(messageModel)

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