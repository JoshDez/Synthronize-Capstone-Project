// GroupFragment.kt

package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.util.Log
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

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


        if (isAdded && isVisible && !isDetached && !isRemoving){
            //retrieve context
            context = requireContext()

            if (::context.isInitialized){

                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root)

                //retrieve community model
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                    if (it.exists()){
                        communityModel = it.toObject(CommunityModel::class.java)!!
                        //content
                        if (::context.isInitialized){
                            //reset main toolbar
                            AppUtil().resetMainToolbar(mainBinding)
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
        // Check if the fragment is added to its activity
        if (isAdded && isVisible) {
            val fragmentTransaction = childFragmentManager.beginTransaction()
            fragmentTransaction.replace(binding.communityFrameLayout.id, fragment)
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    private fun bindButtons(){
        mainBinding.backBtn.visibility = View.VISIBLE
        mainBinding.hamburgerMenuBtn.visibility = View.VISIBLE

        //bind buttons
        binding.feedsBtn.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(FeedsFragment(binding, communityId))
            selectNavigation("feeds")
        }
        binding.eventsBtn.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(EventsFragment(binding, communityId))
            selectNavigation("events")
        }
        binding.forumsBtn.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(ForumsFragment(binding, communityId))
            selectNavigation("forums")
        }
        binding.marketBtn.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(MarketFragment(binding, communityId))
            selectNavigation("market")
        }
        binding.activitiesBtn.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(ActivitiesFragment(binding, communityId))
            selectNavigation("activities")
        }

        //bind buttons from main binding
        mainBinding.backBtn.setOnClickListener {
            AppUtil().headToMainActivity(context, hasAnimation = false)
        }
        mainBinding.hamburgerMenuBtn.setOnClickListener {
            openCommunityMenuDialog()
        }
    }

    private fun openCommunityMenuDialog(){
        val menuBinding = DialogMenuBinding.inflate(layoutInflater)
        val menuDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(menuBinding.root))
            .setMargin(50,0,50,0)
            .setCancelable(true)
            .setGravity(Gravity.BOTTOM)
            .create()


        //Option 1: Search in Community
        menuBinding.option1.visibility = View.VISIBLE
        menuBinding.optionIcon1.setImageResource(R.drawable.search_icon)
        menuBinding.optiontitle1.text = "Search In Community"
        menuBinding.optiontitle1.setOnClickListener {
            menuDialog.dismiss()
            Toast.makeText(context, "To be implemented", Toast.LENGTH_SHORT).show()
        }

        //Option 2: Community General Chat
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optionIcon2.setImageResource(R.drawable.baseline_chat_bubble_24)
        menuBinding.optiontitle2.text = "Community General Chat"
        menuBinding.optiontitle2.setOnClickListener {
            menuDialog.dismiss()
            val intent = Intent(context, Chatroom::class.java)
            intent.putExtra("chatroomName", communityModel.communityChannels[0])
            intent.putExtra("communityId", communityId)
            intent.putExtra("chatroomType", "community_chat")
            intent.putExtra("chatroomId", "$communityId-${communityModel.communityChannels[0]}")
            startActivity(intent)
        }


        //Option 3: Community Settings
        menuBinding.option3.visibility = View.VISIBLE
        if (isUserAdmin){
            menuBinding.optionIcon3.setImageResource(R.drawable.admin_settings)
        } else {
            menuBinding.optionIcon3.setImageResource(R.drawable.gear_icon)
        }
        menuBinding.optiontitle3.text = "Community Settings"
        menuBinding.optiontitle3.setOnClickListener {
            menuDialog.dismiss()
            val intent = Intent(context, CommunitySettings::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            intent.putExtra("isUserAdmin", isUserAdmin)
            startActivity(intent)
        }

        menuBinding.option4.visibility = View.VISIBLE

        menuDialog.show()

    }
    private fun selectNavigation(fragment:String) {
        binding.feedsIconIV.setImageResource(R.drawable.feeds_not_selected)
        binding.eventsIconIV.setImageResource(R.drawable.events_not_selected)
        binding.forumsIconIV.setImageResource(R.drawable.forums_not_selected)
        binding.marketIconIV.setImageResource(R.drawable.market_not_selected)
        binding.activitiesIconIV.setImageResource(R.drawable.activities_not_selected)
        when (fragment) {
            "feeds" -> {
                binding.feedsIconIV.setImageResource(R.drawable.feeds_selected)
            }
            "events" -> {
                binding.eventsIconIV.setImageResource(R.drawable.events_selected)
            }
            "forums" -> {
                binding.forumsIconIV.setImageResource(R.drawable.forums_selected)
            }
            "market" -> {
                binding.marketIconIV.setImageResource(R.drawable.market_selected)
            }
            "activities" -> {
                binding.activitiesIconIV.setImageResource(R.drawable.activities_selected)

            }
        }
    }
}
