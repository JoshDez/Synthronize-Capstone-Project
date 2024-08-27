// GroupFragment.kt

package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.ChatroomAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogCommunityTextChannelsBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CommunityFragment(private val mainBinding: ActivityMainBinding, private val communityId:String) : Fragment(), OnNetworkRetryListener {

    private lateinit var binding: FragmentCommunityBinding
    private lateinit var communityModel: CommunityModel
    private lateinit var context: Context

    //for community text channels
    private lateinit var chatroomAdapter:ChatroomAdapter
    private lateinit var dialogTextChannelsBinding: DialogCommunityTextChannelsBinding
    private lateinit var dialogTextChannel: DialogPlus

    //for Community Admin and App Admin
    private var isUserAdmin = false
    private var isUserModerator = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAdded){
            //retrieve context
            context = requireContext()

            if (::context.isInitialized){
                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)
                communityFragmentWrapper()
            }
        }
    }

    private fun communityFragmentWrapper(){
        //retrieve community model
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            if (it.exists()){
                communityModel = it.toObject(CommunityModel::class.java)!!
                //content
                if (::context.isInitialized){
                    setupCommunityFragment()
                }
            }
        }
    }

    private fun setupCommunityFragment(){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val myModel = it.toObject(UserModel::class.java)!!

            //reset main toolbar
            AppUtil().resetMainToolbar(mainBinding)
            //bind community
            assignUserRole(myModel)
            bindCommunityDetails()
            bindButtons()
            //Set Feeds fragment as default fragment
            selectNavigation("feeds")
            replaceFragment(FeedsFragment(binding, communityId))
        }
    }


    private fun bindCommunityDetails(){
        //set name
        mainBinding.toolbarTitleTV.text = communityModel.communityName
        //set community profile photo
        AppUtil().setCommunityProfilePic(context, communityModel.communityId, mainBinding.toolbarImageCIV)
    }

    private fun assignUserRole(myModel: UserModel) {
        for (user in communityModel.communityMembers){
            if (myModel.userType == "AppAdmin"){
                //User is AppAdmin
                isUserAdmin = true
            } else if (user.value == "Admin" && myModel.userID == user.key){
                //User is Admin
                isUserAdmin = true
            } else if (user.value == "Moderator" && myModel.userID == user.key){
                //User is Moderator
                isUserModerator = true
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
            replaceFragment(ActivitiesFragment(binding, communityId, isUserAdmin))
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

        //Option 2: Community Text Channels
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optionIcon2.setImageResource(R.drawable.baseline_chat_bubble_24)
        menuBinding.optiontitle2.text = "Community Text Channels"
        menuBinding.optiontitle2.setOnClickListener {
            menuDialog.dismiss()
            Handler().postDelayed({
                openTextChannelsDialog()
            }, 500)
        }


        //Option 3: Community Settings
        menuBinding.option3.visibility = View.VISIBLE
        menuBinding.optionIcon3.setImageResource(R.drawable.admin_settings)
        menuBinding.optiontitle3.text = "Community Settings"
        menuBinding.optiontitle3.setOnClickListener {
            menuDialog.dismiss()
            val intent = Intent(context, CommunitySettings::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            intent.putExtra("isUserAdmin", isUserAdmin)
            startActivity(intent)
        }
        menuBinding.option4.visibility = View.VISIBLE


        //Option 4: App Settings
        menuBinding.option4.visibility = View.VISIBLE
        menuBinding.optionIcon4.setImageResource(R.drawable.gear_icon)
        menuBinding.optiontitle4.text = "App Settings"
        menuBinding.optiontitle4.setOnClickListener {
            menuDialog.dismiss()
            val intent = Intent(context, AppSettings::class.java)
            startActivity(intent)
        }
        menuBinding.option4.visibility = View.VISIBLE

        menuDialog.show()

    }

    private fun openTextChannelsDialog() {
        dialogTextChannelsBinding = DialogCommunityTextChannelsBinding.inflate(layoutInflater)
        dialogTextChannel = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogTextChannelsBinding.root))
            .setExpanded(false)
            .create()

        openCommunityTextChannels()


        dialogTextChannel.show()
    }

    private fun openCommunityTextChannels(){
        dialogTextChannelsBinding.createNewTextChannelLayout.visibility = View.GONE
        dialogTextChannelsBinding.communityTextChannelsLayout.visibility = View.VISIBLE

        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val myModel = it.toObject(UserModel::class.java)!!
            var role = ""

            if (myModel.userType == "AppAdmin"){
                role = "AppAdmin"
            } else if (isUserAdmin){
                role = "Admin"
            } else if (isUserModerator){
                role = "Moderator"
            } else {
                role = "Member"
            }

            setupTextChannelsRV(role)

            if (role == "AppAdmin" || role == "Admin"){
                dialogTextChannelsBinding.addTextChannelBtn.visibility = View.VISIBLE
                dialogTextChannelsBinding.addTextChannelBtn.setOnClickListener {
                    openCreateTextChannel()
                }
            }
        }

        dialogTextChannelsBinding.backBtn.setOnClickListener {
            dialogTextChannel.dismiss()
        }
    }

    private fun openCreateTextChannel() {
        dialogTextChannelsBinding.communityTextChannelsLayout.visibility = View.GONE
        dialogTextChannelsBinding.createNewTextChannelLayout.visibility = View.VISIBLE

        dialogTextChannelsBinding.saveBtn.setOnClickListener {
            val name = dialogTextChannelsBinding.textChannelNameEdtTxt.text.toString()
            val roles:ArrayList<String> = ArrayList()
            val chatroomMembers:ArrayList<String> = ArrayList()

            if (dialogTextChannelsBinding.adminCB.isChecked){
                roles.add("Admin")
                chatroomMembers.addAll(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"))
            }
            if (dialogTextChannelsBinding.moderatorCB.isChecked){
                roles.add("Moderator")
                chatroomMembers.addAll(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Moderator"))
            }
            if (dialogTextChannelsBinding.memberCB.isChecked){
                roles.add("Member")
                chatroomMembers.addAll(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Member"))
            }

            if (!dialogTextChannelsBinding.adminCB.isChecked){
                chatroomMembers.addAll(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"))
            }

            if (name.isEmpty() || name.length < 2){
                Toast.makeText(context, "Name should at least have more than 2 characters", Toast.LENGTH_SHORT).show()
            } else if (AppUtil().containsBadWord(name)) {
                Toast.makeText(context, "The name contains sensitive word/s", Toast.LENGTH_SHORT).show()
            } else if (roles.isEmpty()) {
                Toast.makeText(context, "Select at least one user type", Toast.LENGTH_SHORT).show()
            } else {

                var chatroomModel = ChatroomModel()

                FirebaseUtil().retrieveAllChatRoomReferences().add(chatroomModel).addOnSuccessListener {

                    chatroomModel = ChatroomModel(
                        chatroomId = it.id,
                        chatroomType = "community_chat",
                        userIdList = chatroomMembers,
                        lastMsgTimestamp = Timestamp.now(),
                        lastMessage = "Created the text channel",
                        lastMessageUserId = FirebaseUtil().currentUserUid(),
                        chatroomName = name,
                        communityId = communityId,
                        chatroomAdminList = AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin")
                    )

                    FirebaseUtil().retrieveAllChatRoomReferences().document(chatroomModel.chatroomId).set(chatroomModel).addOnSuccessListener {
                        openCommunityTextChannels()
                    }
                }
            }
        }


        dialogTextChannelsBinding.backBtn.setOnClickListener {
            openCommunityTextChannels()
        }

    }

    private fun setupTextChannelsRV(userType:String){
        val query:Query

        if (userType == "AppAdmin"){
            query = FirebaseUtil().retrieveAllChatRoomReferences()
                .whereEqualTo("communityId", communityId)
        } else {
            query = FirebaseUtil().retrieveAllChatRoomReferences()
                .whereEqualTo("communityId", communityId)
                .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
        }

        // Add a listener to handle success or failure of the query
        query.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                android.util.Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                //binding.chatRefreshLayout.isRefreshing =  false
            }
        }

        val options: FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(query, ChatroomModel::class.java).build()

        dialogTextChannelsBinding.textChannelsRV.layoutManager = LinearLayoutManager(context)
        chatroomAdapter = ChatroomAdapter(context, options)
        dialogTextChannelsBinding.textChannelsRV.adapter = chatroomAdapter
        chatroomAdapter.startListening()
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

    override fun retryNetwork() {
        communityFragmentWrapper()
    }
}
