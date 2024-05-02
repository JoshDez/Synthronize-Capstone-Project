package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ViewFlipper
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.ViewPost
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.ItemFeedBinding
import com.example.synthronize.model.FeedsModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue

class FeedsAdapter(private val mainBinding: FragmentCommunityBinding, private val context: Context, 
                   options: FirestoreRecyclerOptions<FeedsModel>): FirestoreRecyclerAdapter<FeedsModel, FeedsAdapter.FeedsViewHolder>(options){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val feedsBinding = ItemFeedBinding.inflate(inflater, parent, false)
        return FeedsViewHolder(mainBinding, feedsBinding, context)
    }

    override fun onBindViewHolder(holder: FeedsViewHolder, position: Int, model: FeedsModel) {
        holder.bind(model)
    }




    //VIEW HOLDER
    class FeedsViewHolder(private val mainBinding: FragmentCommunityBinding, private val feedsBinding: ItemFeedBinding,
                          private val context: Context) : RecyclerView.ViewHolder(feedsBinding.root){

        private lateinit var feedModel:FeedsModel
        private lateinit var viewPageAdapter: ViewPageAdapter
        private var isLoved:Boolean = false
        fun bind(model: FeedsModel){

            this.feedModel = model

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
                feedsBinding.descriptionTV.text = feedModel.feedCaption
                feedsBinding.timestampTV.text = DateUtil().formatTimestampToDate(feedModel.feedTimestamp)
                feedsBinding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }
                feedsBinding.descriptionTV.setOnClickListener {
                    viewPost()
                }
                feedsBinding.commentBtn.setOnClickListener {
                    viewPost()
                }
                bindLove()
                bindComment()
                bindRepost()
                bindContent()
            }
        }

        private fun bindContent() {
            if (feedModel.contentList.isNotEmpty()){
                //displays content with view pager 2
                feedsBinding.viewPager2.visibility = View.VISIBLE
                viewPageAdapter = ViewPageAdapter(feedsBinding.root.context, feedModel.contentList)
                feedsBinding.viewPager2.adapter = viewPageAdapter
                feedsBinding.viewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                //shows the indicator if the content is more than one
                if (feedModel.contentList.size > 1){
                    feedsBinding.circleIndicator3.visibility = View.VISIBLE
                    feedsBinding.circleIndicator3.setViewPager(feedsBinding.viewPager2)
                    feedsBinding.circleIndicator3
                }
            }
        }

        private fun bindRepost() {
            //TODO Not yet implemented
        }

        private fun bindComment() {

            feedsBinding.commentEdtTxt.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged( s: CharSequence?, start: Int, count: Int, after: Int ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val comment = feedsBinding.commentEdtTxt.text.toString()
                    if (comment.isNotEmpty()){
                        feedsBinding.loveLayout.visibility = View.GONE
                        feedsBinding.commentLayout.visibility = View.GONE
                        feedsBinding.repostLayout.visibility = View.GONE
                        feedsBinding.sendBtn.visibility = View.VISIBLE
                    } else {
                        feedsBinding.loveLayout.visibility = View.VISIBLE
                        feedsBinding.commentLayout.visibility = View.VISIBLE
                        feedsBinding.repostLayout.visibility = View.VISIBLE
                        feedsBinding.sendBtn.visibility = View.GONE
                    }
                }
            })

            feedsBinding.sendBtn.setOnClickListener {
                val comment = feedsBinding.commentEdtTxt.text.toString()
                if (comment.isNotEmpty()){
                    //TODO send comment to be implemented
                }
            }

            //TODO Not yet implemented
        }

        //FOR LOVE
        private fun bindLove() {
            updateFeedStatus()
            for (user in feedModel.usersLoves){
                if (user == FirebaseUtil().currentUserUid()){
                    feedsBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                    isLoved = true
                }
            }
            feedsBinding.loveBtn.setOnClickListener {
                if (isLoved){
                    //removes love
                    FirebaseUtil().retrieveCommunityFeedsCollection(feedModel.communityIdOfOrigin).document(feedModel.feedId)
                        .update("usersLoves", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            feedsBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                            isLoved = false
                            updateFeedStatus()
                        }
                } else {
                    //adds love
                    FirebaseUtil().retrieveCommunityFeedsCollection(feedModel.communityIdOfOrigin).document(feedModel.feedId)
                        .update("usersLoves", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            feedsBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                            isLoved = true
                            updateFeedStatus()
                        }
                }
            }
        }

        //Updates feed status every user interaction with the feed
        private fun updateFeedStatus(){
            FirebaseUtil().retrieveCommunityFeedsCollection(feedModel.communityIdOfOrigin)
                .document(feedModel.feedId).get().addOnSuccessListener {
                    val tempFeedModel = it.toObject(FeedsModel::class.java)!!
                    feedsBinding.lovesCountTV.text = tempFeedModel.usersLoves.size.toString()
                    feedsBinding.repostCountTV.text = tempFeedModel.usersReposts.size.toString()
                    //TODO add comments
                    //feedsBinding.commentsCountTV.text
                }
                .addOnFailureListener {
                    //if Offline
                    feedsBinding.lovesCountTV.text = feedModel.usersLoves.size.toString()
                    feedsBinding.repostCountTV.text = feedModel.usersReposts.size.toString()
                    //TODO add comments
                    //feedsBinding.commentsCountTV.text
                }
        }

        private fun viewPost(){
            val intent = Intent(context, ViewPost::class.java)
            intent.putExtra("communityId", feedModel.communityIdOfOrigin)
            intent.putExtra("feedId", feedModel.feedId)
            context.startActivity(intent)
        }

        private fun headToUserProfile() {
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userId", feedModel.ownerId)
            context.startActivity(intent)
        }
    }
}