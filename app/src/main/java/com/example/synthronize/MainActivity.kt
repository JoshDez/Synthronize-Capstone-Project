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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onStartFragment()

        //BOTTOM NAVIGATION BUTTONS
        binding.communitiesBtn.setOnClickListener {
            selectNavigation(binding.communitiesBtn.id)
            replaceFragment(CommunitySelectionFragment(binding, supportFragmentManager))
            resetMainToolbar()
        }
        binding.exploreBtn.setOnClickListener {
            resetMainToolbar()
            selectNavigation(binding.exploreBtn.id)
            replaceFragment(ExploreFragment(binding))
        }

        binding.updatesBtn.setOnClickListener {
            resetMainToolbar()
            selectNavigation(binding.updatesBtn.id)
            replaceFragment(UpdatesFragment(binding))
        }

        binding.profileBtn.setOnClickListener {
            resetMainToolbar()
            selectNavigation(binding.profileBtn.id)
            replaceFragment(ProfileFragment(binding))
        }
        binding.chatBtn.setOnClickListener {
            resetMainToolbar()
            selectNavigation(binding.chatBtn.id)
            replaceFragment(ChatFragment(binding))
        }


    }
    //Function that checks if the intent request for a specific fragment
    private fun onStartFragment(){
        val fragmentRequest = intent.getStringExtra("fragment").toString()
        val communityId = intent.getStringExtra("communityId").toString()
        if (fragmentRequest == "profile"){
            resetMainToolbar()
            selectNavigation(binding.profileBtn.id)
            replaceFragment(ProfileFragment(binding))
        } else if(fragmentRequest == "chat") {
            resetMainToolbar()
            selectNavigation(binding.chatBtn.id)
            replaceFragment(ChatFragment(binding))
        } else if (fragmentRequest == "community") {
            resetMainToolbar()
            selectNavigation(binding.communitiesBtn.id)
            replaceFragment(CommunityFragment(binding, communityId))
        } else {
            //default group selection fragment
            resetMainToolbar()
            selectNavigation(binding.communitiesBtn.id)
            replaceFragment(CommunitySelectionFragment(binding, supportFragmentManager))
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.mainFrameLayout.id, fragment)
        fragmentTransaction.commit()
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
    private fun resetMainToolbar(){
        binding.backBtn.visibility = View.GONE
        binding.kebabMenuBtn.visibility = View.GONE
        binding.hamburgerMenuBtn.visibility = View.GONE
        binding.searchBtn.visibility = View.GONE
        binding.toolbarImageCIV.setImageResource(R.drawable.header_logo)

        //resetting main toolbar setOnClickListeners
        binding.searchBtn.setOnClickListener(null)
        binding.kebabMenuBtn.setOnClickListener(null)
        binding.hamburgerMenuBtn.setOnClickListener(null)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.groups_toolbar_menu, menu)
        return true
    }
}