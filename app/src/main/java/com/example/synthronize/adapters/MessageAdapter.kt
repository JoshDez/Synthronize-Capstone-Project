package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.Chatroom
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemMessageBinding
import com.example.synthronize.model.MessageModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import java.text.SimpleDateFormat

class MessageAdapter(private val context: Context, options: FirestoreRecyclerOptions<MessageModel>):
    FirestoreRecyclerAdapter<MessageModel, MessageAdapter.MessageViewHolder>(options) {

    private var itemCount:Int = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMessageBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: MessageModel) {
        holder.bind(model)
        itemCount += 1
    }

    fun getMessageCount(): Int{
        return itemCount
    }

    class MessageViewHolder(private val binding: ItemMessageBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){
        fun bind(model: MessageModel){
            //bind user's message
            if (model.postID.isNotEmpty()){

            } else {
                //Send message only
                if (model.senderID == FirebaseUtil().currentUserUid()){
                    //If user is the sender
                    binding.recieverLayout.visibility = View.GONE
                    binding.senderLayout.visibility = View.VISIBLE
                    binding.senderMsgTV.visibility = View.VISIBLE
                    binding.senderMsgTV.text = model.message
                } else {
                    //If user is the receiver
                    binding.senderLayout.visibility = View.GONE
                    binding.recieverLayout.visibility = View.VISIBLE
                    binding.recieverMsgTV.visibility = View.VISIBLE
                    binding.recieverMsgTV.text = model.message
                    //retrieve sender user data
                    FirebaseUtil().targetUserDetails(model.senderID).get().addOnCompleteListener {
                        if (it.isSuccessful && it.result.exists()){
                            val userModel = it.result.toObject(UserModel::class.java)!!
                            binding.userNameTV.text = userModel.fullName
                            AppUtil().setUserProfilePic(context, userModel.userID, binding.userProfileCIV)
                        }
                    }
                }
            }
        }
    }

}