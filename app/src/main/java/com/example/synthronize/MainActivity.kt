package com.example.synthronize

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private var currentFragment = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onStartFragment()
        getFCMToken()

        //BOTTOM NAVIGATION BUTTONS
        binding.communitiesBtn.setOnClickListener {
            selectFragment("community_selection")
        }
        binding.exploreBtn.setOnClickListener {
            selectFragment("explore")
        }
        binding.updatesBtn.setOnClickListener {
            selectFragment("updates")
        }
        binding.profileBtn.setOnClickListener {
            selectFragment("profile")
        }
        binding.chatBtn.setOnClickListener {
            selectFragment("chat")
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

    private fun getFCMToken() {
        // Get Token For Receiving Notifications
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseUtil().currentUserDetails().update("fcmToken", token)
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