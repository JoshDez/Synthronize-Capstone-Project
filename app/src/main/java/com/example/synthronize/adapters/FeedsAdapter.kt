package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.CommunityFragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentFeedsBinding
import com.example.synthronize.databinding.ItemPostBinding
import com.example.synthronize.model.FeedsModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class FeedsAdapter(private val mainBinding: FragmentCommunityBinding, private val context: Context, 
                   options: FirestoreRecyclerOptions<FeedsModel>): FirestoreRecyclerAdapter<FeedsModel, FeedsAdapter.FeedsViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val feedsBinding = ItemPostBinding.inflate(inflater, parent, false)
        return FeedsViewHolder(mainBinding, feedsBinding, context)
    }

    override fun onBindViewHolder(holder: FeedsViewHolder, position: Int, model: FeedsModel) {
        holder.bind(model)
    }

    class FeedsViewHolder(private val mainBinding: FragmentCommunityBinding, private val feedsBinding: ItemPostBinding, private val context: Context)
        : RecyclerView.ViewHolder(feedsBinding.root){

        fun bind(model: FeedsModel){
            feedsBinding.descriptionTextView.text = model.feedCaption
            feedsBinding.timestampTextView.text = model.feedTimestamp.toString()
            FirebaseUtil().targetUserDetails(model.ownerId).get().addOnSuccessListener {
                val userModel = it.toObject(UserModel::class.java)!!
                //bind profile photo
                AppUtil().setUserProfilePic(context, userModel.userID, feedsBinding.profileImage)
                //bind post
                AppUtil().setUserCoverPic(context, userModel.userID, feedsBinding.postImage)
                feedsBinding.usernameTextView.text = userModel.fullName
            }
        }

    }

}