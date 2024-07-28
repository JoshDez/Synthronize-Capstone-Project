package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.databinding.ActivityOtherUserProfileBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.example.synthronize.utils.ProfileUtil
import com.google.firebase.firestore.FieldValue

class OtherUserProfile : AppCompatActivity(), OnNetworkRetryListener, OnRefreshListener {
    private lateinit var binding:ActivityOtherUserProfileBinding
    private lateinit var userModel: UserModel
    private lateinit var allFeedsAdapter:AllFeedsAdapter
    private var userID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtherUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //check for internet
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        userID = intent.getStringExtra("userID").toString()
        bindUserDetails()


        binding.postsRV.layoutManager = LinearLayoutManager(this)
        binding.filesRV.layoutManager = LinearLayoutManager(this)
        binding.likesRV.layoutManager = LinearLayoutManager(this)

        //main set on click listeners
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }


    private fun navigate(tab: String) {
        binding.postsRV.visibility = View.GONE
        binding.filesRV.visibility = View.GONE
        binding.likesRV.visibility = View.GONE
        //binding.communityChatsBtn.setTextColor(unselectedColor)
        //binding.communityChatsRV.visibility = View.GONE


        if (tab == "posts"){
            binding.postsRV.visibility = View.VISIBLE
            setupPostsRV()
            //binding.communityChatsBtn.setTextColor(selectedColor)

        }else if (tab == "files"){
            binding.filesRV.visibility = View.VISIBLE
            setupFilesRV()

        }else if (tab == "likes"){
            binding.likesRV.visibility = View.VISIBLE
            setupLikesRV()
        }
    }

    private fun setupLikesRV() {
        //TODO("Not yet implemented")
    }

    private fun setupFilesRV() {
        //TODO("Not yet implemented")
    }

    private fun setupPostsRV() {
        ProfileUtil().getUserPosts(this, userID){
            allFeedsAdapter = it
            binding.postsRV.adapter = allFeedsAdapter
        }
    }


    private fun bindUserDetails() {
        //TODO: Implement loading start
        binding.otherUserRefreshLayout.isRefreshing = true
        FirebaseUtil().targetUserDetails(userID).get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()){
                userModel = it.result.toObject(UserModel::class.java)!!
                binding.userDescriptionTV.text = userModel.description
                binding.userNameTV.text = userModel.username
                binding.userDisplayNameTV.text = userModel.fullName

                if (userModel.birthday.isNotEmpty()){
                    binding.birthdayLayout.visibility = View.VISIBLE
                    binding.birthdayTV.text =  DateAndTimeUtil().formatBirthDate(userModel.birthday)
                }

                AppUtil().setUserProfilePic(this, userID, binding.userProfileCIV)
                AppUtil().setUserCoverPic(this, userID, binding.userCoverIV)

                //bind counts
                ProfileUtil().getCommunitiesCount(userID) { communityCount ->
                    binding.communitiesCountTV.text = communityCount.toString()
                }
                ProfileUtil().getPostsCount(userID) { postsCount ->
                    binding.postsCountTV.text = postsCount.toString()
                }
                ProfileUtil().getFriendsCount(userID) { friendsCount ->
                    binding.friendsCountTV.text = friendsCount.toString()
                }
                //TODO count for files

                //displays the first tab
                binding.postsRV.visibility = View.VISIBLE
                setupPostsRV()

                //set on click listeners
                binding.postsBtn.setOnClickListener {
                    navigate("posts")
                }
                binding.filesBtn.setOnClickListener {
                    navigate("files")
                }
                binding.likesBtn.setOnClickListener {
                    navigate("likes")
                }

                binding.messageUserBtn.setOnClickListener {
                    val intent = Intent(this, Chatroom::class.java)
                    intent.putExtra("chatroomName", userModel.fullName)
                    intent.putExtra("userID", userID)
                    intent.putExtra("chatroomType", "direct_message")
                    startActivity(intent)
                }

                AppUtil().changeFriendsButtonState(binding.friendBtn, userModel)
                //TODO: Implement loading stop
                binding.otherUserRefreshLayout.isRefreshing = false
            }
        }
    }


    override fun onRefresh() {
        binding.otherUserRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            bindUserDetails()
        }, 1000)
    }

    override fun retryNetwork() {
        binding.otherUserRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            bindUserDetails()
        }, 1000)
    }
}