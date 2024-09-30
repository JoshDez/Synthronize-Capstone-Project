package com.example.synthronize

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.ThreadAdapter
import com.example.synthronize.databinding.ActivityViewThreadBinding
import com.example.synthronize.model.ForumsModel
import com.example.synthronize.model.ThreadModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

class ViewThread : AppCompatActivity() {
    private lateinit var binding: ActivityViewThreadBinding
    private lateinit var threadAdapter: ThreadAdapter
    private lateinit var postModel: ForumsModel
    private lateinit var communityId: String
    private lateinit var postId: String
    private lateinit var uriHashMap: HashMap<String, Uri>
    private var canPost: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        postId = intent.getStringExtra("postId").toString()

        getForumsModel()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getForumsModel() {
        FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(postId).get()
            .addOnSuccessListener {
                postModel = it.toObject(ForumsModel::class.java)!!

                ContentUtil().verifyThreadAvailability(postModel) { isAvailable ->
                    if (isAvailable) {
                        binding.forumTimestampTV.text =
                            DateAndTimeUtil().getTimeAgo(postModel.createdTimestamp)
                        binding.captionEdtTxt.setText(postModel.caption)

                        FirebaseUtil().targetUserDetails(postModel.ownerId).get()
                            .addOnSuccessListener { result ->
                                val user = result.toObject(UserModel::class.java)!!
                                binding.ownerUsernameTV.text = user.username
                                AppUtil().setUserProfilePic(
                                    this,
                                    user.userID,
                                    binding.profileCIV
                                )
                            }

                        if (postModel.contentList.isNotEmpty())
                            bindContent(postModel.contentList)
                        bindComments()

                    } else {
                        hideContent()
                    }

                }
            }
    }

    private fun bindComments() {
        val query: Query = FirebaseUtil().retrieveCommunityForumsCollection(communityId)
            .document(postId)
            .collection("comments")
            .orderBy("upvoteList", Query.Direction.DESCENDING) // Sort by upvotes first
            .orderBy("downvoteList", Query.Direction.ASCENDING) // Sort by downvotes second

        val options: FirestoreRecyclerOptions<ThreadModel> =
            FirestoreRecyclerOptions.Builder<ThreadModel>()
                .setQuery(query, ThreadModel::class.java)
                .build()

        binding.commentsRV.layoutManager = LinearLayoutManager(this)
        threadAdapter = ThreadAdapter(this, options, postId, communityId)
        binding.commentsRV.adapter = threadAdapter
        threadAdapter.startListening()

        binding.sendBtn.setOnClickListener {
            val comment = binding.threadEdtTxt.text.toString()
            if (comment.isNotEmpty()) {
                // Generate a new threadId
                val newThreadRef = FirebaseUtil().retrieveCommunityForumsCollection(communityId)
                    .document(postId)
                    .collection("comments")
                    .document() // This creates a new document reference with an auto-generated ID

                val threadId = newThreadRef.id // Get the auto-generated ID

                // Create the comment model with the generated threadId
                val commentModel = ThreadModel(
                    threadId = threadId,
                    commentOwnerId = FirebaseUtil().currentUserUid(),
                    comment = comment,
                    commentTimestamp = Timestamp.now()
                )

                // Add the comment to the collection with the generated threadId
                newThreadRef.set(commentModel).addOnCompleteListener {
                    if (it.isSuccessful) {
                        binding.threadEdtTxt.setText("")
                        bindComments() // Refresh the comments
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
            } else if (temp[1] == "Video") {
                binding.contentLayout.addView(ContentUtil().getVideoThumbnail(this, content))
                binding.contentLayout.addView(ContentUtil().createSpaceView(this))
            }
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

}
