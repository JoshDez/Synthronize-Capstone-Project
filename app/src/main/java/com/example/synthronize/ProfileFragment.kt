package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.adapters.ProfileFilesAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.databinding.FragmentProfileBinding
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

class ProfileFragment(private var mainBinding: ActivityMainBinding) : Fragment(), OnRefreshListener, OnNetworkRetryListener, OnItemClickListener {

    private lateinit var allFeedsAdapter: AllFeedsAdapter
    private lateinit var profileFilesAdapter: ProfileFilesAdapter
    private lateinit var binding: FragmentProfileBinding
    private lateinit var context: Context
    private lateinit var userId: String
    //For OnItemClickListener
    private var isFriendsList = false

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
                bindUserDetails()
                binding.profileRefreshLayout.setOnRefreshListener(this)
            }
        }
    }

    private fun navigate(tab: String, toRefresh:Boolean = false) {
        val unselectedColor = ContextCompat.getColor(context, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(context, R.color.light_teal)
        binding.postsBtn.setTextColor(unselectedColor)
        binding.filesBtn.setTextColor(unselectedColor)
        binding.postsRV.visibility = View.GONE
        binding.filesRV.visibility = View.GONE

        if (tab == "posts"){
            binding.postsBtn.setTextColor(selectedColor)
            binding.postsRV.visibility = View.VISIBLE
            if (toRefresh)
                setupPostsRV()
        }else if (tab == "files"){
            binding.filesBtn.setTextColor(selectedColor)
            binding.filesRV.visibility = View.VISIBLE
            if (toRefresh)
                setupFilesRV()
        }
    }

    private fun setupPostsRV(){
        ProfileUtil().getUserPosts(context, userId){
            allFeedsAdapter = it
            binding.postsRV.layoutManager = LinearLayoutManager(context)
            binding.postsRV.adapter = allFeedsAdapter
        }
    }

    private fun setupFilesRV() {
        ProfileUtil().getUserFiles(context, userId){
            profileFilesAdapter = it
            binding.filesRV.layoutManager = LinearLayoutManager(context)
            binding.filesRV.adapter = profileFilesAdapter
        }
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
                AppUtil().showMoreAndLessWords(userModel.description, binding.userDescriptionTV, 50)
                if (userModel.birthday.isNotEmpty()){
                    binding.birthdayLayout.visibility = View.VISIBLE
                    binding.birthdayTV.text = DateAndTimeUtil().formatDateFromMMDDYYYY(userModel.birthday)
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
                ProfileUtil().getFilesCount(userId) {filesCount ->
                    binding.filesCountTV.text = filesCount.toString()
                }

                //prepares recycler views
                binding.postsRV.visibility = View.INVISIBLE
                binding.filesRV.visibility = View.INVISIBLE
                setupPostsRV()
                setupFilesRV()

                //displays the first tab
                navigate("posts")

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
                binding.friendsContainer.setOnClickListener {
                    isFriendsList = true
                    ProfileUtil().openUserFriendsListDialog(context, userModel.userID, layoutInflater, this)
                }
                binding.communitiesContainer.setOnClickListener {
                    isFriendsList = false
                    ProfileUtil().openCommunityListDialog(context, userModel.userID, layoutInflater, this)
                }
                binding.postsContainer.setOnClickListener {
                    ProfileUtil().openPostsDescriptionDialog(context, layoutInflater)
                }
                binding.filesContainer.setOnClickListener {
                    ProfileUtil().openFilesDescriptionDialog(context, layoutInflater)
                }
                mainBinding.kebabMenuBtn.visibility = View.VISIBLE
                mainBinding.kebabMenuBtn.setOnClickListener {
                    openMenuDialog()
                }

                setupPostsRV()
                setupFilesRV()

                binding.profileRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun openMenuDialog(){
        val menuBinding = DialogMenuBinding.inflate(layoutInflater)
        val menuDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(menuBinding.root))
            .setMargin(400, 0, 0, 0)
            .setBackgroundColorResId(R.color.transparent)
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
        menuBinding.optiontitle3.text = "Deactivate Account"
        menuBinding.optionIcon3.setImageResource(R.drawable.block_user_icon)
        menuBinding.optiontitle3.setOnClickListener {
            menuDialog.dismiss()
            Handler().postDelayed({
                deactivateWarningDialog()
            }, 500)
        }

        //Option 4
        menuBinding.option4.visibility = View.VISIBLE
        menuBinding.optiontitle4.text = "Log out"
        menuBinding.optionIcon4.setImageResource(R.drawable.baseline_logout_24)
        menuBinding.optiontitle4.setOnClickListener {
            menuDialog.dismiss()
            Handler().postDelayed({
                logoutWarningDialog()
            }, 500)
        }

        menuDialog.show()
    }

    private fun headToSettings() {
        val intent = Intent(requireContext(), AppSettings::class.java)
        startActivity(intent)
    }

    private fun logoutWarningDialog(){
        val warningDialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val warningDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(warningDialogBinding.root))
            .setBackgroundColorResId(R.color.transparent)
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

    private fun deactivateWarningDialog(){
        val warningDialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val warningDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(warningDialogBinding.root))
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .setCancelable(true)
            .create()

        warningDialogBinding.titleTV.text = "Deactivate Account"
        warningDialogBinding.messageTV.text = "Do you want to deactivate your account? (You can reactivate your account by signing in)"

        warningDialogBinding.yesBtn.setOnClickListener {
            val updates = mapOf(
                "userAccess.Disabled" to "",
                "userAccess.Enabled" to FieldValue.delete()
            )
            FirebaseUtil().currentUserDetails().update(updates).addOnSuccessListener {
                //logout user
                FirebaseUtil().logoutUser(context)
            }
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

    override fun onItemClick(id: String, isChecked: Boolean) {
        if (isFriendsList){
            val intent = Intent(context, OtherUserProfile::class.java)
            intent.putExtra("userID", id)
            startActivity(intent)
        } else {
            FirebaseUtil().retrieveCommunityDocument(id).get().addOnCompleteListener {
                if (it.result.exists()){
                    val communityModel = it.result.toObject(CommunityModel::class.java)!!
                    DialogUtil().openCommunityPreviewDialog(context, layoutInflater, communityModel)
                }
            }
        }
    }
}