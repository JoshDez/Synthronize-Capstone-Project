package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.R
import com.example.synthronize.databinding.ItemThreadCommentBinding
import com.example.synthronize.model.ThreadModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue

class ThreadAdapter(
    private val context: Context,
    options: FirestoreRecyclerOptions<ThreadModel>,
    private val postId: String,
    private val communityId: String
) : FirestoreRecyclerAdapter<ThreadModel, ThreadAdapter.ThreadViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemThreadCommentBinding.inflate(inflater, parent, false)
        return ThreadViewHolder(binding, context, postId, communityId, ::updateFeedStatus)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int, model: ThreadModel) {
        holder.bind(model)
    }

    // Function to refresh the feed inside the adapter
    private fun updateFeedStatus() {
        notifyDataSetChanged()  // Refresh the adapter's data
        Toast.makeText(context,"Updated lol", Toast.LENGTH_SHORT).show()

    }

    class ThreadViewHolder(
        private val binding: ItemThreadCommentBinding,
        private val context: Context,
        private val postId: String,
        private val communityId: String,
        private val updateFeedStatus: () -> Unit  // Callback to refresh feed
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var threadModel: ThreadModel
        private var isUpvoted = false
        private var isDownvoted = false

        fun bind(model: ThreadModel) {
            threadModel = model

            binding.descriptionTV.text = model.comment
            binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(model.commentTimestamp)
            FirebaseUtil().targetUserDetails(model.commentOwnerId).get().addOnSuccessListener {
                val user = it.toObject(UserModel::class.java)!!
                binding.usernameTV.text = user.username
                AppUtil().setUserProfilePic(context, user.userID, binding.profileCIV)
            }

            // Initialize upvote and downvote state
            updateVoteButtons()

            binding.upBtn.setOnClickListener {
                updateVoteButtons()
                updateFeedStatus
                handleUpvote()
            }

            binding.downBtn.setOnClickListener {
                handleDownvote()
            }
        }

        private fun handleUpvote() {
            val currentUserUid = FirebaseUtil().currentUserUid()
            val docRef = FirebaseUtil().retrieveCommunityForumsCommentCollection(communityId, postId).document(threadModel.threadId)
            val updates = mutableMapOf<String, Any>()

            if (isUpvoted) {
                // Remove upvote and decrement vote count
                updates["upvoteList"] = FieldValue.arrayRemove(currentUserUid)
                updates["voteCount"] = FieldValue.increment(-1)  // Decrement vote count
                isUpvoted = false
                updateVoteButtons()
            } else {
                // Add upvote and increment vote count
                updates["upvoteList"] = FieldValue.arrayUnion(currentUserUid)
                updates["voteCount"] = FieldValue.increment(1)  // Increment vote count
                if (isDownvoted) {
                    // Remove downvote if previously downvoted
                    updates["downvoteList"] = FieldValue.arrayRemove(currentUserUid)
                    updates["voteCount"] = FieldValue.increment(1)  // Adjust vote count
                    isDownvoted = false
                }
                isUpvoted = true
                updateVoteButtons()
            }

            docRef.update(updates).addOnSuccessListener {
                updateVoteButtons()
                updateFeedStatus()  // Call to refresh feed after upvote
            }
        }

        private fun handleDownvote() {
            val currentUserUid = FirebaseUtil().currentUserUid()
            val docRef = FirebaseUtil().retrieveCommunityForumsCommentCollection(communityId, postId).document(threadModel.threadId)
            val updates = mutableMapOf<String, Any>()

            if (isDownvoted) {
                // Remove downvote and increment vote count
                updates["downvoteList"] = FieldValue.arrayRemove(currentUserUid)
                updates["voteCount"] = FieldValue.increment(1)  // Increment vote count
                isDownvoted = false
                updateVoteButtons()
                updateFeedStatus
            } else {
                // Add downvote and decrement vote count
                updates["downvoteList"] = FieldValue.arrayUnion(currentUserUid)
                updates["voteCount"] = FieldValue.increment(-1)  // Decrement vote count
                if (isUpvoted) {
                    // Remove upvote if previously upvoted
                    updates["upvoteList"] = FieldValue.arrayRemove(currentUserUid)
                    updates["voteCount"] = FieldValue.increment(-1)  // Adjust vote count
                    isUpvoted = false
                }
                isDownvoted = true
                updateFeedStatus
            }

            docRef.update(updates).addOnSuccessListener {
                updateVoteButtons()
                updateFeedStatus()  // Call to refresh feed after downvote
            }
        }

        private fun updateVoteButtons() {
            val currentUserUid = FirebaseUtil().currentUserUid()

            isUpvoted = threadModel.upvoteList.contains(currentUserUid)
            isDownvoted = threadModel.downvoteList.contains(currentUserUid)

            // Update button resources based on vote state
            binding.upBtn.setImageResource(if (isUpvoted) R.drawable.upbtn else R.drawable.upbtn)
            binding.downBtn.setImageResource(if (isDownvoted) R.drawable.downbtn else R.drawable.downbtn)

            // Update vote counts
            binding.upvoteCountTV.text = threadModel.upvoteList.size.toString()
            binding.downvoteCountTV.text = threadModel.downvoteList.size.toString()
            Toast.makeText(context,"Updated lol 2", Toast.LENGTH_SHORT).show()
        }


    }
    override fun onDataChanged() {
        super.onDataChanged()
        // Notify the RecyclerView when the data changes
        notifyDataSetChanged()
    }
}
