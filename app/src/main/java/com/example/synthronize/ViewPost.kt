package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CommentAdapter
import com.example.synthronize.adapters.ContentUsersAdapter
import com.example.synthronize.databinding.ActivityViewPostBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.PostModel
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

class ViewPost : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener {
    private lateinit var binding:ActivityViewPostBinding
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var postModel: PostModel
    private lateinit var communityId:String
    private lateinit var postId:String
    private var isLoved:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        postId = intent.getStringExtra("postId").toString()

        binding.viewPostRefreshLayout.setOnRefreshListener(this)
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        getFeedModel()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getFeedModel(){
        binding.viewPostRefreshLayout.isRefreshing = true
        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).get().addOnCompleteListener {
            if (it.result.exists()){
                postModel = it.result.toObject(PostModel::class.java)!!

                ContentUtil().verifyCommunityContentAvailability(postModel.ownerId, postModel.communityId) { isAvailable ->
                    if (isAvailable){
                        binding.feedTimestampTV.text = DateAndTimeUtil().getTimeAgo(postModel.createdTimestamp)
                        binding.captionEdtTxt.setText(postModel.caption)
                        binding.contentLayout.removeAllViews()

                        FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener {result ->
                            val user = result.toObject(UserModel::class.java)!!
                            AppUtil().setUserProfilePic(this, user.userID, binding.profileCIV)


                            if (user.nickname.isNotEmpty()){
                                binding.displayNameTV.text = user.nickname
                                if (user.username.isNotEmpty())
                                    binding.displayNameTV2.text = "@${user.username}"
                            } else if(user.username.isNotEmpty()){
                                binding.displayNameTV.text = user.username
                            } else {
                                binding.displayNameTV.text = user.fullName
                            }

                            binding.displayNameTV.setOnClickListener {
                                headToUserProfile()
                            }
                            binding.displayNameTV2.setOnClickListener {
                                headToUserProfile()
                            }


                        }

                        binding.kebabMenuBtn.setOnClickListener {
                            DialogUtil().openMenuDialog(this, layoutInflater, "Post",
                                postModel.postId, postModel.ownerId, postModel.communityId){closeCurrentActivity ->
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

                        if (postModel.contentList.isNotEmpty())
                            bindContent(postModel.contentList)

                        bindLove()
                        bindComments()
                        bindSendPost()
                        binding.viewPostRefreshLayout.isRefreshing = false
                    } else {
                        hideContent()
                    }
                    binding.viewPostRefreshLayout.isRefreshing = false
                }
            } else {
                hideContent()
            }
        }
    }



    private fun headToUserProfile() {
        if (postModel.ownerId != FirebaseUtil().currentUserUid()){
            val intent = Intent(this, OtherUserProfile::class.java)
            intent.putExtra("userID", postModel.ownerId)
            startActivity(intent)
        }
    }



    fun showUserSelectionDialog(typedText: String, mentionRange: IntRange) {
        // Fetch users from Firestore
        val query = FirebaseUtil().allUsersCollectionReference()
            .whereGreaterThanOrEqualTo("username" , typedText)
            .whereLessThanOrEqualTo("username", typedText + "\uf8ff")

        query.get().addOnSuccessListener {
            if (it.documents.isNotEmpty()){
                val options = FirestoreRecyclerOptions.Builder<UserModel>()
                    .setQuery(query, UserModel::class.java)
                    .build()

                binding.userListRV.layoutManager = LinearLayoutManager(this)
                val adapter = ContentUsersAdapter(this, options, true, binding.commentEdtTxt, mentionRange)
                binding.userListRV.adapter = adapter
                adapter.startListening()
            }
        }
    }

    private fun sendNotificationsToMentionedUsers(comment:String){
        // Use regex to find all occurrences of @ followed by word characters, excluding the "@" in the result
        val mentionRegex = Regex("@(\\w+)")
        val matches = mentionRegex.findAll(comment)

        // Collect all matched usernames into a list without the "@"
        val usernames = matches.map { it.groupValues[1] }.toList()

        if (usernames.isNotEmpty()){
            FirebaseUtil().allUsersCollectionReference().whereIn("username", usernames).get().addOnSuccessListener {
                for (document in it.documents){
                    val userModel = document.toObject(UserModel::class.java)!!
                    //sends notification
                    NotificationUtil().sendNotificationToUser(this, postModel.postId, userModel.userID, "Mention",
                        "0","Post", postModel.communityId, DateAndTimeUtil().timestampToString(Timestamp.now()))
                }
            }
        }
    }

    private fun bindComments(){
        val commentsReference = FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).collection("comments")

        val query: Query = commentsReference.orderBy("commentTimestamp", Query.Direction.ASCENDING)

        val options: FirestoreRecyclerOptions<CommentModel> =
            FirestoreRecyclerOptions.Builder<CommentModel>().setQuery(query, CommentModel::class.java).build()

        binding.commentsRV.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(this, options, commentsReference)
        binding.commentsRV.adapter = commentAdapter
        commentAdapter.startListening()


        binding.commentEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged( s: CharSequence?, start: Int, count: Int, after: Int ) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { text ->
                    // Detect "@username" pattern with any characters after '@'
                    val mentionMatch = Regex("@(\\w+)$").find(text.subSequence(0, start + count))
                    if (mentionMatch != null) {
                        binding.userListRV.visibility = View.VISIBLE
                        val typedText = mentionMatch.groupValues[1] // Get text after @
                        showUserSelectionDialog(typedText, mentionMatch.range)
                    } else {
                        binding.userListRV.visibility = View.GONE
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })


        binding.sendBtn.setOnClickListener {
            val comment = binding.commentEdtTxt.text.toString()
            if (comment.isEmpty()){
                Toast.makeText(this, "Please type your comment", Toast.LENGTH_SHORT).show()
            } else if(AppUtil().containsSensitiveWords(comment)){
                Toast.makeText(this, "Your comment contains sensitive words", Toast.LENGTH_SHORT).show()
            } else {
                commentAdapter.stopListening()
                val commentModel = CommentModel()
                commentsReference.add(commentModel).addOnCompleteListener {
                    if (it.isSuccessful){
                        val commentModel = CommentModel(
                            commentId = it.result.id,
                            commentOwnerId = FirebaseUtil().currentUserUid(),
                            comment = comment,
                            commentTimestamp = Timestamp.now()
                        )

                        commentsReference.document(commentModel.commentId).set(commentModel).addOnCompleteListener {task ->
                            if (task.isSuccessful){
                                binding.commentEdtTxt.setText("")
                                bindComments()

                                //send mentioned users notification
                                sendNotificationsToMentionedUsers(comment)


                                //gets comments count before sending the notification
                                FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                                    .collection("comments").get().addOnSuccessListener { comments ->
                                        //sends notification
                                        NotificationUtil().sendNotificationToUser(this, postModel.postId, postModel.ownerId, "Comment",
                                            "${comments.size()}","Post", postModel.communityId, DateAndTimeUtil().timestampToString(Timestamp.now()))
                                    }
                            }
                        }
                    }
                }

            }
        }

    }

