package com.example.synthronize.adapters

import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ViewFlipper
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.R
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.ItemFeedBinding
import com.example.synthronize.model.FeedsModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
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
                          private val context: Context)
        : RecyclerView.ViewHolder(feedsBinding.root), GestureDetector.OnGestureListener{

        private lateinit var feedModel:FeedsModel
        private lateinit var gestureDetector: GestureDetectorCompat
        private var isLoved:Boolean = false

        init {
            itemView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
        }
        fun bind(model: FeedsModel){

            this.feedModel = model
            gestureDetector = GestureDetectorCompat(feedsBinding.root.context, this)

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
                bindLove()
                bindComment()
                bindRepost()
                bindContent()
            }
        }

        private fun bindContent() {
            ContentUtil().addContentToViewFlipper(context, feedsBinding, feedModel.contentList)
        }

        private fun bindRepost() {
            //TODO Not yet implemented
        }

        private fun bindComment() {
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
        private fun headToUserProfile() {
            //TODO Not yet implemented
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onShowPress(e: MotionEvent) {}

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {}

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val sensitivity = 50
            if (e1 != null) {
                if (e1.x - e2.x > sensitivity) {
                    feedsBinding.contentLayout.showNext()
                } else if (e2.x - e1.x > sensitivity) {
                    feedsBinding.contentLayout.showPrevious()
                }
            }
            return true
        }
    }
}