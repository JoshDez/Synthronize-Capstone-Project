package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityOtherUserProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.google.firebase.firestore.FieldValue

class OtherUserProfile : AppCompatActivity() {
    private lateinit var binding:ActivityOtherUserProfileBinding
    private lateinit var userModel: UserModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtherUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //check for internet
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root)

        val userID = intent.getStringExtra("userID").toString()
        bindUserDetails(userID)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun bindUserDetails(userID:String) {
        //TODO: Implement loading start

        FirebaseUtil().targetUserDetails(userID).get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()){
                userModel = it.result.toObject(UserModel::class.java)!!
                binding.userDescriptionTV.text = userModel.description
                binding.userNameTV.text = userModel.username
                binding.userDisplayNameTV.text = userModel.fullName
                if (userModel.birthday.isNotEmpty()){
                    binding.birthdayLayout.visibility = View.VISIBLE
                    binding.birthdayTV.text =  DateUtil().formatBirthDate(userModel.birthday)
                }

                AppUtil().setUserProfilePic(this, userID, binding.userProfileCIV)
                AppUtil().setUserCoverPic(this, userID, binding.userCoverIV)

                //bind counts
                getCommunitiesCount()
                getFriendsCount()
                getPostsCount()

                binding.messageUserBtn.setOnClickListener {
                    val intent = Intent(this, Chatroom::class.java)
                    intent.putExtra("chatroomName", userModel.fullName)
                    intent.putExtra("userID", userID)
                    intent.putExtra("chatroomType", "direct_message")
                    startActivity(intent)
                }

                changeFriendsButtonState()
                //TODO: Implement loading stop
            }
        }
    }

    private fun changeFriendsButtonState(){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val myUserModel = it.toObject(UserModel::class.java)!!

            //checks if already friends with user
            if (AppUtil().isUserOnList(userModel.friendsList, FirebaseUtil().currentUserUid())){
                binding.friendBtn.text = "Unfriend"
                binding.friendBtn.setOnClickListener {
                    userModel.friendsList = userModel.friendsList.filterNot { it == FirebaseUtil().currentUserUid() }
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState()
                    }
                }

            } else if (AppUtil().isUserOnList(userModel.friendRequests, FirebaseUtil().currentUserUid())){
                binding.friendBtn.text = "Cancel Request"
                binding.friendBtn.setOnClickListener {
                    userModel.friendRequests = userModel.friendRequests.filterNot { it == FirebaseUtil().currentUserUid() }
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState()
                    }
                }
            } else if (AppUtil().isUserOnList(myUserModel.friendRequests, userModel.userID)){
                binding.friendBtn.text = "Accept Request"
                binding.friendBtn.setOnClickListener {
                    FirebaseUtil().currentUserDetails().update("friendsList", FieldValue.arrayUnion(userModel.userID))
                    FirebaseUtil().targetUserDetails(userModel.userID).update("friendsList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                }
            } else {
                binding.friendBtn.text = "Add Friend"
                binding.friendBtn.setOnClickListener {
                    userModel.friendRequests = userModel.friendRequests.plus(FirebaseUtil().currentUserUid())
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState()
                    }
                }
            }
        }
    }


    private fun getCommunitiesCount(){
        FirebaseUtil().retrieveAllCommunityCollection()
            .whereArrayContains("communityMembers", FirebaseUtil().currentUserUid()).get().addOnSuccessListener {
                binding.communitiesCountTV.text = it.size().toString()
            }.addOnFailureListener {
                binding.communitiesCountTV.text = "0"
            }
    }

    private fun getFriendsCount(){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            binding.friendsCountTV.text = user.friendsList.size.toString()
        }
    }

    private fun getPostsCount(){
        FirebaseUtil().retrieveAllCommunityCollection().get()
            .addOnSuccessListener { querySnapshot ->
                var totalPosts = 0
                for (document in querySnapshot.documents) {
                    FirebaseUtil().retrieveAllCommunityCollection()
                        .document(document.id) // Access each document within the collection
                        .collection("feeds")
                        .whereEqualTo("ownerId", FirebaseUtil().currentUserUid())
                        .get()
                        .addOnSuccessListener { feedsSnapshot ->
                            totalPosts += feedsSnapshot.size()
                            binding.postsCountTV.text = totalPosts.toString()
                        }
                }
            }
            .addOnFailureListener {
                binding.postsCountTV.text = "0"
            }
    }

}