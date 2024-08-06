package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.Chatroom
import com.example.synthronize.databinding.ItemChatroomBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
//CHATROOMS
class ChatroomAdapter(private val context: Context, options: FirestoreRecyclerOptions<ChatroomModel>,
                      private val postId:String = "", private val communityIdOfPost:String = ""):
    FirestoreRecyclerAdapter<ChatroomModel, ChatroomAdapter.ChatroomViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatroomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemChatroomBinding.inflate(inflater, parent, false)
        return ChatroomViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: ChatroomViewHolder, position: Int, model: ChatroomModel) {
        holder.bind(model)
    }


    inner class ChatroomViewHolder(private val binding: ItemChatroomBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        private lateinit var chatroomModel: ChatroomModel

        fun bind(model: ChatroomModel){
            chatroomModel = model

            if (chatroomModel.chatroomType == "direct_message"){
                //if the user is id
                if (chatroomModel.userIdList[0] != FirebaseUtil().currentUserUid()){
                    bindDirectMessage(chatroomModel.userIdList[0])
                } else {
                    bindDirectMessage(chatroomModel.userIdList[1])
                }
            } else if (chatroomModel.chatroomType == "group_chat"){
                bindGroupChat()
            } else {
                bindCommunityChat()
            }
        }

        private fun bindCommunityChat() {
            AppUtil().setCommunityProfilePic(context, chatroomModel.communityId, binding.userCircleImageView)
            binding.chatroomNameTV.text = chatroomModel.chatroomName
            binding.lastUserMessageTV.text = chatroomModel.lastMessage
            binding.lastTimestampTV.text = DateAndTimeUtil().getTimeAgo(chatroomModel.lastMsgTimestamp)

            FirebaseUtil().targetUserDetails(chatroomModel.lastMessageUserId).get().addOnSuccessListener {user ->
                val userModel = user.toObject(UserModel::class.java)!!
                if (chatroomModel.lastMessageUserId != FirebaseUtil().currentUserUid())
                //if the message is not from the current user
                    binding.lastUserMessageTV.text = AppUtil().sliceMessage("${userModel.fullName}: ${chatroomModel.lastMessage}", 30)
                else
                //if the message is from the current user
                    binding.lastUserMessageTV.text = AppUtil().sliceMessage(chatroomModel.lastMessage, 30)

                binding.chatroomLayout.setOnClickListener {
                    val intent = Intent(context, Chatroom::class.java)
                    intent.putExtra("chatroomName", chatroomModel.chatroomName)
                    intent.putExtra("chatroomId", chatroomModel.chatroomId)
                    intent.putExtra("chatroomType", chatroomModel.chatroomType)
                    intent.putExtra("communityId", chatroomModel.communityId)
                    intent.putExtra("postId", postId)
                    intent.putExtra("communityIdOfPost", communityIdOfPost)
                    context.startActivity(intent)
                }
            }
        }

        private fun bindGroupChat() {
             FirebaseUtil().targetUserDetails(chatroomModel.lastMessageUserId).get().addOnSuccessListener {
                val userModel = it.toObject(UserModel::class.java)!!

                 binding.chatroomNameTV.text = chatroomModel.chatroomName
                 binding.lastTimestampTV.text = DateAndTimeUtil().getTimeAgo(chatroomModel.lastMsgTimestamp)
                 if (chatroomModel.chatroomProfileUrl.isNotEmpty()){
                     AppUtil().setGroupChatProfilePic(context, chatroomModel.chatroomProfileUrl, binding.userCircleImageView)
                 }

                if (chatroomModel.lastMessageUserId != FirebaseUtil().currentUserUid())
                //if the message is not from the current user
                    binding.lastUserMessageTV.text = AppUtil().sliceMessage("${userModel.fullName}: ${chatroomModel.lastMessage}", 30)
                else
                //if the message is from the current user
                    binding.lastUserMessageTV.text = AppUtil().sliceMessage(chatroomModel.lastMessage, 30)

                 binding.chatroomLayout.setOnClickListener {
                     val intent = Intent(context, Chatroom::class.java)
                     intent.putExtra("chatroomId", chatroomModel.chatroomId)
                     intent.putExtra("chatroomType", chatroomModel.chatroomType)
                     intent.putExtra("postId", postId)
                     intent.putExtra("communityIdOfPost", communityIdOfPost)
                     context.startActivity(intent)
                 }
            }
        }
        private fun bindDirectMessage(uid:String){
            FirebaseUtil().targetUserDetails(uid).get().addOnCompleteListener {
                if (it.isSuccessful && it.result.exists()){

                    val userModel = it.result.toObject(UserModel::class.java)!!
                    binding.chatroomNameTV.text = userModel.fullName
                    binding.lastTimestampTV.text = DateAndTimeUtil().getTimeAgo(chatroomModel.lastMsgTimestamp)
                    //other fields
                    AppUtil().setUserProfilePic(context, userModel.userID, binding.userCircleImageView)
                    if (chatroomModel.lastMessageUserId != FirebaseUtil().currentUserUid())
                    //if the message is not from the current user
                        binding.lastUserMessageTV.text = AppUtil().sliceMessage("${userModel.fullName}: ${chatroomModel.lastMessage}", 30)
                    else
                    //if the message is from the current user
                        binding.lastUserMessageTV.text = AppUtil().sliceMessage(chatroomModel.lastMessage, 30)

                    binding.chatroomLayout.setOnClickListener {
                        val intent = Intent(context, Chatroom::class.java)
                        intent.putExtra("chatroomName", userModel.fullName)
                        intent.putExtra("userID", uid)
                        intent.putExtra("chatroomType", chatroomModel.chatroomType)
                        intent.putExtra("postId", postId)
                        intent.putExtra("communityIdOfPost", communityIdOfPost)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

}