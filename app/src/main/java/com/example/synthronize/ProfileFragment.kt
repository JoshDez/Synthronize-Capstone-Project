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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.databinding.FragmentProfileBinding
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class ProfileFragment(private var mainBinding: ActivityMainBinding) : Fragment() {

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
            if (::context.isInitialized){
                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root)
                bindUserDetails()


                binding.postsRV.layoutManager = LinearLayoutManager(activity)
                binding.filesRV.layoutManager = LinearLayoutManager(activity)
                binding.likesRV.layoutManager = LinearLayoutManager(activity)

                //displays the first tab
                binding.postsRV.visibility = View.VISIBLE
                setupPostsRV()

                binding.postsBtn.setOnClickListener {
                    navigate("posts")
                }

                binding.filesBtn.setOnClickListener {
                    navigate("files")
                }

                binding.likesBtn.setOnClickListener {
                    navigate("likes")
                }
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
        val postsList:ArrayList<PostModel> = ArrayList()

        FirebaseUtil().retrieveAllCommunityCollection().get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    FirebaseUtil().retrieveAllCommunityCollection()
                        .document(document.id) // Access each document within the collection
                        .collection("feeds")
                        .whereEqualTo("ownerId", FirebaseUtil().currentUserUid())
                        .get()
                        .addOnSuccessListener { feedsSnapshot ->
                            var postsAdded = 0
                            feedsSnapshot.size()

                            for (post in feedsSnapshot.documents){
                                var postModel = post.toObject(PostModel::class.java)!!
                                postsList.add(postModel)
                                postsAdded += 1

                                //checks if all the user posts in the community are added
                                if (postsAdded == feedsSnapshot.size()){
                                    //sorts the list by timestamp
                                    postsList.sortByDescending {
                                        it.createdTimestamp
                                    }

                                    //deploys postsRV
                                    allFeedsAdapter = AllFeedsAdapter(context, postsList, false)
                                    binding.postsRV.adapter = allFeedsAdapter
                                }
                            }
                        }
                }
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
                getCommunitiesCount()
                getPostsCount()
                getFriendsCount()

                binding.editProfileBtn.setOnClickListener {
                    headToEditProfile()
                }

                mainBinding.kebabMenuBtn.visibility = View.VISIBLE
                mainBinding.kebabMenuBtn.setOnClickListener {
                    openMenuDialog()
                }
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
        menuBinding.optiontitle1.text = "Log out"
        menuBinding.optionIcon1.setImageResource(R.drawable.baseline_logout_24)
        menuBinding.optiontitle1.setOnClickListener {
            menuDialog.dismiss()
            Handler().postDelayed({
                openWarningDialog()
            }, 500)
        }

        //Option 2
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optiontitle2.text = "Edit Profile"
        menuBinding.optionIcon2.setImageResource(R.drawable.baseline_edit_24)
        menuBinding.optiontitle2.setOnClickListener {
            headToEditProfile()
            menuDialog.dismiss()
        }

        menuDialog.show()
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