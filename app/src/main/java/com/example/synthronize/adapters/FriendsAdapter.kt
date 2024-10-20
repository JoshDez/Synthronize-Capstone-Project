package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.Chatroom
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.databinding.DialogUserMenuBinding
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Timestamp
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.lang.Exception

//FRIENDS LIST
class FriendsAdapter(private val context: Context, options: FirestoreRecyclerOptions<UserModel>):
    FirestoreRecyclerAdapter<UserModel, FriendsAdapter.FriendsViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return FriendsViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int, model: UserModel) {
        holder.bind(model)
    }


    class FriendsViewHolder(private val binding: ItemProfileBinding, private val context: Context, private val layoutInflater: LayoutInflater): RecyclerView.ViewHolder(binding.root){

        private lateinit var userModel: UserModel
        private lateinit var lastSeen: Timestamp

        fun bind(model: UserModel){
            userModel = model

            binding.userFullNameTV.text = model.fullName
            AppUtil().setUserProfilePic(context, model.userID, binding.userCircleImageView)

            binding.userFullNameTV.setOnClickListener {
                openUserMenu()
            }
            binding.userCircleImageView.setOnClickListener {
                openUserMenu()
            }
            binding.userMainLayout.setOnClickListener {
                openUserMenu()
            }

            try {
                lastSeen = userModel.currentStatus["lastSeen"] as Timestamp

                if(!DateAndTimeUtil().isCurrentTimestampOlderThanMinutes(lastSeen, 5)){
                    val textColor = ContextCompat.getColor(context, R.color.green)
                    binding.usernameTV.setTextColor(textColor)
                    binding.usernameTV.text = "Online"
                } else {
                    val textColor = ContextCompat.getColor(context, R.color.less_saturated_light_teal)
                    binding.usernameTV.setTextColor(textColor)
                    binding.usernameTV.text = "Offline"
                }
            } catch (e:Exception){
                Log.d("Friends Adapter", e.toString())
            }
        }

        private fun openUserMenu(){
            val dialogPlusBinding = DialogUserMenuBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(dialogPlusBinding.root))
                .setInAnimation(androidx.appcompat.R.anim.abc_fade_in)
                .setBackgroundColorResId(R.color.transparent)
                .setCancelable(true)
                .setGravity(Gravity.CENTER)
                .create()

            AppUtil().setUserProfilePic(context, userModel.userID, dialogPlusBinding.userProfileCIV)
            AppUtil().setUserCoverPic(context, userModel.userID, dialogPlusBinding.userCoverIV)
            dialogPlusBinding.userDisplayNameTV.text = userModel.fullName
            if (userModel.username.isNotEmpty()){
                dialogPlusBinding.userNameTV.visibility = View.VISIBLE
                dialogPlusBinding.userNameTV.text = "@${userModel.username}"
            }

            dialogPlusBinding.upperButtons.visibility = View.GONE

            //view profile button
            dialogPlusBinding.viewProfileBtn.setOnClickListener {
                val intent = Intent(context, OtherUserProfile::class.java)
                intent.putExtra("userID", userModel.userID)
                context.startActivity(intent)
            }
            //message user button
            dialogPlusBinding.reportProfileBtn.text = "Message User"
            dialogPlusBinding.reportProfileBtn.setOnClickListener {
                val intent = Intent(context, Chatroom::class.java)
                intent.putExtra("chatroomName", userModel.fullName)
                intent.putExtra("userID", userModel.userID)
                intent.putExtra("chatroomType", "direct_message")
                context.startActivity(intent)
                dialogPlus.dismiss()
            }
            dialogPlus.show()
        }
    }

}