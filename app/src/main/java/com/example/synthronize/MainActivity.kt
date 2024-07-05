package com.example.synthronize

import UserLastSeenUpdater
import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root)
        onStartFragment()

        //BOTTOM NAVIGATION BUTTONS
        binding.communitiesBtn.setOnClickListener {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.communitiesBtn.id)
            replaceFragment(CommunitySelectionFragment(binding, supportFragmentManager), "SELECTION_FRAGMENT")
        }
        binding.exploreBtn.setOnClickListener {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.exploreBtn.id)
            replaceFragment(ExploreFragment(binding), "EXPLORE_FRAGMENT")
        }

        binding.updatesBtn.setOnClickListener {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.updatesBtn.id)
            replaceFragment(UpdatesFragment(binding), "UPDATES_FRAGMENT")
        }

        binding.profileBtn.setOnClickListener {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.profileBtn.id)
            replaceFragment(ProfileFragment(binding), "PROFILE_FRAGMENT")
        }
        binding.chatBtn.setOnClickListener {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.chatBtn.id)
            replaceFragment(ChatFragment(binding), "CHAT_FRAGMENT")
        }


    }
    //Function that checks if the intent request for a specific fragment
    private fun onStartFragment(){
        val fragmentRequest = intent.getStringExtra("fragment").toString()
        val communityId = intent.getStringExtra("communityId").toString()
        if (fragmentRequest == "profile"){
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.profileBtn.id)
            replaceFragment(ProfileFragment(binding), "PROFILE_FRAGMENT")
        } else if(fragmentRequest == "chat") {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.chatBtn.id)
            replaceFragment(ChatFragment(binding), "CHAT_FRAGMENT")
        } else if (fragmentRequest == "community") {
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.communitiesBtn.id)
            replaceFragment(CommunityFragment(binding, communityId), "COMMUNITY_FRAGMENT")
        } else {
            //default group selection fragment
            AppUtil().resetMainToolbar(binding)
            selectNavigation(binding.communitiesBtn.id)
            replaceFragment(CommunitySelectionFragment(binding, supportFragmentManager), "SELECTION_FRAGMENT")
        }
    }
    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(tag)

        if (existingFragment == null || existingFragment.javaClass != fragment.javaClass) {
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
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.groups_toolbar_menu, menu)
        return true
    }
}