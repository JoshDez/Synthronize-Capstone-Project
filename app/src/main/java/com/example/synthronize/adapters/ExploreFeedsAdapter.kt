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
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.ViewPost
import com.example.synthronize.databinding.ItemPostBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

class ExploreFeedsAdapter(private val context: Context, private val feedList: ArrayList<PostModel>):RecyclerView.Adapter<ExploreFeedsAdapter.ExploreViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreFeedsAdapter.ExploreViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPostBinding.inflate(inflater, parent, false)
        return ExploreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreFeedsAdapter.ExploreViewHolder, position: Int) {
        holder.bind(feedList[position])
    }

    override fun getItemCount(): Int {
        return feedList.size
    }

    inner class ExploreViewHolder(private val binding: ItemPostBinding): RecyclerView.ViewHolder(binding.root){
        private lateinit var postModel:PostModel
        private lateinit var viewPageAdapter: ViewPageAdapter
        private var isLoved:Boolean = false
        
        fun bind(model: PostModel){

            this.postModel = model

            //SETUP WRAPPER FOR REPOST OR COMMUNITY
            FirebaseUtil().retrieveCommunityDocument(postModel.communityId).get().addOnSuccessListener {
                val community = it.toObject(CommunityModel::class.java)!!
                binding.feedWrapperLayout.visibility = View.VISIBLE
                binding.wrapperName.text = community.communityName
                AppUtil().setCommunityProfilePic(context, community.communityId, binding.wrapperCIV)
            }

            //SETUP FEED
            FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener {
                val owner = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, owner.userID, binding.profileCIV)
                binding.usernameTV.text = owner.username
                binding.descriptionTV.text = postModel.caption
                binding.timestampTV.text = DateUtil().formatTimestampToDate(postModel.createdTimestamp)
                binding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }
                binding.descriptionTV.setOnClickListener {
                    viewPost()
                }
                binding.commentBtn.setOnClickListener {
                    viewPost()
                }
                bindLove()
                bindComment()
                bindRepost()
                bindContent()
            }
        }

        private fun bindContent() {
            if (postModel.contentList.isNotEmpty()){
                //displays content with view pager 2
                binding.viewPager2.visibility = View.VISIBLE
                viewPageAdapter = ViewPageAdapter(binding.root.context, postModel.contentList)
                binding.viewPager2.adapter = viewPageAdapter
                binding.viewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                //shows the indicator if the content is more than one
                if (postModel.contentList.size > 1){
                    binding.circleIndicator3.visibility = View.VISIBLE
                    binding.circleIndicator3.setViewPager(binding.viewPager2)
                    binding.circleIndicator3
                }
            } else {
                //default
                binding.viewPager2.visibility = View.GONE
                binding.circleIndicator3.visibility = View.GONE
            }
        }

        private fun bindRepost() {
            //TODO Not yet implemented
        }

        private fun bindComment() {

            binding.commentEdtTxt.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged( s: CharSequence?, start: Int, count: Int, after: Int ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val comment = binding.commentEdtTxt.text.toString()
                    if (comment.isNotEmpty()){
                        binding.loveLayout.visibility = View.GONE
                        binding.commentLayout.visibility = View.GONE
                        binding.repostLayout.visibility = View.GONE
                        binding.sendBtn.visibility = View.VISIBLE
                    } else {
                        binding.loveLayout.visibility = View.VISIBLE
                        binding.commentLayout.visibility = View.VISIBLE
                        binding.repostLayout.visibility = View.VISIBLE
                        binding.sendBtn.visibility = View.GONE
                    }
                }
            })

            binding.sendBtn.setOnClickListener {
                val comment = binding.commentEdtTxt.text.toString()
                if (comment.isNotEmpty()){
                    val commentModel = CommentModel(
                        commentOwnerId = FirebaseUtil().currentUserUid(),
                        comment = comment,
                        commentTimestamp = Timestamp.now()
                    )
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId).collection("comments").add(commentModel).addOnCompleteListener {
                        if (it.isSuccessful){
                            binding.commentEdtTxt.setText("")
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
            binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)

            updateFeedStatus()

            for (user in postModel.loveList){
                if (user == FirebaseUtil().currentUserUid()){
                    binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                    isLoved = true
                }
            }
            binding.loveBtn.setOnClickListener {
                if (isLoved){
                    //removes love
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .update("loveList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                            isLoved = false
                            updateFeedStatus()
                        }
                } else {
                    //adds love
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .update("loveList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
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
                    binding.lovesCountTV.text = tempPostModel.loveList.size.toString()
                    binding.repostCountTV.text = tempPostModel.repostList.size.toString()
                }
                .addOnFailureListener {
                    //if Offline
                    binding.lovesCountTV.text = postModel.loveList.size.toString()
                    binding.repostCountTV.text = postModel.repostList.size.toString()
                    FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                        .collection("comments").get().addOnSuccessListener {
                            Toast.makeText(context, "${it.size()}", Toast.LENGTH_SHORT).show()
                            binding.commentsCountTV.text = it.size().toString()
                        }.addOnFailureListener {
                            binding.commentsCountTV.text = "0"
                        }
                }
        }

        private fun viewPost(){
            val intent = Intent(context, ViewPost::class.java)
            intent.putExtra("communityId", postModel.communityId)
            intent.putExtra("postId", postModel.postId)
            context.startActivity(intent)
        }

        private fun headToUserProfile() {
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userId", postModel.ownerId)
            context.startActivity(intent)
        }

    }
}