package com.example.synthronize

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogSelectRoleBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.AccTypeRequestModel
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.google.firebase.messaging.FirebaseMessaging
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class MainActivity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private var currentFragment = ""
    private var isFirstTimeSignInWithGoogle = false
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("AppPreferences", MODE_PRIVATE)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppUtil().logoutIfAccDisabled(this)
        onStartFragment()
        setupFCM()
        showUpdatesNotificationAlert()
        showChatNotificationAlert()

        //BOTTOM NAVIGATION BUTTONS
        binding.communitiesBtn.setOnClickListener {
            selectFragment("community_selection")
        }
        binding.exploreBtn.setOnClickListener {
            selectFragment("explore")
        }
        binding.updatesBtn.setOnClickListener {
            selectFragment("updates")
            binding.updatesBtn.foreground = null
        }
        binding.profileBtn.setOnClickListener {
            selectFragment("profile")
        }
        binding.chatBtn.setOnClickListener {
            selectFragment("chat")
            binding.chatBtn.foreground = null
        }

        isFirstTimeSignInWithGoogle = intent.getBooleanExtra("firstTimeSignIn", false)
        if (isFirstTimeSignInWithGoogle){
            openSelectRoleDialog()
        }

    }

    private fun openSelectRoleDialog() {
        val selectRoleBinding = DialogSelectRoleBinding.inflate(layoutInflater)
        val selectRoleDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(selectRoleBinding.root))
            .create()

        var userType = ""

        selectRoleBinding.studentRB.setOnClickListener {
            if (selectRoleBinding.studentRB.isChecked){
                selectRoleBinding.professorRB.isChecked = false
                selectRoleBinding.appAdminRB.isChecked = false
                userType = "Student"
                selectRoleBinding.warningMessageTV.visibility = View.INVISIBLE
            }
        }
        selectRoleBinding.professorRB.setOnClickListener {
            if (selectRoleBinding.professorRB.isChecked){
                selectRoleBinding.studentRB.isChecked = false
                selectRoleBinding.appAdminRB.isChecked = false
                userType = "Professor"
                selectRoleBinding.warningMessageTV.visibility = View.VISIBLE
            }
        }
        selectRoleBinding.appAdminRB.setOnClickListener {
            if (selectRoleBinding.appAdminRB.isChecked){
                selectRoleBinding.professorRB.isChecked = false
                selectRoleBinding.studentRB.isChecked = false
                userType = "AppAdmin"
                selectRoleBinding.warningMessageTV.visibility = View.VISIBLE
            }
        }
        selectRoleBinding.confirmBtn.setOnClickListener {
            if (userType.isNotEmpty()){
                if (userType != "Student"){
                    val tempModel = AccTypeRequestModel()
                    FirebaseUtil().retrieveAllUserTypeRequests().add(tempModel).addOnSuccessListener {acc ->
                        val accTypeModel = AccTypeRequestModel(
                            requestId = acc.id,
                            userId = FirebaseUtil().currentUserUid(),
                            reqAccType = userType
                        )
                        FirebaseUtil().retrieveAllUserTypeRequests().document(accTypeModel.requestId).set(accTypeModel)
                        selectRoleDialog.dismiss()
                    }.addOnFailureListener {
                        Toast.makeText(this, "An error occurred, please try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    selectRoleDialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Please select your role", Toast.LENGTH_SHORT).show()
            }
        }

        selectRoleDialog.show()

    }

    //Function that enables or disables push notifications
    private fun setupFCM() {
        // Load the saved preference
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)

        if (isNotificationsEnabled){
            //enables notification
            FirebaseUtil().getFCMToken()
        } else {
            //disables notification
            FirebaseUtil().removeFCMToken()
        }
    }

    //Function that checks if the intent request for a specific fragment
    private fun onStartFragment(){
        var fragmentRequest = intent.getStringExtra("fragment").toString()
        val communityId = intent.getStringExtra("communityId").toString()

        when (fragmentRequest) {
            "profile" -> {
                selectFragment("profile")
            }
            "chat" -> {
                selectFragment("chat")
            }
            "community" -> {
                selectFragment("community", communityId)
            }
            else -> {
                //default group selection fragment
                selectFragment("community_selection")
            }
        }
    }

    private fun selectFragment(fragmentRequest:String = "", communityId:String = ""){

        when (fragmentRequest) {
            "explore" -> {
                currentFragment = fragmentRequest
                selectNavigation(binding.exploreBtn.id)
                replaceFragment(ExploreFragment(binding), currentFragment)

            }
            "profile" -> {
                currentFragment = fragmentRequest
                selectNavigation(binding.profileBtn.id)
                replaceFragment(ProfileFragment(binding), currentFragment)

            }
            "chat" -> {
                currentFragment = fragmentRequest
                selectNavigation(binding.chatBtn.id)
                replaceFragment(ChatFragment(binding), currentFragment)

            }
            "updates" -> {
                currentFragment = fragmentRequest
                selectNavigation(binding.updatesBtn.id)
                replaceFragment(UpdatesFragment(binding), currentFragment)
            }
            "community_selection" -> {
                currentFragment = fragmentRequest
                selectNavigation(binding.communitiesBtn.id)
                replaceFragment(CommunitySelectionFragment(binding, this), currentFragment)
            }
            "community" -> {
                if (communityId.isNotEmpty() && communityId != "null"){
                    currentFragment = fragmentRequest
                    selectNavigation(binding.communitiesBtn.id)
                    replaceFragment(CommunityFragment(binding, communityId), currentFragment)
                }
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(tag)

        if (existingFragment == null || existingFragment.javaClass != fragment.javaClass) {
            //replace new fragment
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(binding.mainFrameLayout.id, fragment, tag)
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    //Changes navigation's buttons states
    private fun selectNavigation(btnID:Int) {
        binding.exploreBtn.setBackgroundResource(R.drawable.explore_not_selected)
        binding.updatesBtn.setBackgroundResource(R.drawable.notifications_not_selected)
        binding.communitiesBtn.setBackgroundResource(R.drawable.community_not_selected)
        binding.profileBtn.setBackgroundResource(R.drawable.profile_not_selected)
        binding.chatBtn.setBackgroundResource(R.drawable.chat_not_selected)
        when (btnID) {
            binding.exploreBtn.id -> {
                binding.exploreBtn.setBackgroundResource(R.drawable.explore_selected)
            }
            binding.updatesBtn.id -> {
                binding.updatesBtn.setBackgroundResource(R.drawable.notifications_selected)
            }
            binding.communitiesBtn.id -> {
                binding.communitiesBtn.setBackgroundResource(R.drawable.community_selected)
            }
            binding.profileBtn.id -> {
                binding.profileBtn.setBackgroundResource(R.drawable.profile_selected)
            }
            binding.chatBtn.id -> {
                binding.chatBtn.setBackgroundResource(R.drawable.chat_selected)

            }
        }
    }


    private fun showUpdatesNotificationAlert(){
        FirebaseUtil().currentUserDetails().get().addOnCompleteListener {
            if (it.result.exists()){
                val userModel = it.result.toObject(UserModel::class.java)!!

                if (userModel.communityInvitations.isNotEmpty()){

                    binding.updatesBtn.foreground = ContextCompat.getDrawable(this, R.drawable.red_dot)
                } else if (userModel.friendRequests.isNotEmpty()){

                    binding.updatesBtn.foreground = ContextCompat.getDrawable(this, R.drawable.red_dot)
                } else {
                    for (key in userModel.notifications.keys){
                        val tempList = userModel.notifications.getValue(key)
                        try {
                            if (tempList[6] == "not_seen"){

                                binding.updatesBtn.foreground = ContextCompat.getDrawable(this, R.drawable.red_dot)
                                break
                            }
                        } catch (e:Exception){}
                    }
                }
            }
        }
    }

    private fun showChatNotificationAlert(){
        FirebaseUtil().retrieveAllChatRoomReferences()
            .whereArrayContains("userIdList", FirebaseUtil().currentUserUid()).get().addOnSuccessListener {
                for (document in it.documents){
                    val chatroomModel = document.toObject(ChatroomModel::class.java)!!
                    if (!AppUtil().isIdOnList(chatroomModel.usersSeen, FirebaseUtil().currentUserUid()) && chatroomModel.lastMessage.isNotEmpty()){
                        binding.chatBtn.foreground = ContextCompat.getDrawable(this, R.drawable.red_dot)
                        break
                    }
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.groups_toolbar_menu, menu)
        return true
    }

    override fun onBackPressed() {
        if (currentFragment == "community_selection"){
            super.onBackPressed()
        } else {
            selectFragment("community_selection")
        }
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        selectFragment("community", id)
    }
}