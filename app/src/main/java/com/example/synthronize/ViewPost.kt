package com.example.synthronize

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import com.example.synthronize.databinding.ActivityViewPostBinding
import com.example.synthronize.model.FeedsModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.FirebaseUtil

class ViewPost : AppCompatActivity() {
    private lateinit var binding:ActivityViewPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val communityId = intent.getStringExtra("communityId").toString()
        val feedId = intent.getStringExtra("feedId").toString()

        getFeedModel(communityId, feedId)

    }

    private fun getFeedModel(communityId:String, feedId: String){
        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(feedId).get().addOnSuccessListener {
            val feedModel = it.toObject(FeedsModel::class.java)!!
            binding.feedTimestampTV.text = feedModel.feedTimestamp.toDate().toString()
            binding.captionEdtTxt.setText(feedModel.feedCaption)
            bindButtons()

            FirebaseUtil().targetUserDetails(feedModel.ownerId).get().addOnSuccessListener {result ->
                val user = result.toObject(UserModel::class.java)!!
                binding.ownerUsernameTV.text = user.username
                AppUtil().setUserProfilePic(this, user.userID, binding.profileCIV)
            }

            if (feedModel.contentList.isNotEmpty())
                bindContent(feedModel.contentList)

        }
    }

    private fun bindButtons(){
        binding.backBtn.setOnClickListener {
            this.finish()
        }
        binding.sendBtn.setOnClickListener {
            //TODO to be implemented
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
}