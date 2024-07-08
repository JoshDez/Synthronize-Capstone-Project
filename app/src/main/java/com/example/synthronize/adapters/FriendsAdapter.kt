package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.Chatroom
import com.example.synthronize.OtherUserProfile
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.google.firebase.Timestamp
import java.lang.Exception

//FRIENDS LIST
class FriendsAdapter(private val context: Context, options: FirestoreRecyclerOptions<UserModel>):
    FirestoreRecyclerAdapter<UserModel, FriendsAdapter.FriendsViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return FriendsViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int, model: UserModel) {
        holder.bind(model)
    }


    class FriendsViewHolder(private val binding: ItemProfileBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        private lateinit var userModel: UserModel
        private lateinit var lastSeen: Timestamp

        fun bind(model: UserModel){
            userModel = model

            binding.userFullNameTV.text = model.fullName
            AppUtil().setUserProfilePic(context, model.userID, binding.userCircleImageView)

            binding.userFullNameTV.setOnClickListener {
                headToProfile()
            }
            binding.userCircleImageView.setOnClickListener {
                headToProfile()
            }
            binding.userMainLayout.setOnClickListener {
                val intent = Intent(context, Chatroom::class.java)
                intent.putExtra("chatroomName", userModel.fullName)
                intent.putExtra("userID", userModel.userID)
                intent.putExtra("chatroomType", "direct_message")
                context.startActivity(intent)
            }


            //TODO to remove try catch
            try {
                lastSeen = userModel.currentStatus["lastSeen"] as Timestamp

                if(!DateAndTimeUtil().isCurrentTimestampOlderThanMinutes(lastSeen, 5)){
                    binding.usernameTV.text = "Active Now"
                } else {
                    binding.usernameTV.text = "Offline"
                }
            } catch (e:Exception){
                Log.d("Friends Adapter", e.toString())
            }


            //TODO to modify menu

        }



        private fun headToProfile(){
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userID", userModel.userID)
            context.startActivity(intent)
        }

    }

}