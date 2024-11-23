package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil

class SelectedContestantsAdapter(private val context: Context,
                                 private val userModels:List<UserModel>,
                                 private val listener: OnItemClickListener,
                                 private val selectedUserList:ArrayList<String> = ArrayList(),
                                 private val resultType:String = ""):
    RecyclerView.Adapter<SelectedContestantsAdapter.UserViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding, context)
    }

    override fun getItemCount(): Int {
        return userModels.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userModels[position], position)
    }

    inner class UserViewHolder(private val binding: ItemProfileBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){
        fun bind(model: UserModel, position: Int) {
            AppUtil().setUserProfilePic(context, model.userID, binding.userCircleImageView)

            if (model.username.isNotEmpty())
                binding.usernameTV.text = "@${model.username}"

            if (model.userID == FirebaseUtil().currentUserUid())
                binding.userFullNameTV.text = "${model.fullName} (You)"
            else
                binding.userFullNameTV.text = model.fullName


            if (resultType.isEmpty()){
                //for select user dialog
                binding.selectUserCB.visibility = View.VISIBLE
                binding.userPositionTV.visibility = View.VISIBLE
                binding.userPositionTV.text = "${position + 1}"

                for (userId in selectedUserList) {
                    if (model.userID == userId) {
                        binding.selectUserCB.isChecked = true
                    }
                }
                binding.selectUserCB.setOnClickListener {
                    if (binding.selectUserCB.isChecked) {
                        listener.onItemClick(model.userID, true)
                    } else {
                        listener.onItemClick(model.userID, false)
                    }
                }

            } else {
                //for results
                binding.usernameTV.setOnClickListener{
                    listener.onItemClick(model.userID)
                }
                binding.userContainerRL.setOnClickListener{
                    listener.onItemClick(model.userID)
                }
                //displays position of the list if its rank
                if (resultType != "All"){
                    binding.userPositionTV.visibility = View.VISIBLE
                    binding.userPositionTV.text = "${position + 1}"
                }
            }
        }
    }

}