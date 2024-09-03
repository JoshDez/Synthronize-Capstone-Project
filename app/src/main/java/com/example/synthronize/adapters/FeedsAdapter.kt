package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.synthronize.R
import com.example.synthronize.ViewPost
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.ItemPostBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

class FeedsAdapter(private val mainBinding: FragmentCommunityBinding, private val context: Context, 
                   options: FirestoreRecyclerOptions<PostModel>): FirestoreRecyclerAdapter<PostModel, FeedsAdapter.FeedsViewHolder>(options){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val feedBinding = ItemPostBinding.inflate(inflater, parent, false)
        return FeedsViewHolder(mainBinding, feedBinding, context, inflater)
    }

    override fun onBindViewHolder(holder: FeedsViewHolder, position: Int, model: PostModel) {
        holder.checkAvailabilityBeforeBind(model)
    }




    //VIEW HOLDER
    class FeedsViewHolder(private val mainBinding: FragmentCommunityBinding, private val feedBinding: ItemPostBinding,
                          private val context: Context, private val inflater: LayoutInflater) : RecyclerView.ViewHolder(feedBinding.root){

        private lateinit var postModel:PostModel
        private lateinit var viewPageAdapter: ViewPageAdapter
        private var isLoved:Boolean = false


        fun checkAvailabilityBeforeBind(model: PostModel){
            ContentUtil().verifyCommunityContentAvailability(model.ownerId, model.communityId){ isAvailable ->
                if(isAvailable){
                    bindPost(model)
                } else {
                    bindContentNotAvailable()
                }
            }
        }
        private fun bindContentNotAvailable(){
            feedBinding.descriptionTV.text = "Content Not Available"
            feedBinding.viewPager2.visibility = View.GONE
        }

        private fun bindPost(model: PostModel){

            this.postModel = model

            //SETUP POST CONTENT
            FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener {

                if (postModel.caption.isEmpty()){
                    feedBinding.descriptionTV.visibility = View.GONE
                }

                val owner = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, owner.userID, feedBinding.profileCIV)
                feedBinding.usernameTV.text = owner.username
                feedBinding.descriptionTV.text = postModel.caption
                feedBinding.timestampTV.text = DateAndTimeUtil().getTimeAgo(postModel.createdTimestamp)
                feedBinding.usernameTV.setOnClickListener {
                    AppUtil().headToUserProfile(context, postModel.ownerId)
                }
                feedBinding.descriptionTV.setOnClickListener {
                    viewPost()
                }
                feedBinding.commentBtn.setOnClickListener {
                    viewPost()
                }
                feedBinding.menuBtn.setOnClickListener {
                    DialogUtil().openMenuDialog(context, inflater, "Post", postModel.postId, postModel.ownerId, postModel.communityId)
                }
                bindLove()
                bindComment()
                bindSendPost()
                bindContent()
            }
        }

        private fun bindContent() {
            if (postModel.contentList.isNotEmpty()){
                //displays content with view pager 2
                feedBinding.viewPager2.visibility = View.VISIBLE
                viewPageAdapter = ViewPageAdapter(feedBinding.root.context, postModel.contentList)
                feedBinding.viewPager2.adapter = viewPageAdapter
                feedBinding.viewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                //shows the indicator if the content is more than one
                if (postModel.contentList.size > 1){
                    feedBinding.circleIndicator3.visibility = View.VISIBLE
                    feedBinding.circleIndicator3.setViewPager(feedBinding.viewPager2)
                    feedBinding.circleIndicator3
                }
            } else {
                //default
                feedBinding.viewPager2.visibility = View.GONE
                feedBinding.circleIndicator3.visibility = View.GONE
            }
        }

        private fun bindSendPost() {
            feedBinding.sendPostBtn.setOnClickListener {
                DialogUtil().openForwardContentDialog(context, inflater, postModel.postId, postModel.communityId)
                if (!postModel.sendPostList.contains(FirebaseUtil().currentUserUid())){
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .update("sendPostList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            updateFeedStatus()
                        }
                }
            }
        }

        private fun bindComment() {

            feedBinding.commentEdtTxt.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged( s: CharSequence?, start: Int, count: Int, after: Int ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val comment = feedBinding.commentEdtTxt.text.toString()
                    if (comment.isNotEmpty()){
                        feedBinding.loveLayout.visibility = View.GONE
                        feedBinding.commentLayout.visibility = View.GONE
                        feedBinding.sendBtn.visibility = View.VISIBLE
                        feedBinding.sendPostLayout.visibility = View.GONE
                    } else {
                        feedBinding.loveLayout.visibility = View.VISIBLE
                        feedBinding.commentLayout.visibility = View.VISIBLE
                        feedBinding.sendBtn.visibility = View.GONE
                        feedBinding.sendPostLayout.visibility = View.VISIBLE
                    }
                }
            })

            feedBinding.sendBtn.setOnClickListener {
                val comment = feedBinding.commentEdtTxt.text.toString()
                if (comment.isNotEmpty()){
                    val commentModel = CommentModel(
                        commentOwnerId = FirebaseUtil().currentUserUid(),
                        comment = comment,
                        commentTimestamp = Timestamp.now()
                    )
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId).collection("comments").add(commentModel).addOnCompleteListener {
                        if (it.isSuccessful){
                            feedBinding.commentEdtTxt.setText("")
                            updateFeedStatus()
                            Toast.makeText(context, "Comment sent", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            //TODO Not yet implemented
        }

        //FOR LOVE
        private fun bindLove() {
            //default
            feedBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)

            updateFeedStatus()

            for (user in postModel.loveList){
                if (user == FirebaseUtil().currentUserUid()){
                    feedBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                    isLoved = true
                }
            }

            feedBinding.loveBtn.setOnClickListener {
                if (isLoved){
                    //removes love
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .update("loveList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            feedBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                            isLoved = false
                            updateFeedStatus()
                        }
                } else {
                    //adds love
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .update("loveList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            feedBinding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                            isLoved = true
                            updateFeedStatus()
                        }
                }
            }
        }

        //Updates feed status every user interaction with the feed
        private fun updateFeedStatus(){
            FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId)
                .document(postModel.postId).get().addOnSuccessListener {
                    val tempPostModel = it.toObject(PostModel::class.java)!!
                    feedBinding.lovesCountTV.text = tempPostModel.loveList.size.toString()
                    feedBinding.sentPostCountTV.text = tempPostModel.sendPostList.size.toString()
                }
                .addOnFailureListener {
                    //if Offline
                    feedBinding.lovesCountTV.text = postModel.loveList.size.toString()
                    feedBinding.sentPostCountTV.text = postModel.sendPostList.size.toString()
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .collection("comments").get().addOnSuccessListener {
                            Toast.makeText(context, "${it.size()}", Toast.LENGTH_SHORT).show()
                            feedBinding.commentsCountTV.text = it.size().toString()
                        }.addOnFailureListener {
                            feedBinding.commentsCountTV.text = "0"
                        }
                }
            FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                .collection("comments").get().addOnSuccessListener {
                    feedBinding.commentsCountTV.text = it.documents.size.toString()
                }
        }

        private fun viewPost(){
            val intent = Intent(context, ViewPost::class.java)
            intent.putExtra("communityId", postModel.communityId)
            intent.putExtra("postId", postModel.postId)
            context.startActivity(intent)
        }
    }
}