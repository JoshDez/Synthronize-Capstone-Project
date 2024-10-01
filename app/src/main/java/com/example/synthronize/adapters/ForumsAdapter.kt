package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.ViewThread
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.ItemForumPostBinding
import com.example.synthronize.model.ForumModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue

class ForumsAdapter(
    private val mainBinding: FragmentCommunityBinding,
    private val context: Context,
    options: FirestoreRecyclerOptions<ForumModel>
) : FirestoreRecyclerAdapter<ForumModel, ForumsAdapter.ForumsViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val forumsBinding = ItemForumPostBinding.inflate(inflater, parent, false)
        return ForumsViewHolder(mainBinding, forumsBinding, context)
    }

    override fun onBindViewHolder(holder: ForumsViewHolder, position: Int, model: ForumModel) {
        holder.bind(model)
    }

    override fun onDataChanged() {
        super.onDataChanged()
        // Notify the RecyclerView when the data changes
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return super.getItemCount()
    }

    // Sort the items by upvotes and then by downvotes
    override fun getItem(position: Int): ForumModel {
        val sortedList = snapshots.sortedWith(compareByDescending<ForumModel> { it.upvoteList.size }
            .thenBy { it.downvoteList.size })
        return sortedList[position]
    }

    // VIEW HOLDER
    inner class ForumsViewHolder(
        private val mainBinding: FragmentCommunityBinding,
        private val forumsBinding: ItemForumPostBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(forumsBinding.root) {

        private lateinit var forumsModel: ForumModel
        private lateinit var viewPageAdapter: ViewPageAdapter
        private var isUpvoted = false
        private var isDownvoted = false

        fun bind(model: ForumModel) {
            this.forumsModel = model

            // SETUP FEED
            FirebaseUtil().targetUserDetails(forumsModel.ownerId).get().addOnSuccessListener {
                val owner = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, owner.userID, forumsBinding.profileCIV)
                forumsBinding.usernameTV.text = owner.username
                forumsBinding.descriptionTV.text = forumsModel.caption
                forumsBinding.timestampTV.text =
                    DateAndTimeUtil().getTimeAgo(forumsModel.createdTimestamp)
                forumsBinding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }
                forumsBinding.descriptionTV.setOnClickListener {
                    Toast.makeText(context, "To be implemented", Toast.LENGTH_SHORT).show()
                }
                forumsBinding.commentBtn.setOnClickListener {
                    viewThread()
                }
                val inflater = LayoutInflater.from(context)
                forumsBinding.menuBtn.setOnClickListener {
                 DialogUtil().openMenuDialog(context, inflater, "Forum", forumsModel.forumId, forumsModel.ownerId, forumsModel.communityId){}

                }
                bindUpvote()
                bindDownvote()
                bindContent()
            }
        }

        private fun bindContent() {
            if (forumsModel.contentList.isNotEmpty()) {
                // displays content with view pager 2
                forumsBinding.viewPager2.visibility = View.VISIBLE
                viewPageAdapter =
                    ViewPageAdapter(forumsBinding.root.context, forumsModel.contentList)
                forumsBinding.viewPager2.adapter = viewPageAdapter
                forumsBinding.viewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                // shows the indicator if the content is more than one
                if (forumsModel.contentList.size > 1) {
                    forumsBinding.circleIndicator3.visibility = View.VISIBLE
                    forumsBinding.circleIndicator3.setViewPager(forumsBinding.viewPager2)
                }
            } else {
                // default
                forumsBinding.viewPager2.visibility = View.GONE
                forumsBinding.circleIndicator3.visibility = View.GONE
            }
        }

        private fun bindUpvote() {
            // Default
            forumsBinding.upBtn.setImageResource(R.drawable.upbtn)

            // Update initial feed status
            updateFeedStatus()

            // Check if current user has upvoted
            for (user in forumsModel.upvoteList) {
                if (user == FirebaseUtil().currentUserUid()) {
                    forumsBinding.upBtn.setImageResource(R.drawable.upbtn)
                    isUpvoted = true
                }
            }

            forumsBinding.upBtn.setOnClickListener {
                if (isUpvoted) {
                    // Remove upvote
                    FirebaseUtil().retrieveCommunityForumsCollection(
                        forumsModel.communityId
                    )
                        .document(forumsModel.forumId)
                        .update("upvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.upBtn.setImageResource(R.drawable.upbtn)
                            isUpvoted = false
                            updateFeedStatus()
                        }
                } else {
                    // Add upvote
                    FirebaseUtil().retrieveCommunityForumsCollection(
                        forumsModel.communityId,
                    )
                        .document(forumsModel.forumId)
                        .update("upvoteList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.upBtn.setImageResource(R.drawable.upbtn)
                            isUpvoted = true
                            // If previously downvoted, remove downvote
                            if (isDownvoted) {
                                FirebaseUtil().retrieveCommunityForumsCollection(
                                    forumsModel.communityId
                                )
                                    .document(forumsModel.forumId)
                                    .update("downvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                                    .addOnSuccessListener {
                                        isDownvoted = false
                                    }
                            }
                            updateFeedStatus()
                        }
                }
            }
        }

        private fun bindDownvote() {
            // Default
            forumsBinding.downBtn.setImageResource(R.drawable.downbtn)

            // Update initial feed status
            updateFeedStatus()

            // Check if current user has downvoted
            for (user in forumsModel.downvoteList) {
                if (user == FirebaseUtil().currentUserUid()) {
                    forumsBinding.downBtn.setImageResource(R.drawable.downbtn)
                    isDownvoted = true
                }
            }

            forumsBinding.downBtn.setOnClickListener {
                if (isDownvoted) {
                    // Remove downvote
                    FirebaseUtil().retrieveCommunityForumsCollection(
                        forumsModel.communityId
                    )
                        .document(forumsModel.forumId)
                        .update("downvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.downBtn.setImageResource(R.drawable.downbtn)
                            isDownvoted = false
                            updateFeedStatus()
                        }
                } else {
                    // Add downvote
                    FirebaseUtil().retrieveCommunityForumsCollection(
                        forumsModel.communityId
                    )
                        .document(forumsModel.forumId)
                        .update("downvoteList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.downBtn.setImageResource(R.drawable.downbtn)
                            isDownvoted = true
                            // If previously upvoted, remove upvote
                            if (isUpvoted) {
                                FirebaseUtil().retrieveCommunityForumsCollection(
                                    forumsModel.communityId
                                )
                                    .document(forumsModel.forumId)
                                    .update("upvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                                    .addOnSuccessListener {
                                        isUpvoted = false
                                    }
                            }
                            updateFeedStatus()
                        }
                }
            }
        }

        private fun updateFeedStatus() {
            // Update UI with vote counts
            forumsBinding.upvoteCountTV.text = forumsModel.upvoteList.size.toString()
            forumsBinding.downvoteCountTV.text = forumsModel.downvoteList.size.toString()
            FirebaseUtil().retrieveCommunityForumsCollection(
                forumsModel.communityId
            ).document(forumsModel.forumId)
                .collection("comments").get().addOnSuccessListener {

                    forumsBinding.commentsCountTV.text = it.size().toString()
                }.addOnFailureListener {
                    forumsBinding.commentsCountTV.text = "0"
                }
        }

        private fun headToUserProfile() {
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userId", forumsModel.forumId)
            context.startActivity(intent)
        }

        private fun viewThread(){
            val intent = Intent(context, ViewThread::class.java)
            intent.putExtra("communityId", forumsModel.communityId )
            intent.putExtra("forumId", forumsModel.forumId)
            context.startActivity(intent)
        }
    }
}


