package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.ItemFeedBinding
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
        val feedsBinding = ItemFeedBinding.inflate(inflater, parent, false)
        return FeedsViewHolder(mainBinding, feedsBinding, context)
    }

    override fun onBindViewHolder(holder: FeedsViewHolder, position: Int, model: FeedsModel) {
        holder.bind(model)
    }

    class FeedsViewHolder(private val mainBinding: FragmentCommunityBinding, private val feedsBinding: ItemFeedBinding,
                          private val context: Context)
        : RecyclerView.ViewHolder(feedsBinding.root){

        fun bind(feedModel: FeedsModel){

            //SETUP WRAPPER FOR REPOST OR COMMUNITY
            if (feedModel.repostId.isNotEmpty()){
                feedsBinding.feedWrapperLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(feedModel.repostOwnerId).get().addOnSuccessListener {
                    val user = it.toObject(UserModel::class.java)!!
                    feedsBinding.wrapperName.text = user.username
                    AppUtil().setUserProfilePic(context, user.userID, feedsBinding.profileCIV)
                    feedsBinding.wrapperName.setOnClickListener {
                        headToUserProfile()
                    }
                }
            }

            //SETUP FEED
            FirebaseUtil().targetUserDetails(feedModel.ownerId).get().addOnSuccessListener {
                val owner = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, owner.userID, feedsBinding.profileCIV)
                feedsBinding.usernameTV.text = owner.username
                feedsBinding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }
                feedsBinding.descriptionTV.text = feedModel.feedCaption
                feedsBinding.timestampTV.text = feedModel.feedTimestamp.toString()
                bindContent(feedModel.contentList)
            }
        }

        private fun bindContent(contentList:List<String>){
            //TODO: Add full functionality and replace the temporary code
            val tempArray = contentList[0].split('-')
            if (tempArray[1] == "Image"){
                //TODO: to be implemented
                AppUtil().setImageContent(context, contentList[0], feedsBinding.feedImageIV)
            } else if (tempArray[2] == "Video"){
                //TODO: to be implemented
            }
        }

        private fun headToUserProfile(){
            //TODO: to be implemented
        }


    }

}