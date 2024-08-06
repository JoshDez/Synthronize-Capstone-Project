package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemFileBinding
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class FilesAdapter(private val context: Context, options: FirestoreRecyclerOptions<FileModel>):
    FirestoreRecyclerAdapter<FileModel, FilesAdapter.FileViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFileBinding.inflate(inflater, parent, false)
        return FileViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int, model: FileModel) {
        holder.bind(model)
    }


    class FileViewHolder(private val binding: ItemFileBinding, private val context: Context,
                                private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var fileModel: FileModel

        fun bind(model: FileModel){
            fileModel = model

            FirebaseUtil().targetUserDetails(fileModel.fileOwnerId).get().addOnSuccessListener {
                val user = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, fileModel.fileOwnerId, binding.profileCIV)
                binding.usernameTV.text = user.username
            }
            binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(fileModel.uploadTimestamp)
            binding.captionTV.text = fileModel.caption
            binding.fileNameTV.text = fileModel.fileName
        }
    }

}