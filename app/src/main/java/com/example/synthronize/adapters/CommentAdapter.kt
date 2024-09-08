package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.databinding.ItemCommentBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class CommentAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommentModel>):
    FirestoreRecyclerAdapter<CommentModel, CommentAdapter.CommentViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommentBinding.inflate(inflater, parent, false)
        return CommentViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int, model: CommentModel) {

        holder.bind(model)
    }

    class CommentViewHolder(private val binding: ItemCommentBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){
        private lateinit var commentModel:CommentModel

        fun bind(model: CommentModel){
            commentModel = model
            binding.commentTV.text = commentModel.comment
            binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(commentModel.commentTimestamp)
            FirebaseUtil().targetUserDetails(commentModel.commentOwnerId).get().addOnSuccessListener {
                val user = it.toObject(UserModel::class.java)!!
                binding.userNameTV.text = user.username
                AppUtil().setUserProfilePic(context, user.userID, binding.userProfileCIV)
            }
            binding.userNameTV.setOnClickListener {
                headToUserProfile()
            }
            binding.userProfileCIV.setOnClickListener {
                headToUserProfile()
            }
        }


        private fun headToUserProfile() {
            if (commentModel.commentOwnerId != FirebaseUtil().currentUserUid()){
                val intent = Intent(context, OtherUserProfile::class.java)
                intent.putExtra("userID", commentModel.commentOwnerId)
                context.startActivity(intent)
            }
        }
    }

}