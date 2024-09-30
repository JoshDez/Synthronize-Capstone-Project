package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemSuggestionBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil

//Friend suggestion used for in Explore fragment

class FriendSuggestionAdapter(private val context: Context, private val uidList:ArrayList<String>)
    :RecyclerView.Adapter<FriendSuggestionAdapter.FriendSuggestionViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendSuggestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSuggestionBinding.inflate(inflater, parent, false)
        return FriendSuggestionViewHolder(binding, context)
    }

    override fun getItemCount(): Int {
        return uidList.size
    }

    override fun onBindViewHolder(holder: FriendSuggestionViewHolder, position: Int) {
        FirebaseUtil().targetUserDetails(uidList[position]).get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            holder.bind(userModel)
        }
    }

    class FriendSuggestionViewHolder(private val binding: ItemSuggestionBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){
        private lateinit var userModel: UserModel

        fun bind(model: UserModel){
            userModel = model
            AppUtil().setUserProfilePic(context, model.userID, binding.suggestionCIV)
            binding.suggestionNameTV.text = AppUtil().sliceMessage(model.fullName, 30)

            FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
                val myUserModel = it.toObject(UserModel::class.java)!!

                if (!myUserModel.friendsList.contains(userModel.userID) && !myUserModel.blockList.contains(userModel.userID)){
                    binding.actionBtn.visibility = View.VISIBLE
                    AppUtil().changeFriendsButtonState(context, binding.actionBtn, userModel)
                }
            }

            binding.suggestionCIV.setOnClickListener {
                AppUtil().headToUserProfile(context, model.userID)
            }

            binding.suggestionMainLayout.setOnClickListener {
                AppUtil().headToUserProfile(context, model.userID)
            }

            binding.suggestionNameTV.setOnClickListener {
                AppUtil().headToUserProfile(context, model.userID)
            }
        }
    }
}