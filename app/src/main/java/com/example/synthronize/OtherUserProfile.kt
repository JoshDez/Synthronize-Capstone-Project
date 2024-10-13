package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.adapters.ProfileFilesAdapter
import com.example.synthronize.databinding.ActivityOtherUserProfileBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.example.synthronize.utils.ProfileUtil
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class OtherUserProfile : AppCompatActivity(), OnNetworkRetryListener, OnRefreshListener, OnItemClickListener {
    private lateinit var binding:ActivityOtherUserProfileBinding
    private lateinit var userModel: UserModel
    private lateinit var myUserModel: UserModel
    private lateinit var allFeedsAdapter:AllFeedsAdapter
    private lateinit var profileFilesAdapter: ProfileFilesAdapter
    private var userID = ""
    //For OnItemClickListener
    private var isFriendsList = false
    private var currentTab = ""


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

        binding.otherUserRefreshLayout.setOnRefreshListener(this)

        //main set on click listeners
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }


    private fun navigate(tab: String, toRefresh:Boolean = false) {
        val unselectedColor = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(this, R.color.light_teal)
        binding.postsBtn.setTextColor(unselectedColor)
        binding.filesBtn.setTextColor(unselectedColor)
        binding.postsRV.visibility = View.GONE
        binding.filesRV.visibility = View.GONE

        if (tab == "posts"){
            binding.postsBtn.setTextColor(selectedColor)
            binding.postsRV.visibility = View.VISIBLE
            currentTab = "posts"
            if (toRefresh)
                setupPostsRV()
        }else if (tab == "files"){
            binding.filesBtn.setTextColor(selectedColor)
            binding.filesRV.visibility = View.VISIBLE
            currentTab = "files"
            if (toRefresh)
                setupFilesRV()
        }
    }

    private fun setupFilesRV(searchQuery:String = "") {
        ProfileUtil().getUserFiles(this, userID, searchQuery, true){
            profileFilesAdapter = it
            binding.filesRV.layoutManager = LinearLayoutManager(this)
            binding.filesRV.adapter = profileFilesAdapter
        }
    }

    private fun setupPostsRV(searchQuery:String = "") {
        ProfileUtil().getUserPosts(this, userID, searchQuery, true){
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

                    if (!AppUtil().isIdOnList(userModel.blockList, FirebaseUtil().currentUserUid()) && !userModel.userAccess.containsKey("Disabled")){
                        //displays the profile
                        AppUtil().showMoreAndLessWords(userModel.description, binding.userDescriptionTV, 50)
                        binding.userNameTV.text = "@${userModel.username}"
                        binding.userDisplayNameTV.text = userModel.fullName

                        if (userModel.birthday.isNotEmpty()){
                            binding.birthdayLayout.visibility = View.VISIBLE
                            binding.birthdayTV.text =  DateAndTimeUtil().formatDateFromMMDDYYYY(userModel.birthday)
                        }
                        if (userModel.userType.isNotEmpty()){
                            binding.userRoleLayout.visibility = View.VISIBLE
                            binding.userRoleTV.text = userModel.userType
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
                        ProfileUtil().getFilesCount(userID) {filesCount ->
                            binding.filesCountTV.text = filesCount.toString()
                        }


                        //displays the first tab
                        navigate("posts")


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
                        binding.friendsContainer.setOnClickListener {
                            isFriendsList = true
                            ProfileUtil().openUserFriendsListDialog(this, userModel.userID, layoutInflater, this)
                        }
                        binding.communitiesContainer.setOnClickListener {
                            isFriendsList = false
                            ProfileUtil().openCommunityListDialog(this, userModel.userID, layoutInflater, this)
                        }
                        binding.postsContainer.setOnClickListener {
                            ProfileUtil().openPostsDescriptionDialog(this, layoutInflater)
                        }
                        binding.filesContainer.setOnClickListener {
                            ProfileUtil().openFilesDescriptionDialog(this, layoutInflater)
                        }

                        binding.cancelBtn.setOnClickListener {
                            binding.searchEdtTxt.setText("")
                            binding.searchContainerLL.visibility = View.GONE
                        }
                        binding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                val searchQuery = binding.searchEdtTxt.text.toString()
                                if (currentTab == "files"){
                                    setupFilesRV(searchQuery)
                                } else if (currentTab == "posts"){
                                    setupPostsRV(searchQuery)
                                }
                            }
                        })

                        setupPostsRV()
                        setupFilesRV()

                        AppUtil().changeFriendsButtonState(this, binding.friendBtn, userModel)
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
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.TOP)
            .setCancelable(true)
            .create()

        //Option 1
        menuBinding.option1.visibility = View.VISIBLE
        menuBinding.optiontitle1.text = "Search"
        menuBinding.optionIcon1.setImageResource(R.drawable.search_icon)
        menuBinding.optiontitle1.setOnClickListener {
            menuDialog.dismiss()
            binding.searchContainerLL.visibility = View.VISIBLE
        }

        //Option 2
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optionIcon2.setImageResource(R.drawable.block_user_icon)
        showBlockUserDialog(menuBinding, menuDialog)

        //Option 3
        menuBinding.option3.visibility = View.VISIBLE
        menuBinding.optiontitle3.text = "Report"
        menuBinding.optionIcon3.setImageResource(R.drawable.baseline_edit_24)
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
                    .setBackgroundColorResId(R.color.transparent)
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
                    .setBackgroundColorResId(R.color.transparent)
                    .create()

                warningBinding.messageTV.text = "Do you want to unblock this user?"
                warningBinding.titleTV.text = "Unblock User?"

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

    override fun onItemClick(id: String, isChecked: Boolean) {
        if (isFriendsList){
            val intent = Intent(this, OtherUserProfile::class.java)
            intent.putExtra("userID", id)
            startActivity(intent)
        } else {
            FirebaseUtil().retrieveCommunityDocument(id).get().addOnCompleteListener {
                if (it.result.exists()){
                    val communityModel = it.result.toObject(CommunityModel::class.java)!!
                    DialogUtil().openCommunityPreviewDialog(this, layoutInflater, communityModel)
                }
            }
        }
    }
}