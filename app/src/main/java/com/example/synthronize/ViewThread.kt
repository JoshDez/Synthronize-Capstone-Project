package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.ThreadAdapter
import com.example.synthronize.databinding.ActivityViewThreadBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.ForumModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.ThreadModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.example.synthronize.utils.NotificationUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

class ViewThread : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener {
    private lateinit var binding: ActivityViewThreadBinding
    private lateinit var threadAdapter: ThreadAdapter
    private lateinit var forumsModel: ForumModel
    private lateinit var communityId: String
    private lateinit var forumId: String
    private var isDownvoted:Boolean = false
    private var isUpvoted:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        forumId = intent.getStringExtra("forumId").toString()

        binding.viewThreadRefreshLayout.setOnRefreshListener(this)
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        getForumsModel()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getForumsModel() {
        binding.viewThreadRefreshLayout.isRefreshing = true

        FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(forumId).get()
            .addOnSuccessListener {
                forumsModel = it.toObject(ForumModel::class.java)!!

                ContentUtil().verifyThreadAvailability(forumsModel) { isAvailable ->
                    if (isAvailable) {
                        binding.forumTimestampTV.text =
                            DateAndTimeUtil().getTimeAgo(forumsModel.createdTimestamp)
                        binding.captionEdtTxt.setText(forumsModel.caption)

                        FirebaseUtil().targetUserDetails(forumsModel.ownerId).get()
                            .addOnSuccessListener { result ->
                                val user = result.toObject(UserModel::class.java)!!
                                binding.ownerUsernameTV.text = user.username
                                AppUtil().setUserProfilePic(
                                    this,
                                    user.userID,
                                    binding.profileCIV
                                )
                            }

                        binding.kebabMenuBtn.setOnClickListener {
                            DialogUtil().openMenuDialog(this, layoutInflater, "Forum",
                                forumsModel.forumId, forumsModel.ownerId, forumsModel.communityId){closeCurrentActivity ->
                                if (closeCurrentActivity){
                                    Handler().postDelayed({
                                        onBackPressed()
                                    }, 2000)
                                }
                            }
                        }

                        binding.profileCIV.setOnClickListener {
                            headToUserProfile()
                        }

                        binding.ownerUsernameTV.setOnClickListener {
                            headToUserProfile()
                        }

                        if (forumsModel.contentList.isNotEmpty())
                            bindContent(forumsModel.contentList)
                        
                        bindComments()
                        bindUpvote()
                        bindDownvote()
                    } else {
                        hideContent()
                    }
                    binding.viewThreadRefreshLayout.isRefreshing = false
                }
            }
    }

    private fun headToUserProfile() {
        if (forumsModel.ownerId != FirebaseUtil().currentUserUid()){
            val intent = Intent(this, OtherUserProfile::class.java)
            intent.putExtra("userID", forumsModel.ownerId)
            startActivity(intent)
        }
    }
    private fun bindComments() {
        val query: Query = FirebaseUtil().retrieveCommunityForumsCollection(communityId)
            .document(forumId)
            .collection("comments")
            .orderBy("upvoteList", Query.Direction.DESCENDING) // Sort by upvotes first
            .orderBy("downvoteList", Query.Direction.ASCENDING) // Sort by downvotes second

        val options: FirestoreRecyclerOptions<ThreadModel> =
            FirestoreRecyclerOptions.Builder<ThreadModel>()
                .setQuery(query, ThreadModel::class.java)
                .build()

        binding.commentsRV.layoutManager = LinearLayoutManager(this)
        threadAdapter = ThreadAdapter(this, options, forumId, communityId)
        binding.commentsRV.adapter = threadAdapter
        threadAdapter.startListening()

        binding.sendBtn.setOnClickListener {
            val comment = binding.threadEdtTxt.text.toString()
            if (comment.isNotEmpty()) {
                // Generate a new threadId
                val newThreadRef = FirebaseUtil().retrieveCommunityForumsCollection(communityId)
                    .document(forumId)
                    .collection("comments")
                    .document() // This creates a new document reference with an auto-generated ID

                val threadId = newThreadRef.id // Get the auto-generated ID

                // Create the comment model with the generated threadId
                val threadModel = ThreadModel(
                    threadId = threadId,
                    commentOwnerId = FirebaseUtil().currentUserUid(),
                    comment = comment,
                    commentTimestamp = Timestamp.now()
                )

                // Add the comment to the collection with the generated threadId
                newThreadRef.set(threadModel).addOnCompleteListener {
                    if (it.isSuccessful) {
                        binding.threadEdtTxt.setText("")
                        bindComments() // Refresh the comments
                        FirebaseUtil().retrieveCommunityForumsCollection(communityId)
                            .document(forumId)
                            .collection("comments").get().addOnSuccessListener {comments ->
                                NotificationUtil().sendNotificationToUser(this, forumsModel.forumId, forumsModel.ownerId, "Comment",
                                    "${comments.size() + 1}","Forum", forumsModel.communityId, DateAndTimeUtil().timestampToString(
                                        Timestamp.now()))
                        }
                    }
                }
            }
        }
    }

