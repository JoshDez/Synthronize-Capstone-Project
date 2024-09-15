package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.databinding.ItemCommentBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.CollectionReference
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CommentAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommentModel>, private var commentPath: CollectionReference):
    FirestoreRecyclerAdapter<CommentModel, CommentAdapter.CommentViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommentBinding.inflate(inflater, parent, false)
        return CommentViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int, model: CommentModel) {
        holder.bind(model)
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding, private val context: Context, private val inflater: LayoutInflater): RecyclerView.ViewHolder(binding.root){
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

            binding.commentTV.setOnLongClickListener {
                if (commentModel.commentOwnerId == FirebaseUtil().currentUserUid()){
                    showWarningDialog()
                }
                true
            }

        }

        private fun showWarningDialog() {
            val warningDialogBinding = DialogWarningMessageBinding.inflate(inflater)
            val warningDialog = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(warningDialogBinding.root))
                .setBackgroundColorResId(R.color.transparent)
                .setGravity(Gravity.CENTER)
                .setCancelable(true)
                .create()

            warningDialogBinding.titleTV.text = "Delete Comment?"
            warningDialogBinding.messageTV.text = "Do you want to permanently delete your comment?"

            warningDialogBinding.yesBtn.setOnClickListener {
                if (commentModel.commentId.isNotEmpty())
                    commentPath.document(commentModel.commentId).delete()

                warningDialog.dismiss()
            }
            warningDialogBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
            }

            warningDialog.show()
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