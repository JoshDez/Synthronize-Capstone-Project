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
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil

class SearchUserAdapter(private val context: Context, options: FirestoreRecyclerOptions<UserModel>):
    FirestoreRecyclerAdapter<UserModel, SearchUserAdapter.UserViewHolder>(options) {
    var totalItems = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: UserModel) {
        totalItems += 1
        holder.bind(model)
    }

    class UserViewHolder(private val binding: ItemProfileBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        fun bind(model: UserModel){

            if (model.userID == FirebaseUtil().currentUserUid()){
                binding.userFullNameTV.text = "${model.fullName} (You)"
            }else {
                binding.userFullNameTV.text = model.fullName
                binding.userContainerRL.setOnClickListener{
                    val intent = Intent(context, OtherUserProfile::class.java)
                    intent.putExtra("userID", model.userID)
                    context.startActivity(intent)
                }
            }




        }




    }

}