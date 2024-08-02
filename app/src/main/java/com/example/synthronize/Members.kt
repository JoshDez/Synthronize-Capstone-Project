package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityMembersBinding
import com.example.synthronize.databinding.DialogUserMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class Members : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityMembersBinding
    private lateinit var membersAdapter: SearchUserAdapter
    private lateinit var adminAdapter: SearchUserAdapter
    private lateinit var moderatorAdapter: SearchUserAdapter
    private lateinit var communityId:String
    private lateinit var chatroomId:String
    private lateinit var communityModel: CommunityModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()

        queryUsers()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.searchEdtTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchQuery = binding.searchEdtTxt.text.toString()
                queryUsers(searchQuery)
            }
        })
    }
    override fun onItemClick(id: String, isChecked: Boolean) {
        if (id != FirebaseUtil().currentUserUid()){

            FirebaseUtil().targetUserDetails(id).get().addOnSuccessListener { result ->
                val userModel = result.toObject(UserModel::class.java)!!

                val dialogPlusBinding = DialogUserMenuBinding.inflate(layoutInflater)
                val dialogPlus = DialogPlus.newDialog(this)
                    .setContentHolder(ViewHolder(dialogPlusBinding.root))
                    .setInAnimation(androidx.appcompat.R.anim.abc_fade_in)
                    .setCancelable(true)
                    .setGravity(Gravity.CENTER)
                    .create()

                AppUtil().setUserProfilePic(this, userModel.userID, dialogPlusBinding.userProfileCIV)
                AppUtil().setUserCoverPic(this, userModel.userID, dialogPlusBinding.userCoverIV)
                dialogPlusBinding.userDisplayNameTV.text = userModel.fullName
                if (userModel.username.isNotEmpty()){
                    dialogPlusBinding.userNameTV.visibility = View.VISIBLE
                    dialogPlusBinding.userNameTV.text = userModel.username
                }

                //view profile button
                dialogPlusBinding.viewProfileBtn.setOnClickListener {
                    val intent = Intent(this, OtherUserProfile::class.java)
                    intent.putExtra("userID", id)
                    startActivity(intent)
                }
                //message user button
                dialogPlusBinding.messageUserBtn.setOnClickListener {
                    val intent = Intent(this, Chatroom::class.java)
                    intent.putExtra("chatroomName", userModel.fullName)
                    intent.putExtra("userID", id)
                    intent.putExtra("chatroomType", "direct_message")
                    startActivity(intent)
                    dialogPlus.dismiss()
                }


                //Additional Buttons
                //Buttons only for App Admin
                FirebaseUtil().isUserAppAdmin(FirebaseUtil().currentUserUid()){isUserAppAdmin ->
                    if (isUserAppAdmin){
                        //Buttons only for App Admin
                        if (userModel.userType != "AppAdmin"){
                            showKickUserButton(id, dialogPlusBinding, dialogPlus)
                            if (!isUserAdmin(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn, dialogPlus, "Admin")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                            if (!isUserModerator(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn2, dialogPlus, "Moderator")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                            if (!isUserMember(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn3, dialogPlus, "Member")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                        }
                    } else if (isUserAdmin(FirebaseUtil().currentUserUid())){
                        //Buttons only for Admin or Community Admin
                        if(userModel.userType != "AppAdmin" && !AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"), id)){
                            showKickUserButton(id, dialogPlusBinding, dialogPlus)
                            if (!isUserAdmin(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn, dialogPlus, "Admin")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                            if (!isUserModerator(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn2, dialogPlus, "Moderator")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                            if (!isUserMember(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn3, dialogPlus, "Member")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                        }
                    } else if (isUserModerator(FirebaseUtil().currentUserUid())){
                        //Buttons only for Moderators
                        if(userModel.userType != "AppAdmin" && !AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"), id)
                            && !AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Moderator"), id)){
                            showKickUserButton(id, dialogPlusBinding, dialogPlus)
                            if (!isUserModerator(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn2, dialogPlus, "Moderator")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                            if (!isUserMember(id)){
                                showChangeRoleButton(id, dialogPlusBinding.changeUserRoleBtn3, dialogPlus, "Member")
                                showBanUserBtn(id, dialogPlusBinding, dialogPlus)
                            }
                        }
                    }
                }

                AppUtil().changeFriendsButtonState(dialogPlusBinding.friendBtn, userModel)

                showReportButton(id, dialogPlusBinding)

                dialogPlus.show()
            }

        }
    }

    //For User Dialog Menu
    private fun isUserAdmin(userId: String): Boolean{
        for (user in AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin")){
            if (userId == user){
                return true
            }
        }
        return false
    }

    private fun isUserModerator(userId: String): Boolean{
        for (user in AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Moderator")){
            if (userId == user){
                return true
            }
        }
        return false
    }

    private fun isUserMember(userId: String): Boolean{
        for (user in AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Member")){
            if (userId == user){
                return true
            }
        }
        return false
    }

    private fun showBanUserBtn(id:String, dialogBinding: DialogUserMenuBinding, dialogPlus: DialogPlus){
        if (!AppUtil().isIdOnList(communityModel.bannedUsers, id)){
            dialogBinding.banUserFromCommunityBtn.visibility = View.VISIBLE
            dialogBinding.banUserFromCommunityBtn.text = "Ban User From Community"
            dialogBinding.banUserFromCommunityBtn.setOnClickListener {
                val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                val warningDialog = DialogPlus.newDialog(this)
                    .setContentHolder(ViewHolder(warningBinding.root))
                    .setGravity(Gravity.CENTER)
                    .setMargin(50, 700, 50, 700)
                    .create()

                warningBinding.messageTV.text = "Do you want to ban this user from community?"
                warningBinding.titleTV.text = "Are you sure?"

                warningBinding.yesBtn.setOnClickListener {
                    //removes user from community channels before leaving the community
                    FirebaseUtil().removeUserFromAllCommunityChannels(communityModel.communityId, id){isSuccessful ->
                        if (isSuccessful){
                            //Removes user from community members
                            val updatedMap = mapOf(
                                "communityMembers.${id}" to FieldValue.delete()
                            )
                            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                                .update(updatedMap).addOnSuccessListener {
                                    //adds the user to the blockList
                                    FirebaseUtil().retrieveCommunityDocument(communityId).update("bannedUsers", FieldValue.arrayUnion(id)).addOnSuccessListener {
                                        Toast.makeText(this, "The user is now banned", Toast.LENGTH_SHORT).show()
                                        queryUsers()
                                        warningDialog.dismiss()
                                    }.addOnFailureListener {
                                        Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                warningBinding.NoBtn.setOnClickListener {
                    warningDialog.dismiss()
                    Handler().postDelayed({
                        dialogPlus.show()
                    }, 500)
                }

                dialogPlus.dismiss()
                Handler().postDelayed({
                    warningDialog.show()
                }, 500)
            }
        }
    }

    private fun showReportButton(id:String, dialogBinding:DialogUserMenuBinding) {
        dialogBinding.reportProfileBtn.setOnClickListener {
            DialogUtil().openReportDialog(this, layoutInflater, "User", id)
        }
    }
    private fun showChangeRoleButton(userId: String, changeUserRoleBtn:AppCompatButton, dialogPlus:DialogPlus, role:String){
        changeUserRoleBtn.visibility = View.VISIBLE
        changeUserRoleBtn.text = "Make User $role"
        changeUserRoleBtn.setOnClickListener {
            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val warningDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(warningBinding.root))
                .setGravity(Gravity.CENTER)
                .setMargin(50, 700, 50, 700)
                .create()

            warningBinding.messageTV.text = "Do you want to make this user $role?"
            warningBinding.titleTV.text = "Change User Role"

            warningBinding.yesBtn.setOnClickListener {
                val newMapUpdate = mapOf(
                    "communityMembers.${userId}" to role
                )
                FirebaseUtil().retrieveCommunityDocument(communityId).update(newMapUpdate).addOnSuccessListener {
                    Toast.makeText(this, "The user is now $role", Toast.LENGTH_SHORT).show()
                    queryUsers()
                    warningDialog.dismiss()
                }
            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
                Handler().postDelayed({
                    dialogPlus.show()
                }, 500)
            }

            dialogPlus.dismiss()
            Handler().postDelayed({
                warningDialog.show()
            }, 500)
        }
    }


    private fun showKickUserButton(userId: String, dialogPlusBinding:DialogUserMenuBinding, dialogPlus:DialogPlus){
        //if the target user is not an admin
        dialogPlusBinding.kickUserBtn.visibility = View.VISIBLE
        dialogPlusBinding.kickUserBtn.setOnClickListener {
            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val warningDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(warningBinding.root))
                .setGravity(Gravity.CENTER)
                .setMargin(50, 700, 50, 700)
                .create()

            warningBinding.messageTV.text = "Do you want to kick this user from the community?"
            warningBinding.titleTV.text = "Kick User"

            warningBinding.yesBtn.setOnClickListener {
                FirebaseUtil().removeUserFromAllCommunityChannels(communityModel.communityId, FirebaseUtil().currentUserUid()){isSuccessful ->
                    if (isSuccessful){
                        val updatedMap = mapOf(
                            "communityMembers.${userId}" to FieldValue.delete()
                        )
                        FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                            .update(updatedMap).addOnSuccessListener {
                                Toast.makeText(this, "The user has been kicked", Toast.LENGTH_SHORT).show()
                                queryUsers()
                                warningDialog.dismiss()
                            }
                    } else {
                        Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                    }
                }


            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
                Handler().postDelayed({
                    dialogPlus.show()
                }, 500)
            }
            dialogPlus.dismiss()
            Handler().postDelayed({
                warningDialog.show()
            }, 500)
        }
    }

    //FOR RETRIEVING USER FROM FIRESTORE
    private fun queryUsers(searchQuery:String = ""){
        binding.membersLayout.visibility = View.INVISIBLE
        binding.moderatorLayout.visibility = View.INVISIBLE
        binding.adminLayout.visibility = View.INVISIBLE

        if (communityId.isNotEmpty()){
            //For Community Members
            binding.toolbarTitleTV.text = "Community Members"
            FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                communityModel = it.toObject(CommunityModel::class.java)!!

                //FOR MEMBERS
                val membersQuery:Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", communityModel.communityMembers.keys.toList())
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val membersOptions:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(membersQuery, UserModel::class.java).build()

                setupMembersRV(membersOptions)


                //FOR ADMINS
                val admins = AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin")
                if (admins.isNotEmpty()){
                    val adminsQuery:Query = FirebaseUtil().allUsersCollectionReference()
                        .whereIn("userID", admins)
                        .whereGreaterThanOrEqualTo("fullName", searchQuery)

                    val adminsOptions:FirestoreRecyclerOptions<UserModel> =
                        FirestoreRecyclerOptions.Builder<UserModel>().setQuery(adminsQuery, UserModel::class.java).build()

                    setupAdminRV(adminsOptions)
                }

                //FOR MODERATORS
                val moderators = AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Moderator")
                if (moderators.isNotEmpty()){
                    val moderatorQuery:Query = FirebaseUtil().allUsersCollectionReference()
                        .whereIn("userID", moderators)
                        .whereGreaterThanOrEqualTo("fullName", searchQuery)

                    val moderatorOptions:FirestoreRecyclerOptions<UserModel> =
                        FirestoreRecyclerOptions.Builder<UserModel>().setQuery(moderatorQuery, UserModel::class.java).build()

                    setupModeratorRV(moderatorOptions)
                }

            }
        } else {
            //TODO For Chatroom Members
            binding.toolbarTitleTV.text = "Chatroom Members"
        }
    }

    private fun setupModeratorRV(options: FirestoreRecyclerOptions<UserModel>) {
        binding.moderatorLayout.visibility = View.VISIBLE
        moderatorAdapter = SearchUserAdapter(this, options, this)
        binding.moderatorRV.layoutManager = LinearLayoutManager(this)
        binding.moderatorRV.adapter = moderatorAdapter
        moderatorAdapter.startListening()
    }

    private fun setupMembersRV(options:FirestoreRecyclerOptions<UserModel>){
        binding.membersLayout.visibility = View.VISIBLE
        membersAdapter = SearchUserAdapter(this, options, this)
        binding.membersRV.layoutManager = LinearLayoutManager(this)
        binding.membersRV.adapter = membersAdapter
        membersAdapter.startListening()
    }

    private fun setupAdminRV(options:FirestoreRecyclerOptions<UserModel>){
        binding.adminLayout.visibility = View.VISIBLE
        adminAdapter = SearchUserAdapter(this, options, this)
        binding.adminRV.layoutManager = LinearLayoutManager(this)
        binding.adminRV.adapter = adminAdapter
        adminAdapter.startListening()
    }

    override fun onStart() {
        super.onStart()
        if (::adminAdapter.isInitialized){
            adminAdapter.startListening()
        }
        if (::membersAdapter.isInitialized){
            membersAdapter.startListening()
        }
        if (::moderatorAdapter.isInitialized){
            moderatorAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adminAdapter.isInitialized){
            adminAdapter.notifyDataSetChanged()
        }
        if (::membersAdapter.isInitialized){
            membersAdapter.notifyDataSetChanged()
        }
        if (::moderatorAdapter.isInitialized){
            moderatorAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::adminAdapter.isInitialized){
            adminAdapter.stopListening()
        }
        if (::membersAdapter.isInitialized){
            membersAdapter.stopListening()
        }
        if (::moderatorAdapter.isInitialized){
            moderatorAdapter.stopListening()
        }
    }
}