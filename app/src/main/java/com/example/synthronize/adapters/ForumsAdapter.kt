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
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.ItemForumPostBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.ForumsModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

class ForumsAdapter(
    private val mainBinding: FragmentCommunityBinding,
    private val context: Context,
    options: FirestoreRecyclerOptions<ForumsModel>,
    private val sortByLikes: Boolean = true // true for sorting by likes, false for sorting by latest post
) : FirestoreRecyclerAdapter<ForumsModel, ForumsAdapter.ForumsViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val forumsBinding = ItemForumPostBinding.inflate(inflater, parent, false)
        return ForumsViewHolder(mainBinding, forumsBinding, context)
    }

    override fun onBindViewHolder(holder: ForumsViewHolder, position: Int, model: ForumsModel) {
        holder.bind(model)
    }

    override fun onDataChanged() {
        super.onDataChanged()
        // Notify the RecyclerView when the data changes
        notifyDataSetChanged()
    }

    // VIEW HOLDER
    inner class ForumsViewHolder(
        private val mainBinding: FragmentCommunityBinding,
        private val forumsBinding: ItemForumPostBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(forumsBinding.root) {

        private lateinit var forumsModel: ForumsModel
        private lateinit var viewPageAdapter: ViewPageAdapter
        private var isUpvoted = false
        private var isDownvoted = false

        fun bind(model: ForumsModel) {
            this.forumsModel = model

            // SETUP FEED
            FirebaseUtil().targetUserDetails(forumsModel.ownerId).get().addOnSuccessListener {
                val owner = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, owner.userID, forumsBinding.profileCIV)
                forumsBinding.usernameTV.text = owner.username
                forumsBinding.descriptionTV.text = forumsModel.caption
                forumsBinding.timestampTV.text =
                    DateUtil().formatTimestampToDate(forumsModel.createdTimestamp)
                forumsBinding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }
                forumsBinding.descriptionTV.setOnClickListener {
                    Toast.makeText(context, "To be implemented", Toast.LENGTH_SHORT).show()
                }
                forumsBinding.commentBtn.setOnClickListener {
                    Toast.makeText(context, "To be implemented", Toast.LENGTH_SHORT).show()
                }
                bindVote()
                bindComment()
                bindRepost()
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

        private fun bindRepost() {
            // TODO Not yet implemented
        }

        private fun bindComment() {
            forumsBinding.commentEdtTxt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val comment = forumsBinding.commentEdtTxt.text.toString()
                    forumsBinding.sendBtn.visibility = if (comment.isNotEmpty()) View.VISIBLE else View.GONE
                    forumsBinding.upBtn.visibility = View.VISIBLE
                    forumsBinding.downBtn.visibility = View.VISIBLE
                    forumsBinding.repostLayout.visibility = View.VISIBLE
                }
            })

            forumsBinding.sendBtn.setOnClickListener {
                val comment = forumsBinding.commentEdtTxt.text.toString()
                if (comment.isNotEmpty()) {
                    val commentModel = CommentModel(
                        commentOwnerId = FirebaseUtil().currentUserUid(),
                        comment = comment,
                        commentTimestamp = Timestamp.now()
                    )
                    FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                        .document(forumsModel.postId).collection("comments").add(commentModel)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                forumsBinding.commentEdtTxt.setText("")
                                Toast.makeText(context, "Comment sent", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

            // TODO Not yet implemented
        }

        // FOR VOTING
        private fun bindVote() {
            bindUpvote()
            bindDownvote()
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
                    FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                        .document(forumsModel.postId)
                        .update("upvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.upBtn.setImageResource(R.drawable.upbtn)
                            isUpvoted = false
                            updateFeedStatus()
                        }
                } else {
                    // Add upvote
                    FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                        .document(forumsModel.postId)
                        .update("upvoteList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.upBtn.setImageResource(R.drawable.upbtn)
                            isUpvoted = true
                            // If previously downvoted, remove downvote
                            if (isDownvoted) {
                                FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                                    .document(forumsModel.postId)
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
                    FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                        .document(forumsModel.postId)
                        .update("downvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.downBtn.setImageResource(R.drawable.downbtn)
                            isDownvoted = false
                            updateFeedStatus()
                        }
                } else {
                    // Add downvote
                    FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                        .document(forumsModel.postId)
                        .update("downvoteList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                        .addOnSuccessListener {
                            forumsBinding.downBtn.setImageResource(R.drawable.downbtn)
                            isDownvoted = true
                            // If previously upvoted, remove upvote
                            if (isUpvoted) {
                                FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
                                    .document(forumsModel.postId)
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
            // Determine whether to move the thread up or down based on the logic
            val totalVotes = forumsModel.upvoteList.size + forumsModel.downvoteList.size
            val upvotePercentage = if (totalVotes != 0) (forumsModel.upvoteList.size.toDouble() / totalVotes.toDouble()) * 100 else 0.0
            val downvotePercentage = if (totalVotes != 0) (forumsModel.downvoteList.size.toDouble() / totalVotes.toDouble()) * 100 else 0.0


            // Update UI with vote counts
            forumsBinding.upvoteCountTV.text = forumsModel.upvoteList.size.toString()
            forumsBinding.downvoteCountTV.text = forumsModel.downvoteList.size.toString()
        }

        private fun headToUserProfile() {
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userId", forumsModel.ownerId)
            context.startActivity(intent)
        }
    }
}
