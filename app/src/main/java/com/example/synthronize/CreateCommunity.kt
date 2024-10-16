package com.example.synthronize

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityCreateCommunityBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NotificationUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CreateCommunity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityCreateCommunityBinding
    private lateinit var searchUserAdapter:SearchUserAdapter
    private lateinit var selectedUsersAdapter:SearchUserAdapter
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedCommunityProfileUri: Uri
    private lateinit var selectedCommunityBannerUri: Uri
    private var currentUserType = ""
    private var communityName: String = ""
    private var communityType: String = ""
    private var communityDesc: String = ""
    private var searchUserQuery = ""
    private var selectedUsersList: ArrayList<String> = ArrayList()
    private var isCommunityProfile = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Launcher for community profile pic
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    if (isCommunityProfile){
                        selectedCommunityProfileUri = data.data!!
                        Glide.with(this)
                            .load(selectedCommunityProfileUri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.communityProfileCIV)
                        Glide.with(this)
                            .load(selectedCommunityProfileUri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.communityProfileCIV2)
                    } else {
                        selectedCommunityBannerUri = data.data!!
                        Glide.with(this)
                            .load(selectedCommunityBannerUri)
                            .into(binding.communityBannerIV)
                    }
                }
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //First Layout that the user will see
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            currentUserType = userModel.userType
            changeLayout(1)
        }
    }

    private fun changeLayout(layoutNo:Int){
        binding.firstSectionLayout.visibility = View.GONE
        binding.secondSectionLayout.visibility = View.GONE
        binding.thirdSectionLayout.visibility = View.GONE
        binding.fourthSectionLayout.visibility = View.GONE


        when(layoutNo){
            1 -> bindFirstSectionLayout()
            2 -> bindSecondSectionLayout()
            3 -> bindThirdSectionLayout()
            4 -> bindFourthSectionLayout()
        }
    }

    //First Section Layout
    private fun bindFirstSectionLayout(){
        //makes the layout visible
        binding.firstSectionLayout.visibility = View.VISIBLE

        //replaces previousBtn with cancelBtn
        binding.previousBtn.visibility = View.GONE
        binding.cancelBtn.visibility = View.VISIBLE

        binding.communityNameEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = binding.communityNameEdtTxt.text.toString()
                isCommunityNameAvailable(name){isAvailable ->
                    if (isAvailable){
                        communityName = name
                    } else {
                        binding.communityNameEdtTxt.error = "Community name is not available"
                        communityName = ""
                    }
                }
            }
        })

        if (currentUserType == "Student"){
            //public account as default
            binding.privateRB.visibility = View.GONE
            binding.publicRB.isChecked = true
            communityType = "Public"

        } else if (currentUserType == "AppAdmin" || currentUserType == "Teacher"){
            binding.publicRB.setOnClickListener {
                binding.privateRB.isChecked = false
                communityType = "Public"
            }
            binding.privateRB.setOnClickListener {
                binding.publicRB.isChecked = false
                communityType = "Private"
            }
        }

        binding.nextBtn.setOnClickListener {
            if (communityName.isEmpty()){
                binding.communityNameEdtTxt.error = "Enter community name"
            } else if (AppUtil().containsBadWord(communityName)){
                binding.communityNameEdtTxt.error = "Your community name contains sensitive words"
            } else if (communityType.isEmpty()){
                Toast.makeText(this, "Select community type", Toast.LENGTH_SHORT).show()
            } else {
                communityName = binding.communityNameEdtTxt.text.toString()
                communityDesc = binding.communityDescEdtTxt.text.toString()
                changeLayout(2)
            }
        }
        binding.cancelBtn.setOnClickListener {
            onBackPressed()
        }
    }


    //Second Section Layout
    private fun bindSecondSectionLayout(){
        //makes the layout visible
        binding.secondSectionLayout.visibility = View.VISIBLE

        //replaces cancelBtn with previousBtn
        binding.cancelBtn.visibility = View.GONE
        binding.previousBtn.visibility = View.VISIBLE

        binding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchUserQuery = binding.searchEdtTxt.text.toString()
                searchUsers()
            }
        })

        binding.nextBtn.setOnClickListener {
            changeLayout(3)
        }
        binding.previousBtn.setOnClickListener {
            changeLayout(1)
        }
    }

    //Third Section Layout
    private fun bindThirdSectionLayout(){
        //makes the layout visible
        binding.thirdSectionLayout.visibility = View.VISIBLE

        isCommunityProfile = true
        binding.nameTV.text = communityName

        binding.communityProfileCIV.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        binding.nextBtn.setOnClickListener {
            changeLayout(4)
        }
        binding.previousBtn.setOnClickListener {
            binding.nextBtn.text = "Next"
            changeLayout(2)
        }
    }

    private fun bindFourthSectionLayout() {
        //makes the layout visible
        binding.fourthSectionLayout.visibility = View.VISIBLE

        isCommunityProfile = false
        binding.nextBtn.text = "Save"

        binding.communityBannerIV.setOnClickListener {
            ImagePicker.with(this)
                .crop(25f, 10f)
                .compress(1080)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        binding.nextBtn.setOnClickListener {
            createCommunity()
        }
        binding.previousBtn.setOnClickListener {
            binding.nextBtn.text = "Next"
            changeLayout(3)
        }

    }

    private fun createCommunity(){
        //loading
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        dialogLoadingBinding.messageTV.text = "Saving..."

        loadingDialog.show()

        var communityModel = CommunityModel()
        selectedUsersList
        //delay before heading to the community tab
        var delay:Long = 0

        FirebaseUtil().retrieveAllCommunityCollection().add(communityModel).addOnSuccessListener {
            //retrieve new communityId
            val communityId = it.id
            communityModel = CommunityModel(
                communityId = communityId,
                communityName = communityName,
                communityDescription = communityDesc,
                communityType = communityType,
                communityCode = generateRandomCode(),
                communityMembers = mapOf(FirebaseUtil().currentUserUid() to "Admin")
            )
            //save community profile to firebase storage
            if (::selectedCommunityProfileUri.isInitialized){

                var imageUrl = "${communityId}-${Timestamp.now()}"

                //delete the image from firebase storage
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                    var commmunity = it.toObject(CommunityModel::class.java)!!
                    if (commmunity.communityMedia.containsKey("community_photo")){
                        FirebaseUtil().retrieveCommunityProfilePicRef(commmunity.communityMedia["community_photo"]!!).delete()
                    }
                }

                //upload the image to firestore
                FirebaseUtil().retrieveCommunityProfilePicRef(imageUrl).putFile(selectedCommunityProfileUri).addOnSuccessListener {
                    val updates = hashMapOf<String, Any>(
                        "communityMedia.community_photo" to imageUrl
                    )
                    FirebaseUtil().retrieveCommunityDocument(communityId).update(updates).addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Image uploaded successfully")
                    }
                }
                delay += 1000
            }

            //save community banner to firebase storage
            if (::selectedCommunityBannerUri.isInitialized){

                var imageUrl = "${communityId}-${Timestamp.now()}"

                //delete the image from firebase storage
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {community ->
                    var communityModel = community.toObject(CommunityModel::class.java)!!
                    if (communityModel.communityMedia.containsKey("community_banner_photo")){
                        FirebaseUtil().retrieveCommunityBannerPicRef(communityModel.communityMedia["community_banner_photo"]!!).delete()
                    }
                }

                //upload the image to firestore
                FirebaseUtil().retrieveCommunityBannerPicRef(imageUrl).putFile(selectedCommunityBannerUri).addOnSuccessListener {
                    val updates = hashMapOf<String, Any>(
                        "communityMedia.community_banner_photo" to imageUrl
                    )
                    FirebaseUtil().retrieveCommunityDocument(communityId).update(updates).addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Image uploaded successfully")
                    }
                }
                delay += 1000
            }

            //set data to firestore
            FirebaseUtil().retrieveCommunityDocument(communityId).set(communityModel).addOnSuccessListener {
                //invites the selected users to community
                inviteUsersToCommunity(communityId)
                var chatroomModel = ChatroomModel()
                FirebaseUtil().retrieveAllChatRoomReferences().add(chatroomModel).addOnSuccessListener {chatroom ->
                    //Creates General channel for Community Chat
                    val chatroomModel = ChatroomModel(
                        chatroomName = "General",
                        chatroomId = chatroom.id,
                        chatroomType = "community_chat",
                        userIdList = listOf(FirebaseUtil().currentUserUid()),
                        lastMsgTimestamp = Timestamp.now(),
                        lastMessage = "created general channel",
                        lastMessageUserId = FirebaseUtil().currentUserUid(),
                        communityId = communityId
                    )
                    FirebaseUtil().retrieveChatRoomReference(chatroom.id).set(chatroomModel).addOnSuccessListener {
                        loadingDialog.dismiss()
                        AppUtil().headToMainActivity(this, "community", delay, communityId)
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                        loadingDialog.dismiss()
                    }
                }
            }.addOnFailureListener {e ->
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
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
            NotificationUtil().sendPushNotificationsForRequestsAndInvitations(this, user, "Community Invitation", communityName)
        }
    }

    private fun setupSelectedUsersRV(){
        if (selectedUsersList.isNotEmpty()){

            binding.selectedUsersLayout.visibility = View.VISIBLE
            binding.selectedUsersTV.text = "Selected Users (${selectedUsersList.size})"

            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", selectedUsersList)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            binding.selectedUsersRV.layoutManager = LinearLayoutManager(this)
            selectedUsersAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
            binding.selectedUsersRV.adapter = selectedUsersAdapter
            selectedUsersAdapter.startListening()

        } else {
            binding.selectedUsersTV.text = "Selected Users (0)"
            binding.selectedUsersLayout.visibility = View.GONE
        }
    }


    private fun searchUsers(){
        if (searchUserQuery.isNotEmpty()){
            if (searchUserQuery[0] == '@'){
                //search for username
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("username", searchUserQuery.removePrefix("@"))

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                binding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
                binding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            } else {
                //search for fullName
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("fullName", searchUserQuery)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                binding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
                binding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        } else {
            //query all users
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            binding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
            searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
            binding.searchedUsersRV.adapter = searchUserAdapter
            searchUserAdapter.startListening()
        }
    }

    private fun isCommunityNameAvailable(name: String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveAllCommunityCollection().whereEqualTo("communityName", name).get().addOnSuccessListener{
            if (!it.isEmpty){
                //community name already exists
                callback(false)
            } else {
                callback(true)
            }
        }.addOnFailureListener {
                callback(false)
        }
    }

    override fun onBackPressed() {
        if (communityName.isNotEmpty() || communityType.isNotEmpty()){
            //hides keyboard
            hideKeyboard()
            //create dialog
            val binding = DialogWarningMessageBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(binding.root))
                .setGravity(Gravity.CENTER)
                .setBackgroundColorResId(R.color.transparent)
                .create()

            binding.titleTV.text = "Exit Community Creation"
            binding.messageTV.text = "Do you want to exit Community Creation?"
            binding.yesBtn.setOnClickListener {
                super.onBackPressed()
            }
            binding.NoBtn.setOnClickListener {
                dialogPlus.dismiss()
            }
            dialogPlus.show()
        } else {
            super.onBackPressed()
        }


    }

    private fun generateRandomCode(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..10)
            .map { allowedChars.random() }
            .joinToString("")
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
    override fun onStart() {
        super.onStart()
        if (::searchUserAdapter.isInitialized){
            searchUserAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::searchUserAdapter.isInitialized){
            searchUserAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::searchUserAdapter.isInitialized){
            searchUserAdapter.stopListening()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }



}