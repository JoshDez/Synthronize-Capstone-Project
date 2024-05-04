// GroupFragment.kt

package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil

class CommunityFragment(private val mainBinding: ActivityMainBinding, private val communityId:String) : Fragment() {

    private lateinit var binding: FragmentCommunityBinding
    private lateinit var communityModel: CommunityModel
    private lateinit var context: Context
    private var isUserAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //retrieve context
            context = requireContext()
            //retrieve community model
            FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                if (it.exists()){
                    communityModel = it.toObject(CommunityModel::class.java)!!
                    //content
                    if (::context.isInitialized){
                        //bind community
                        assignUserRole()
                        bindCommunityDetails()
                        bindButtons()
                        //Set Feeds fragment as default fragment
                        selectNavigation("feeds")
                        replaceFragment(FeedsFragment(binding, communityId))
                    }
                }
            }
        }
    }


    private fun bindCommunityDetails(){
        //set name
        mainBinding.toolbarTitleTV.text = communityModel.communityName
        //set community profile photo
        AppUtil().setCommunityProfilePic(context, communityModel.communityId, mainBinding.toolbarImageCIV)
    }

    private fun assignUserRole() {
        for (userId in communityModel.communityAdmin){
            if (FirebaseUtil().currentUserUid() == userId){
                isUserAdmin = true
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.communityFrameLayout.id, fragment)
        fragmentTransaction.commit()
    }

    private fun bindButtons(){
        mainBinding.backBtn.visibility = View.VISIBLE
        mainBinding.communitySettingsBtn.visibility = View.VISIBLE

        //bind buttons
        binding.feedsTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(FeedsFragment(binding, communityId))
            selectNavigation("feeds")
        }
        binding.eventsTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(EventsFragment(binding, communityId))
            selectNavigation("events")
        }
        binding.forumsTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(ForumsFragment(binding, communityId))
            selectNavigation("forums")
        }
        binding.marketTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(MarketFragment(binding, communityId))
            selectNavigation("market")
        }
        binding.filesTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(FilesFragment(binding, communityId))
            selectNavigation("files")
        }
        binding.generalChatsBtn.setOnClickListener {
            val intent = Intent(context, Chatroom::class.java)
            intent.putExtra("chatroomName", "General Chat")
            intent.putExtra("communityId", communityId)
            intent.putExtra("chatroomType", "community_chat")
            startActivity(intent)
        }

        //bind buttons from main binding
        mainBinding.backBtn.setOnClickListener {
            AppUtil().headToMainActivity(context)
        }
        mainBinding.communitySettingsBtn.setOnClickListener {
            val intent = Intent(context, CommunitySettings::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            intent.putExtra("isUserAdmin", isUserAdmin)
            startActivity(intent)
        }

        //changes settings button appearance if user is admin

        if (isUserAdmin){
            mainBinding.communitySettingsBtn.setBackgroundResource(R.drawable.admin_settings)
        } else {
            mainBinding.communitySettingsBtn.setBackgroundResource(R.drawable.gear_icon)
        }
    }
    private fun selectNavigation(fragment:String) {
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.less_saturated_light_purple)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.light_purple)
        binding.feedsTextView.setTextColor(unselectedColor)
        binding.eventsTextView.setTextColor(unselectedColor)
        binding.forumsTextView.setTextColor(unselectedColor)
        binding.marketTextView.setTextColor(unselectedColor)
        binding.filesTextView.setTextColor(unselectedColor)
        when (fragment) {
            "feeds" -> {
                binding.feedsTextView.setTextColor(selectedColor)
            }
            "events" -> {
                binding.eventsTextView.setTextColor(selectedColor)
            }
            "forums" -> {
                binding.forumsTextView.setTextColor(selectedColor)
            }
            "market" -> {
                binding.marketTextView.setTextColor(selectedColor)
            }
            "files" -> {
                binding.filesTextView.setTextColor(selectedColor)

            }
        }
    }
}
