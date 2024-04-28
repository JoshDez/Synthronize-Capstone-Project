package com.example.synthronize.adapters

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.ItemCommunityBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import kotlinx.coroutines.NonDisposableHandle.parent

class SearchCommunityAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>):
    FirestoreRecyclerAdapter<CommunityModel, SearchCommunityAdapter.CommunityViewHolder>(options) {
    private var totalItems = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommunityBinding.inflate(inflater, parent, false)
        return CommunityViewHolder(binding, context, inflater, parent)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        totalItems += 1
        holder.bind(model)
    }
    fun getTotalItems():Int{
        return totalItems
    }

    class CommunityViewHolder(private val binding: ItemCommunityBinding, private val context: Context,
                              private val inflater: LayoutInflater, private val parent:ViewGroup): RecyclerView.ViewHolder(binding.root){
        private lateinit var dialogBinding: DialogCommunityPreviewBinding
        private lateinit var communityModel: CommunityModel
        fun bind(model: CommunityModel){
            //Checks if user is not on community block list
            if (!AppUtil().isUserOnList(model.blockList, FirebaseUtil().currentUserUid())){
                communityModel = model
                binding.groupNameTextView.text = model.communityName
                //TODO: Bind community image
                AppUtil().setCommunityProfilePic(context, communityModel.communityId, binding.profileImage)
                binding.itemGroupLayout.setOnClickListener {
                    AppUtil().openCommunityPreviewDialog(context, inflater, communityModel)
                }
            } else {
                //hide the community if the user is on the community block list
                binding.itemGroupLayout.visibility == View.GONE
            }
        }
    }
}