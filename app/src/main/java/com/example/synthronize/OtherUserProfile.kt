package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.databinding.ActivityOtherUserProfileBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.example.synthronize.utils.ProfileUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class OtherUserProfile : AppCompatActivity(), OnNetworkRetryListener, OnRefreshListener {
    private lateinit var binding:ActivityOtherUserProfileBinding
    private lateinit var userModel: UserModel
    private lateinit var myUserModel: UserModel
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

        binding.otherUserRefreshLayout.setOnRefreshListener(this)

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

        }
    }

    private fun setupFilesRV() {
        //TODO("Not yet implemented")
    }

    private fun setupPostsRV() {
        ProfileUtil().getUserPosts(this, userID){
            allFeedsAdapter = it
            binding.postsRV.layoutManager = LinearLayoutManager(this)
            binding.postsRV.adapter = allFeedsAdapter
        }
    }


    private fun bindUserDetails() {
        binding.otherUserRefreshLayout.isRefreshing = true
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            myUserModel = it.toObject(UserModel::class.java)!!


            if (AppUtil().isIdOnList(myUserModel.blockList, userID)){
                //removes message btn if the user is blocked by current user
                binding.messageUserBtn.visibility = View.GONE
            }

            FirebaseUtil().targetUserDetails(userID).get().addOnCompleteListener {otherUser ->
                if (otherUser.isSuccessful && otherUser.result.exists()){
                    userModel = otherUser.result.toObject(UserModel::class.java)!!

                    if (!AppUtil().isIdOnList(userModel.blockList, FirebaseUtil().currentUserUid())){
                        //displays the profile
                        binding.userDescriptionTV.text = userModel.description
                        binding.userNameTV.text = "@${userModel.username}"
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

                        binding.messageUserBtn.setOnClickListener {
                            val intent = Intent(this, Chatroom::class.java)
                            intent.putExtra("chatroomName", userModel.fullName)
                            intent.putExtra("userID", userID)
                            intent.putExtra("chatroomType", "direct_message")
                            startActivity(intent)
                        }

                        binding.kebabMenuBtn.setOnClickListener {
                            openMenuDialog()
                        }

                        AppUtil().changeFriendsButtonState(binding.friendBtn, userModel)
                    } else {
                        //displays profile not available message if user blocked you
                        binding.kebabMenuBtn.visibility = View.GONE
                        binding.scrollLayout.visibility = View.GONE
                        binding.contentNotAvailableLayout.visibility = View.VISIBLE
                    }
                    binding.otherUserRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun openMenuDialog(){
        val menuBinding = DialogMenuBinding.inflate(layoutInflater)
        val menuDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(menuBinding.root))
            .setMargin(400, 0, 0, 0)
            .setGravity(Gravity.TOP)
            .setCancelable(true)
            .create()

        //Option 1
        menuBinding.option1.visibility = View.VISIBLE
        menuBinding.optiontitle1.text = "Search"
        menuBinding.optionIcon1.setImageResource(R.drawable.gear_icon)
        menuBinding.optiontitle1.setOnClickListener {
            val intent = Intent(this, Search::class.java)
            //TODO to implement search user
            startActivity(intent)
        }

        //Option 2
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optionIcon2.setImageResource(R.drawable.baseline_edit_24)
        showBlockUserDialog(menuBinding, menuDialog)

        //Option 3
        menuBinding.option3.visibility = View.VISIBLE
        menuBinding.optiontitle3.text = "Report"
        menuBinding.optionIcon3.setImageResource(R.drawable.baseline_logout_24)
        menuBinding.optiontitle3.setOnClickListener {
            menuDialog.dismiss()
            Handler().postDelayed({
                DialogUtil().openReportDialog(this, layoutInflater, "User", userModel.userID)
            }, 500)
        }

        menuDialog.show()
    }

    private fun showBlockUserDialog(menuBinding: DialogMenuBinding, menuDialog: DialogPlus) {
        if (!AppUtil().isIdOnList(myUserModel.blockList, userID)){
            menuBinding.optiontitle2.text = "Block"
            menuBinding.optiontitle2.setOnClickListener {
                //The user is not yet blocked
                val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                val warningDialog = DialogPlus.newDialog(this)
                    .setContentHolder(ViewHolder(warningBinding.root))
                    .setGravity(Gravity.CENTER)
                    .setMargin(50, 700, 50, 700)
                    .create()

                warningBinding.messageTV.text = "Do you want to block this user? (You will still see this account in communities and group chats)"
                warningBinding.titleTV.text = "Block User?"

                warningBinding.yesBtn.setOnClickListener {
                    //adds user to blockList
                    FirebaseUtil().currentUserDetails().update("blockList", FieldValue.arrayUnion(userID)).addOnSuccessListener {
                        onBackPressed()
                        Toast.makeText(this, "The user is now blocked", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                    }
                    warningDialog.dismiss()
                }
                warningBinding.NoBtn.setOnClickListener {
                    warningDialog.dismiss()
                }

                menuDialog.dismiss()
                Handler().postDelayed({
                    warningDialog.show()
                }, 500)
            }

        } else {
            menuBinding.optiontitle2.text = "Unblock"
            menuBinding.optiontitle2.setOnClickListener {
                //The user is blocked
                val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                val warningDialog = DialogPlus.newDialog(this)
                    .setContentHolder(ViewHolder(warningBinding.root))
                    .setGravity(Gravity.CENTER)
                    .setMargin(50, 700, 50, 700)
                    .create()

                warningBinding.messageTV.text = "Do you want to block this user? (You will still see this account in communities and group chats)?"
                warningBinding.titleTV.text = "Block User?"

                warningBinding.yesBtn.setOnClickListener {
                    //removes user from blockList
                    FirebaseUtil().currentUserDetails().update("blockList", FieldValue.arrayRemove(userID)).addOnSuccessListener {
                        Toast.makeText(this, "The user is now unblocked", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                    }
                    warningDialog.dismiss()
                }
                warningBinding.NoBtn.setOnClickListener {
                    warningDialog.dismiss()
                }

                menuDialog.dismiss()
                Handler().postDelayed({
                    warningDialog.show()
                }, 500)
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
        onRefresh()
    }
}