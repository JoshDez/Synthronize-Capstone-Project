package com.example.synthronize

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityCommunitySettingsBinding
import com.example.synthronize.databinding.DialogSelectUserBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NotificationUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CommunitySettings : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityCommunitySettingsBinding
    private lateinit var communityModel: CommunityModel
    private lateinit var dialogPlusBinding: DialogSelectUserBinding
    private lateinit var selectedUsersAdapter: SearchUserAdapter
    private lateinit var searchUserAdapter: SearchUserAdapter
    private var selectedUsersList:ArrayList<String> = arrayListOf()
    private var searchUserQuery = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val communityId = intent.getStringExtra("communityId").toString()
        val isUserAdmin = intent.getBooleanExtra("isUserAdmin", false)

        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            communityModel = it.toObject(CommunityModel::class.java)!!
            navigate("general")
        }

        if (isUserAdmin){
            binding.navigationLayout.visibility = View.VISIBLE
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.adminBtn.setOnClickListener {
            navigate("admin")
        }

        binding.generalBtn.setOnClickListener {
            navigate("general")
        }

    }

    private fun navigate(tab:String){
        val unselectedColor = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(this, R.color.light_teal)
        binding.generalBtn.setTextColor(unselectedColor)
        binding.adminBtn.setTextColor(unselectedColor)
        binding.generalLayout.visibility = View.GONE
        binding.adminLayout.visibility = View.GONE

        if (tab == "general"){
            setupGeneralLayout()
            binding.generalBtn.setTextColor(selectedColor)
        }else if (tab == "admin") {
            setupAdminLayout()
            binding.adminBtn.setTextColor(selectedColor)
        }
    }

    private fun setupAdminLayout() {
        binding.adminLayout.visibility = View.VISIBLE

        if (communityModel.communityType == "Private"){
            binding.viewJoinRequestsBtn.visibility = View.VISIBLE
            binding.viewJoinRequestsBtn.setOnClickListener {
                val intent = Intent(this, Requests::class.java)
                intent.putExtra("communityId", communityModel.communityId)
                startActivity(intent)
            }
        }

        binding.bannedUsersBtn.setOnClickListener {
            val intent = Intent(this, BanAndBlockList::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.inviteUsersAdminBtn.setOnClickListener {
            openInviteMembersDialog()
        }

        binding.viewCommunityReportsBtn.setOnClickListener {
            val intent = Intent(this, Reports::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.editCommunityDetailsBtn.setOnClickListener {
            val intent = Intent(this, EditCommunity::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }
        binding.editCommunityRulesBtn.setOnClickListener {
            val intent = Intent(this, CommunityRules::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            intent.putExtra("toEdit", true)
            startActivity(intent)
        }
        binding.deleteCommunityBtn.setOnClickListener {
            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val warningDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(warningBinding.root))
                .setBackgroundColorResId(R.color.transparent)
                .setGravity(Gravity.CENTER)
                .create()
            warningBinding.titleTV.text = "Warning"
            warningBinding.messageTV.text = "Do you want to permanently delete this community?"
            warningBinding.yesBtn.setOnClickListener {
                warningDialog.dismiss()
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId).delete().addOnSuccessListener {
                    Toast.makeText(this, "The community is deleted", Toast.LENGTH_SHORT).show()
                    deleteAllCommunityChannels()
                    AppUtil().headToMainActivity(this)
                }
            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
            }
            warningDialog.show()
        }
    }

    private fun setupGeneralLayout() {
        binding.generalLayout.visibility = View.VISIBLE

        //Common Binds
        binding.communityNameTV.text = communityModel.communityName
        binding.communityCodeEdtTxt.setText(communityModel.communityCode)
        AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.userProfileCIV)
        AppUtil().setCommunityBannerPic(this, communityModel.communityId, binding.communityBannerIV)

        if (communityModel.communityDescription.isNotEmpty()){
            binding.communityDescriptionTV.visibility = View.VISIBLE
            binding.communityDescriptionTV.text = communityModel.communityDescription
        }

        if (communityModel.communityType == "Public"){
            binding.inviteUsersGeneralBtn.visibility = View.VISIBLE
            binding.inviteUsersGeneralBtn.setOnClickListener {
                openInviteMembersDialog()
            }
        }

        binding.leaveCommunityBtn.setOnClickListener {
            //get the list of userIDs of Admins
            val admin = AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin")

            if (admin.size == 1 && admin.contains(FirebaseUtil().currentUserUid())){
                //shows a toast message
                Toast.makeText(this, "The community should at least have 1 admin available", Toast.LENGTH_SHORT).show()
            } else {
                //show error message
                val dialogPlusBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                val dialogPlus = DialogPlus.newDialog(this)
                    .setContentHolder(ViewHolder(dialogPlusBinding.root))
                    .setGravity(Gravity.CENTER)
                    .setBackgroundColorResId(R.color.transparent)
                    .setCancelable(true)
                    .create()

                dialogPlusBinding.titleTV.text = "Warning!"
                dialogPlusBinding.messageTV.text = "Do you want to leave this community?"
                dialogPlusBinding.yesBtn.setOnClickListener {
                    //removes user from community channels before leaving the community
                    FirebaseUtil().removeUserFromAllCommunityChannels(communityModel.communityId, FirebaseUtil().currentUserUid()){isSuccessful ->
                        if (isSuccessful){
                            //Removes user from community members
                            val updatedMap = mapOf(
                                "communityMembers.${FirebaseUtil().currentUserUid()}" to FieldValue.delete()
                            )
                            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                                .update(updatedMap).addOnSuccessListener {
                                    AppUtil().headToMainActivity(this)
                                }
                        } else {
                            Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialogPlusBinding.NoBtn.setOnClickListener {
                    dialogPlus.dismiss()
                }
                dialogPlus.show()
            }
        }

        binding.viewCommunityRulesBtn.setOnClickListener {
            val intent = Intent(this, CommunityRules::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.copyCodeBtn.setOnClickListener {
            val textToCopy = binding.communityCodeEdtTxt.text.toString()
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Copied Text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.viewMembersBtn.setOnClickListener {
            val intent = Intent(applicationContext, Members::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.reportsMadeBtn.setOnClickListener {
            val intent = Intent(applicationContext, Reports::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            intent.putExtra("isPersonalReport", true)
            startActivity(intent)
        }

        binding.reportCommunityBtn.setOnClickListener {
            DialogUtil().openReportDialog(this, layoutInflater, "Community",communityModel.communityId)
        }
    }

    private fun openInviteMembersDialog() {
        dialogPlusBinding = DialogSelectUserBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogPlusBinding.root))
            .setExpanded(false)
            .create()

        searchUsers()

        dialogPlusBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchUserQuery = dialogPlusBinding.searchEdtTxt.text.toString()
                searchUsers()
            }
        })

        setupSelectedUsersRV()

        dialogPlusBinding.backBtn.setOnClickListener {
            dialogPlus.dismiss()
        }

        dialogPlusBinding.assignBtn.text = "Invite Members"
        dialogPlusBinding.assignBtn.setOnClickListener {
            if (selectedUsersList.isNotEmpty()){
                inviteUsersToCommunity(communityModel.communityId)
                dialogPlus.dismiss()
                Toast.makeText(this, "Successfully invited users", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select users", Toast.LENGTH_SHORT).show()
            }
        }
        Handler().postDelayed({
            dialogPlus.show()
        }, 500)
    }


    private fun setupSelectedUsersRV(){
        if (selectedUsersList.isNotEmpty()){

            dialogPlusBinding.selectedUsersLayout.visibility = View.VISIBLE
            dialogPlusBinding.selectedUsersTV.text = "Selected Users (${selectedUsersList.size})"

            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", selectedUsersList)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            dialogPlusBinding.selectedUsersRV.layoutManager = LinearLayoutManager(this)
            selectedUsersAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
            dialogPlusBinding.selectedUsersRV.adapter = selectedUsersAdapter
            selectedUsersAdapter.startListening()

        } else {
            dialogPlusBinding.selectedUsersTV.text = "Selected Users (0)"
            dialogPlusBinding.selectedUsersLayout.visibility = View.GONE
        }
    }


    private fun searchUsers(){
        if (searchUserQuery.isNotEmpty()){
            if (searchUserQuery[0] == '@'){
                //search for username
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereNotIn("userID", communityModel.communityMembers.keys.toList())
                    .whereGreaterThanOrEqualTo("username", searchUserQuery.removePrefix("@"))

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
                dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            } else {
                //search for fullName
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereNotIn("userID", communityModel.communityMembers.keys.toList())
                    .whereGreaterThanOrEqualTo("fullName", searchUserQuery)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
                dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        } else {
            //query all users
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereNotIn("userID",  communityModel.communityMembers.keys.toList())

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
            searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
            dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
            searchUserAdapter.startListening()
        }
    }


    private fun deleteAllCommunityChannels(){
        FirebaseUtil().retrieveAllChatRoomReferences()
            .whereEqualTo("communityId", communityModel.communityId).get().addOnSuccessListener {channels ->
                for (channel in channels.documents){
                    val chatModel = channel.toObject(ChatroomModel::class.java)!!
                    FirebaseUtil().retrieveChatRoomReference(chatModel.chatroomId).delete()
                }
            }
    }

    private fun inviteUsersToCommunity(communityId: String) {
        for (user in selectedUsersList){
            // Create a map to represent the field you want to update
            val updates = hashMapOf<String, Any>(
                "communityInvitations.${FirebaseUtil().currentUserUid()}" to communityId
            )
            FirebaseUtil().targetUserDetails(user).update(updates)
            NotificationUtil().sendPushNotificationsForRequestsAndInvitations(this, user, "Community Invitation", communityModel.communityName)
        }
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        //Interface for select user adapter
        if (isChecked) {
            //add user to selected user list
            selectedUsersList.add(id)
            setupSelectedUsersRV()
            searchUsers()
        } else {
            //remove user to selected user list
            selectedUsersList.remove(id)
            setupSelectedUsersRV()
            searchUsers()
        }
    }


}