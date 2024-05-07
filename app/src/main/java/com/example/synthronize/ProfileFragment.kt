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
import android.widget.Toast
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.databinding.FragmentProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateUtil
import com.example.synthronize.utils.FirebaseUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class ProfileFragment(private var mainBinding: ActivityMainBinding) : Fragment() {
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
            if (::context.isInitialized)
                bindUserDetails()
        }
    }

    private fun bindUserDetails() {

        FirebaseUtil().targetUserDetails(FirebaseUtil().currentUserUid()).get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()){
                val userModel = it.result.toObject(UserModel::class.java)!!

                userId = userModel.userID

                binding.userNameTV.text = "@${userModel.username}"
                binding.userDisplayNameTV.text = userModel.fullName
                binding.userDescriptionTV.text = userModel.description
                if (userModel.birthday.isNotEmpty()){
                    binding.birthdayLayout.visibility = View.VISIBLE
                    binding.birthdayTV.text = DateUtil().formatBirthDate(userModel.birthday)
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