package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract.Profile
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.databinding.FragmentProfileBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.example.synthronize.utils.ProfileUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class ProfileFragment(private var mainBinding: ActivityMainBinding) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var allFeedsAdapter: AllFeedsAdapter
    private lateinit var binding: FragmentProfileBinding
    private lateinit var context: Context
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "PROFILE"

        //checks if the fragment is already added to the activity
        if (isAdded){
            context = requireContext()
            userId = FirebaseUtil().currentUserUid()
            if (::context.isInitialized){
                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)

                //TODO add loading screen
                bindUserDetails()

                binding.postsRV.layoutManager = LinearLayoutManager(activity)
                binding.filesRV.layoutManager = LinearLayoutManager(activity)
                binding.likesRV.layoutManager = LinearLayoutManager(activity)
                binding.profileRefreshLayout.setOnRefreshListener(this)

            }

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

    private fun setupPostsRV(){
        ProfileUtil().getUserPosts(context, userId){
            allFeedsAdapter = it
            binding.postsRV.layoutManager = LinearLayoutManager(context)
            binding.postsRV.adapter = allFeedsAdapter
        }
    }

    private fun setupLikesRV() {
        //TODO Not Yet Implemented
    }

    private fun setupFilesRV() {
        //TODO Not Yet Implemented
    }

    private fun bindUserDetails() {

        AppUtil().resetMainToolbar(mainBinding)

        binding.profileRefreshLayout.isRefreshing = true

        FirebaseUtil().targetUserDetails(FirebaseUtil().currentUserUid()).get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()){
                val userModel = it.result.toObject(UserModel::class.java)!!

                userId = userModel.userID

                binding.userNameTV.text = "@${userModel.username}"
                binding.userDisplayNameTV.text = userModel.fullName
                binding.userDescriptionTV.text = userModel.description
                if (userModel.birthday.isNotEmpty()){
                    binding.birthdayLayout.visibility = View.VISIBLE
                    binding.birthdayTV.text = DateAndTimeUtil().formatBirthDate(userModel.birthday)
                }

                //binds userProfilePic
                AppUtil().setUserProfilePic(context, userId, binding.userProfileCIV)
                AppUtil().setUserCoverPic(context, userId, binding.userCoverIV)

                //bind counts
                ProfileUtil().getCommunitiesCount(userId) { communityCount ->
                    binding.communitiesCountTV.text = communityCount.toString()
                }
                ProfileUtil().getPostsCount(userId) { postsCount ->
                    binding.postsCountTV.text = postsCount.toString()
                }
                ProfileUtil().getFriendsCount(userId) { friendsCount ->
                    binding.friendsCountTV.text = friendsCount.toString()
                }
                //TODO count for files


                //displays the first tab
                binding.postsRV.visibility = View.VISIBLE
                setupPostsRV()


                //set on click listeners
                binding.editProfileBtn.setOnClickListener {
                    headToEditProfile()
                }
                binding.postsBtn.setOnClickListener {
                    navigate("posts")
                }
                binding.filesBtn.setOnClickListener {
                    navigate("files")
                }
                binding.likesBtn.setOnClickListener {
                    navigate("likes")
                }

                mainBinding.kebabMenuBtn.visibility = View.VISIBLE
                mainBinding.kebabMenuBtn.setOnClickListener {
                    openMenuDialog()
                }

                binding.profileRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun openMenuDialog(){
        val menuBinding = DialogMenuBinding.inflate(layoutInflater)
        val menuDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(menuBinding.root))
            .setMargin(400, 0, 0, 0)
            .setGravity(Gravity.TOP)
            .setCancelable(true)
            .create()

        //Option 1
        menuBinding.option1.visibility = View.VISIBLE
        menuBinding.optiontitle1.text = "Settings"
        menuBinding.optionIcon1.setImageResource(R.drawable.gear_icon)
        menuBinding.optiontitle1.setOnClickListener {
            headToSettings()
        }



        //Option 2
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optiontitle2.text = "Edit Profile"
        menuBinding.optionIcon2.setImageResource(R.drawable.baseline_edit_24)
        menuBinding.optiontitle2.setOnClickListener {
            headToEditProfile()
            menuDialog.dismiss()
        }

        //Option 3
        menuBinding.option3.visibility = View.VISIBLE
        menuBinding.optiontitle3.text = "Log out"
        menuBinding.optionIcon3.setImageResource(R.drawable.baseline_logout_24)
        menuBinding.optiontitle3.setOnClickListener {
            menuDialog.dismiss()
            Handler().postDelayed({
                openWarningDialog()
            }, 500)
        }

        menuDialog.show()
    }

    private fun headToSettings() {
        val intent = Intent(requireContext(), AppSettings::class.java)
        startActivity(intent)
    }

    private fun openWarningDialog(){
        val warningDialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val warningDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(warningDialogBinding.root))
            .setMargin(50, 800, 50, 800)
            .setGravity(Gravity.CENTER)
            .setCancelable(true)
            .create()

        warningDialogBinding.titleTV.text = "Log out"
        warningDialogBinding.messageTV.text = "Do you want to logout?"

        warningDialogBinding.yesBtn.setOnClickListener {
            FirebaseUtil().logoutUser(context)
        }
        warningDialogBinding.NoBtn.setOnClickListener {
            warningDialog.dismiss()
        }

        warningDialog.show()
    }

    private fun headToEditProfile(){
        val intent = Intent(requireContext(), EditProfile::class.java)
        intent.putExtra("userID", userId)
        startActivity(intent)
    }

    override fun onRefresh() {
        binding.profileRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            bindUserDetails()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}