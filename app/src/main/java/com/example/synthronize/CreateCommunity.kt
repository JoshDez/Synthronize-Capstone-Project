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
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CreateCommunity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityCreateCommunityBinding
    private lateinit var searchUserAdapter:SearchUserAdapter
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedCommunityProfileUri: Uri
    private lateinit var selectedCommunityBannerUri: Uri
    private var communityName: String = ""
    private var communityType: String = ""
    private var communityDesc: String = ""
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
        changeLayout(1)
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

        binding.publicRB.setOnClickListener {
            binding.privateRB.isChecked = false
            communityType = "Public"
        }

        binding.privateRB.setOnClickListener {
            binding.publicRB.isChecked = false
            communityType = "Private"
        }

        binding.nextBtn.setOnClickListener {
            if (communityName.isEmpty()){
                binding.communityNameEdtTxt.error = "Enter community name"
                Toast.makeText(this, "Enter community name", Toast.LENGTH_SHORT).show()
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
                val name = binding.searchEdtTxt.text.toString()
                searchUsers(name)
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
        var communityModel = CommunityModel()
        selectedUsersList
        //delay before heading to the community tab
        var delay:Long = 0

        //TODO: invite users in selectedUsersList to community


        FirebaseUtil().retrieveAllCommunityCollection().add(communityModel).addOnSuccessListener {
            //retrieve new communityId
            val communityId = it.id
            communityModel = CommunityModel(
                communityId = communityId,
                communityName = communityName,
                communityDescription = communityDesc,
                communityType = communityType,
                communityCode = generateRandomCode(),
                communityMembers = listOf(FirebaseUtil().currentUserUid()),
                communityAdmin = listOf(FirebaseUtil().currentUserUid())
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
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                    var commmunity = it.toObject(CommunityModel::class.java)!!
                    if (commmunity.communityMedia.containsKey("community_banner_photo")){
                        FirebaseUtil().retrieveCommunityBannerPicRef(commmunity.communityMedia["community_banner_photo"]!!).delete()
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

                //Creates General channel for Community Chat
                val chatroomID = "$communityId-General"
                val chatroomModel = ChatroomModel(
                    chatroomName = "General",
                    chatroomId = chatroomID,
                    chatroomType = "community_chat",
                    userIdList = communityModel.communityMembers,
                    lastMsgTimestamp = Timestamp.now(),
                    lastMessage = "",
                    lastMessageUserId = FirebaseUtil().currentUserUid()
                )
                FirebaseUtil().retrieveChatRoomReference(chatroomID).set(chatroomModel).addOnSuccessListener {
                    AppUtil().headToMainActivity(this, "community", delay, communityId)
                }
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
        }
    }


    private fun searchUsers(searchQuery:String){
        val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
            .whereGreaterThanOrEqualTo("fullName", searchQuery)

        val options: FirestoreRecyclerOptions<UserModel> =
            FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

        //set up searched users recycler view
        binding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
        searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUsersList)
        binding.searchedUsersRV.adapter = searchUserAdapter
        searchUserAdapter.startListening()
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
            val dialogPlusBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(dialogPlusBinding.root))
                .setGravity(Gravity.CENTER)
                .setMargin(50, 700, 50, 700)
                .create()

            dialogPlusBinding.titleTV.text = "Exit Community Creation"
            dialogPlusBinding.messageTV.text = "Do you want to exit Community Creation?"
            dialogPlusBinding.yesBtn.setOnClickListener {
                super.onBackPressed()
            }
            dialogPlusBinding.NoBtn.setOnClickListener {
                dialogPlus.dismiss()
            }
            dialogPlus.show()
        } else {
            super.onBackPressed()
        }


    }

    private fun generateRandomCode(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('_', '-', '@', '#', '$', '%', '^', '&')
        return (1..10)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onUserClick(uid: String, isChecked: Boolean) {
        //Interface for select user adapter
        if (isChecked) {
            //add user to selected user list
            selectedUsersList.add(uid)
        } else {
            //remove user to selected user list
            selectedUsersList.remove(uid)
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