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

class ContentUsersAdapter(private val context: Context, options: FirestoreRecyclerOptions<UserModel>):
    FirestoreRecyclerAdapter<UserModel, ContentUsersAdapter.UserViewHolder>(options) {

    private var totalItems = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: UserModel) {
        totalItems += 1
        holder.bind(model)
    }

    inner class UserViewHolder(private val binding: ItemProfileBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        fun bind(model: UserModel){

            AppUtil().setUserProfilePic(context,model.userID, binding.userCircleImageView)
            if (model.username.isNotEmpty())
                binding.usernameTV.text = "@${model.username}"

            if (model.userID == FirebaseUtil().currentUserUid()){
                binding.userFullNameTV.text = "${model.fullName} (You)"
            }else {
                binding.userFullNameTV.text = model.fullName

                binding.usernameTV.setOnClickListener{
                    headToProfile(model.userID)
                }
                binding.userContainerRL.setOnClickListener{
                    headToProfile(model.userID)
                }
            }
        }
        private fun headToProfile(userID:String){
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userID", userID)
            context.startActivity(intent)
        }
    }

}