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
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.lang.Exception

class Members : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityMembersBinding
    private lateinit var membersAdapter: SearchUserAdapter
    private lateinit var adminAdapter: SearchUserAdapter
    private lateinit var communityId:String
    private lateinit var chatroomId:String
    private lateinit var communityModel: CommunityModel
    private lateinit var chatroomModel: ChatroomModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        chatroomId = intent.getStringExtra("chatroomId").toString()

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
    override fun onUserClick(userId: String, isChecked: Boolean) {
        if (userId != FirebaseUtil().currentUserUid()){

            FirebaseUtil().targetUserDetails(userId).get().addOnSuccessListener { result ->
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
                    intent.putExtra("userID", userId)
                    startActivity(intent)
                }
                //message user button
                dialogPlusBinding.messageUserBtn.setOnClickListener {
                    val intent = Intent(this, Chatroom::class.java)
                    intent.putExtra("chatroomName", userModel.fullName)
                    intent.putExtra("userID", userId)
                    intent.putExtra("chatroomType", "direct_message")
                    startActivity(intent)
                    dialogPlus.dismiss()
                }

                //additional buttons if current user is admin
                if (isUserAdmin(FirebaseUtil().currentUserUid())){
                    //kick user button
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
                            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                                .update("communityAdmin", FieldValue.arrayRemove(userId))
                            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                                .update("communityMembers", FieldValue.arrayRemove(userId)).addOnSuccessListener {
                                    Toast.makeText(this, "The user has been kicked", Toast.LENGTH_SHORT).show()
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

                    //if the user in the profile is not yet admin
                    if (!isUserAdmin(userId)){
                        dialogPlusBinding.makeUserAdminBtn.visibility = View.VISIBLE
                        dialogPlusBinding.makeUserAdminBtn.setOnClickListener {
                            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                            val warningDialog = DialogPlus.newDialog(this)
                                .setContentHolder(ViewHolder(warningBinding.root))
                                .setGravity(Gravity.CENTER)
                                .setMargin(50, 700, 50, 700)
                                .create()

                            warningBinding.messageTV.text = "Do you want to make this user admin?"
                            warningBinding.titleTV.text = "Grant Admin Privilege"

                            warningBinding.yesBtn.setOnClickListener {
                                communityModel.communityAdmin = communityModel.communityAdmin.plus(userId)
                                FirebaseUtil().retrieveCommunityDocument(communityId).set(communityModel).addOnSuccessListener {
                                    Toast.makeText(this, "The user is now Admin", Toast.LENGTH_SHORT).show()
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
                }

                changeFriendsButtonState(userModel, dialogPlusBinding)
                dialogPlus.show()
            }

        }
    }
    //For User Dialog Menu
    private fun isUserAdmin(userId: String): Boolean{
        for (user in communityModel.communityAdmin){
            if (userId == user){
                return true
            }
        }
        return false
    }

    //For User Dialog Menu
    private fun changeFriendsButtonState(userModel:UserModel, dialogUserMenuBinding: DialogUserMenuBinding){
        //checks if already friends with user
        if (AppUtil().isUserOnList(userModel.friendsList, FirebaseUtil().currentUserUid())){
            dialogUserMenuBinding.friendBtn.text = "Unfriend"
            dialogUserMenuBinding.friendBtn.setOnClickListener {
                userModel.friendsList = userModel.friendsList.filterNot { it == FirebaseUtil().currentUserUid() }
                FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                    changeFriendsButtonState(userModel, dialogUserMenuBinding)
                }
            }

        } else if (AppUtil().isUserOnList(userModel.friendRequests, FirebaseUtil().currentUserUid())){
            dialogUserMenuBinding.friendBtn.text = "Cancel Request"
            dialogUserMenuBinding.friendBtn.setOnClickListener {
                userModel.friendRequests = userModel.friendRequests.filterNot { it == FirebaseUtil().currentUserUid() }
                FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                    changeFriendsButtonState(userModel, dialogUserMenuBinding)
                }
            }
        } else {
            dialogUserMenuBinding.friendBtn.text = "Add Friend"
            dialogUserMenuBinding.friendBtn.setOnClickListener {
                userModel.friendRequests = userModel.friendRequests.plus(FirebaseUtil().currentUserUid())
                FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                    changeFriendsButtonState(userModel, dialogUserMenuBinding)
                }
            }
        }
    }


    //FOR RETRIEVING USER FROM FIRESTORE
    private fun queryUsers(searchQuery:String = ""){
        binding.membersLayout.visibility = View.INVISIBLE
        binding.adminLayout.visibility = View.INVISIBLE

        if (communityId.isNotEmpty()){
            //For Community Members
            binding.toolbarTitleTV.text = "Community Members"
            FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                communityModel = it.toObject(CommunityModel::class.java)!!

                //FOR MEMBERS
                val membersQuery:Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", communityModel.communityMembers)
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val membersOptions:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(membersQuery, UserModel::class.java).build()


                //FOR ADMINS
                val adminsQuery:Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", communityModel.communityAdmin)
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val adminsOptions:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(adminsQuery, UserModel::class.java).build()

                //Setup Recyclers
                setupMembersRV(membersOptions)
                setupAdminRV(adminsOptions)


            }
        } else {
            //TODO For Chatroom Members
            binding.toolbarTitleTV.text = "Chatroom Members"
        }
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
    }

    override fun onResume() {
        super.onResume()
        if (::adminAdapter.isInitialized){
            adminAdapter.notifyDataSetChanged()
        }
        if (::membersAdapter.isInitialized){
            membersAdapter.notifyDataSetChanged()
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
    }
}