    //FOR LOVE
    private fun bindLove() {
        //default
        binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)

        updateFeedStatus()

        for (user in postModel.loveList) {
            if (user == FirebaseUtil().currentUserUid()) {
                binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                isLoved = true
            }
        }
        binding.loveBtn.setOnClickListener {
            if (isLoved) {
                //removes love
                FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId)
                    .document(postModel.postId)
                    .update("loveList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                        isLoved = false
                        updateFeedStatus()
                    }
            } else {
                //adds love
                FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId)
                    .document(postModel.postId)
                    .update("loveList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                        isLoved = true
                        updateFeedStatus()
                        //sends notification
                        NotificationUtil().sendNotificationToUser(this, postModel.postId, postModel.ownerId, "Love",
                            "${postModel.loveList.size + 1}","Post", postModel.communityId, DateAndTimeUtil().timestampToString(Timestamp.now()))
                    }
            }
        }
    }


    //For Send Post
    private fun bindSendPost() {
        binding.sendPostBtn.setOnClickListener {
            DialogUtil().openForwardContentDialog(this, layoutInflater, postModel.postId, postModel.communityId)
            if (!postModel.sendPostList.contains(FirebaseUtil().currentUserUid())){
                FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId)
                    .update("sendPostList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
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
                binding.sentPostCountTV.text = tempPostModel.sendPostList.size.toString()
                binding.lovesCountTV.setOnClickListener {
                    DialogUtil().openInteractionUsersList(this, layoutInflater, tempPostModel.loveList)
                }
            }
            .addOnFailureListener {
                //if Offline
                binding.lovesCountTV.text = postModel.loveList.size.toString()
                binding.sentPostCountTV.text = postModel.sendPostList.size.toString()
            }
    }

    private fun bindContent(contentList: List<String>){
        for (content in contentList){
            //Identifies the content
            val temp = content.split('-')
            if (temp[1] == "Image"){
                binding.contentLayout.addView(ContentUtil().getImageView(this, content))
                binding.contentLayout.addView(ContentUtil().createSpaceView(this))
            }else if (temp[1] == "Video"){
                binding.contentLayout.addView(ContentUtil().getVideoThumbnail(this, content))
                binding.contentLayout.addView(ContentUtil().createSpaceView(this))
            }
        }
    }


    private fun hideContent(){
        binding.scrollViewLayout.visibility = View.GONE
        binding.bottomToolbar.visibility = View.INVISIBLE
        binding.divider2.visibility = View.INVISIBLE
        binding.contentNotAvailableLayout.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (::commentAdapter.isInitialized)
            commentAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (::commentAdapter.isInitialized)
            commentAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::commentAdapter.isInitialized)
            commentAdapter.stopListening()
    }

    override fun onRefresh() {
        Handler().postDelayed({
            getFeedModel()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}