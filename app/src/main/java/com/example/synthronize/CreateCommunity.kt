package com.example.synthronize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.adapters.SelectUsersAdapter
import com.example.synthronize.databinding.ActivityCreateCommunityBinding
import com.example.synthronize.databinding.DialogAddCommunityBinding
import com.example.synthronize.databinding.DialogSaveUserBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CreateCommunity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityCreateCommunityBinding
    private lateinit var selectUsersAdapter:SelectUsersAdapter
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedCommunityProfileUri: Uri
    private var communityName: String = ""
    private var communityType: String = ""
    private var communityDesc: String = ""
    private var selectedUsersList: ArrayList<String> = ArrayList()

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
                    selectedCommunityProfileUri = data.data!!
                    Glide.with(this)
                        .load(selectedCommunityProfileUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.communityProfileCIV)
                }
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        changeLayout(1)
    }

    private fun changeLayout(layoutNo:Int){
        binding.firstSectionLayout.visibility = View.GONE
        binding.secondSectionLayout.visibility = View.GONE
        binding.thirdSectionLayout.visibility = View.GONE


        when(layoutNo){
            1 -> bindFirstSectionLayout()
            2 -> bindSecondSectionLayout()
            3 -> bindThirdSectionLayout()
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
            //TODO: add dialog
            this.finish()
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

        binding.nameTV.text = communityName
        binding.nextBtn.text = "Save"




        binding.communityProfileCIV.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }


        binding.nextBtn.setOnClickListener {
            createCommunity()
        }
        binding.previousBtn.setOnClickListener {
            binding.nextBtn.text = "Next"
            changeLayout(2)
        }
    }

    private fun createCommunity(){
        var communityModel = CommunityModel()
        selectedUsersList.add(FirebaseUtil().currentUserUid())

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
                communityMembers = selectedUsersList,
                communityAdmin = listOf(FirebaseUtil().currentUserUid())
            )
            //save community profile to firebase storage
            if (::selectedCommunityProfileUri.isInitialized){
                FirebaseUtil().retrieveCommunityProfilePicRef(communityId).putFile(selectedCommunityProfileUri)
            }

            //set data to firestore
            FirebaseUtil().retrieveCommunityDocument(communityId).set(communityModel).addOnSuccessListener {
                AppUtil().headToMainActivity(this, "community", 0, communityId)
            }
        }
    }


    private fun searchUsers(searchQuery:String){
        val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
            .whereGreaterThanOrEqualTo("fullName", searchQuery)

        val options: FirestoreRecyclerOptions<UserModel> =
            FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

        //set up searched users recycler view
        binding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
        selectUsersAdapter = SelectUsersAdapter(this, this, selectedUsersList, options)
        binding.searchedUsersRV.adapter = selectUsersAdapter
        selectUsersAdapter.startListening()
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
            //create dialog
            val dialogPlusBinding = DialogSaveUserBinding.inflate(layoutInflater)
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

    override fun onItemClick(uid: String, isChecked: Boolean) {
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
        if (::selectUsersAdapter.isInitialized){
            selectUsersAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::selectUsersAdapter.isInitialized){
            selectUsersAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::selectUsersAdapter.isInitialized){
            selectUsersAdapter.stopListening()
        }
    }



}