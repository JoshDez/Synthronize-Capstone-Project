package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SelectUsersAdapter(private val context: Context, private val listener: OnItemClickListener, private val selectedUserList:ArrayList<String>, options: FirestoreRecyclerOptions<UserModel>):
    FirestoreRecyclerAdapter<UserModel, SelectUsersAdapter.UserViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: UserModel) {
        holder.bind(model)
    }
    inner class UserViewHolder(private val binding: ItemProfileBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        fun bind(model: UserModel){

            if (model.userID == FirebaseUtil().currentUserUid()){
                binding.userMainLayout.visibility = View.GONE
            }else {
                AppUtil().setUserProfilePic(context,model.userID, binding.userCircleImageView)
                binding.userFullNameTV.text = model.fullName
                binding.selectUserCB.visibility = View.VISIBLE

                for (userId in selectedUserList){
                    if (model.userID == userId){
                        binding.selectUserCB.isChecked = true
                    }
                }

                binding.selectUserCB.setOnClickListener {
                    if (binding.selectUserCB.isChecked){
                       listener.onItemClick(model.userID, true)
                    } else {
                        listener.onItemClick(model.userID, false)
                    }
                }


            }
        }
    }

}