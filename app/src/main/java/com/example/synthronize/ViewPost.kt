package com.example.synthronize

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.CommentAdapter
import com.example.synthronize.databinding.ActivityViewPostBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

class ViewPost : AppCompatActivity() {
    private lateinit var binding:ActivityViewPostBinding
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var communityId:String
    private lateinit var postId:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        postId = intent.getStringExtra("postId").toString()

        getFeedModel()
    }

    private fun getFeedModel(){
        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).get().addOnSuccessListener {
            val feedModel = it.toObject(PostModel::class.java)!!
            binding.feedTimestampTV.text = DateUtil().formatTimestampToDate(feedModel.createdTimestamp)
            binding.captionEdtTxt.setText(feedModel.caption)
            binding.backBtn.setOnClickListener {
                this.finish()
            }

            FirebaseUtil().targetUserDetails(feedModel.ownerId).get().addOnSuccessListener {result ->
                val user = result.toObject(UserModel::class.java)!!
                binding.ownerUsernameTV.text = user.username
                AppUtil().setUserProfilePic(this, user.userID, binding.profileCIV)
            }

            if (feedModel.contentList.isNotEmpty())
                bindContent(feedModel.contentList)

            bindComments()

        }
    }

    private fun bindComments(){
        val query: Query = FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).collection("comments")
            .orderBy("commentTimestamp", Query.Direction.ASCENDING)

        val options: FirestoreRecyclerOptions<CommentModel> =
            FirestoreRecyclerOptions.Builder<CommentModel>().setQuery(query, CommentModel::class.java).build()

        binding.commentsRV.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(this, options)
        binding.commentsRV.adapter = commentAdapter
        commentAdapter.startListening()

        binding.sendBtn.setOnClickListener {
            val comment = binding.commentEdtTxt.text.toString()
            if (comment.isNotEmpty()){
                val commentModel = CommentModel(
                    commentOwnerId = FirebaseUtil().currentUserUid(),
                    comment = comment,
                    commentTimestamp = Timestamp.now()
                )
                FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).collection("comments").add(commentModel).addOnCompleteListener {
                    if (it.isSuccessful){
                        binding.commentEdtTxt.setText("")
                        bindComments()
                    }
                }
            }
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
                //TODO to be implemented
            }
        }
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
}