    private fun bindContent(contentList: List<String>) {
        for (content in contentList) {
            // Identifies the content
            val temp = content.split('-')
            if (temp[1] == "Image") {
                binding.contentLayout.addView(ContentUtil().getImageView(this, content))
                binding.contentLayout.addView(ContentUtil().createSpaceView(this))
            }
        }
    }


    private fun bindUpvote() {
        // Default
        binding.upBtn.setImageResource(R.drawable.upbtn)

        // Update initial feed status
        updateFeedStatus()

        // Check if current user has upvoted
        for (user in forumsModel.upvoteList) {
            if (user == FirebaseUtil().currentUserUid()) {
                binding.upBtn.setImageResource(R.drawable.upbtn)
                isUpvoted = true
            }
        }

        binding.upBtn.setOnClickListener {
            if (isUpvoted) {
                // Remove upvote
                FirebaseUtil().retrieveCommunityForumsCollection(
                    forumsModel.communityId
                )
                    .document(forumsModel.forumId)
                    .update("upvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        binding.upBtn.setImageResource(R.drawable.upbtn)
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
                        binding.upBtn.setImageResource(R.drawable.upbtn)
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
                                    updateFeedStatus()
                                }
                        }
                        updateFeedStatus()
                        NotificationUtil().sendNotificationToUser(this, forumsModel.forumId, forumsModel.ownerId, "Upvote",
                            "${forumsModel.upvoteList.size + 1}","Forum", forumsModel.communityId, DateAndTimeUtil().timestampToString(
                                Timestamp.now()))
                    }
            }
        }
    }

    private fun bindDownvote() {
        // Default
        binding.downBtn.setImageResource(R.drawable.downbtn)

        // Update initial feed status
        updateFeedStatus()

        // Check if current user has downvoted
        for (user in forumsModel.downvoteList) {
            if (user == FirebaseUtil().currentUserUid()) {
                binding.downBtn.setImageResource(R.drawable.downbtn)
                isDownvoted = true
            }
        }

        binding.downBtn.setOnClickListener {
            if (isDownvoted) {
                // Remove downvote
                FirebaseUtil().retrieveCommunityForumsCollection(
                    forumsModel.communityId
                )
                    .document(forumsModel.forumId)
                    .update("downvoteList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        binding.downBtn.setImageResource(R.drawable.downbtn)
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
                        binding.downBtn.setImageResource(R.drawable.downbtn)
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
                                    updateFeedStatus()
                                }
                        }
                        updateFeedStatus()
                        NotificationUtil().sendNotificationToUser(this, forumsModel.forumId, forumsModel.ownerId, "Downvote",
                            "${forumsModel.downvoteList.size + 1}","Forum", forumsModel.communityId, DateAndTimeUtil().timestampToString(
                                Timestamp.now()))
                    }
            }
        }
    }

    private fun updateFeedStatus() {
        // Update UI with vote counts
        FirebaseUtil().retrieveCommunityForumsCollection(forumsModel.communityId)
            .document(forumsModel.forumId).get().addOnSuccessListener {
                val tempForumModel = it.toObject(ForumModel::class.java)!!
                binding.upvoteCountTV.text = tempForumModel.upvoteList.size.toString()
                binding.downvoteCountTV.text = tempForumModel.downvoteList.size.toString()
            }
            .addOnFailureListener {
                //if Offline
                binding.upvoteCountTV.text = forumsModel.upvoteList.size.toString()
                binding.downvoteCountTV.text = forumsModel.downvoteList.size.toString()
            }
    }

    private fun hideContent() {
        binding.scrollViewLayout.visibility = View.GONE
        binding.bottomToolbar.visibility = View.INVISIBLE
        binding.divider2.visibility = View.INVISIBLE
        binding.contentNotAvailableLayout.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (::threadAdapter.isInitialized)
            threadAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (::threadAdapter.isInitialized)
            threadAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::threadAdapter.isInitialized)
            threadAdapter.stopListening()
    }

    override fun onRefresh() {
        Handler().postDelayed({
            getForumsModel()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }

}
