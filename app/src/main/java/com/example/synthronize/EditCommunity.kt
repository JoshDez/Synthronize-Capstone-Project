package com.example.synthronize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.example.synthronize.databinding.ActivityEditCommunityBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder


class EditCommunity : AppCompatActivity() {
    private lateinit var binding: ActivityEditCommunityBinding
    private lateinit var dialogBinding: DialogWarningMessageBinding
    private lateinit var communityModel: CommunityModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedCommunityProfileUri: Uri
    private lateinit var selectedBannerPicUri: Uri
    private lateinit var selectedImageUri: Uri
    private lateinit var communityId:String
    private var currentCommunityType = ""
    private var isCommunityNameValid = true
    private var isProfilePic = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()


        //Launcher for user profile pic and user cover pic
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    selectedImageUri = data.data!!
                    if (isProfilePic){
                        //bind new profile pic
                        selectedCommunityProfileUri = selectedImageUri
                        Glide.with(this).load(selectedCommunityProfileUri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.communityProfileCIV)
                    } else {
                        //bind new cover pic
                        selectedBannerPicUri = selectedImageUri
                        Glide.with(this).load(selectedBannerPicUri)
                            .into(binding.communityBannerIV)
                    }
                }
            }
        }

        retrieveAndBindCommunityDetails()
        bindSetOnClickListeners()
    }
    
    private fun isModified(): Boolean {
        return binding.communityNameEdtTxt.text.toString() != communityModel.communityName ||
                binding.communityDescEdtTxt.text.toString() != communityModel.communityDescription ||
                currentCommunityType != communityModel.communityType ||
                ::selectedCommunityProfileUri.isInitialized || ::selectedBannerPicUri.isInitialized
    }
    private fun retrieveAndBindCommunityDetails() {
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()){

                communityModel = it.result.toObject(CommunityModel::class.java)!!

                //bind community details
                binding.communityNameEdtTxt.setText(communityModel.communityName)
                binding.communityDescEdtTxt.setText(communityModel.communityDescription)

                currentCommunityType = communityModel.communityType
                if (currentCommunityType == "Private"){
                    binding.privateRB.isChecked = true
                } else {
                    binding.publicRB.isChecked = true
                }

                //adds text watcher to username edit text to validate username
                bindCommunityNameEdtTxtTextWatcher(communityModel.communityName)

                //bind profile picture
                AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.communityProfileCIV, true)
                //bind banner picture
                AppUtil().setCommunityBannerPic(this, communityModel.communityId, binding.communityBannerIV, true)

            }
        }
    }

    private fun validateCommunityDetails() {

        if (binding.communityNameEdtTxt.text.toString().isEmpty()) {
            binding.communityNameEdtTxt.error = "full name should not be blank"

        } else if (isCommunityNameValid) {

            //TODO: Loading to be implemented
            //Set Community Details to userModel
            communityModel.communityName = binding.communityNameEdtTxt.text.toString()
            communityModel.communityDescription = binding.communityDescEdtTxt.text.toString()
            communityModel.communityType = currentCommunityType
            setCommunityDetailsToFirebase()
        }
    }

    private fun setCommunityDetailsToFirebase() {
        if (::communityModel.isInitialized){
            var delay:Long = 0
            //set new user profile pic
            if (::selectedCommunityProfileUri.isInitialized){
                FirebaseUtil().retrieveCommunityProfilePicRef(communityModel.communityId).putFile(selectedCommunityProfileUri).addOnSuccessListener {

                }
                //adds a second to give time for the firebase to upload
                delay += 3000
            }
            //set new user cover pic
            if (::selectedBannerPicUri.isInitialized){
                //TODO to implement banner
                FirebaseUtil().retrieveCommunityBannerPicRef(communityModel.communityId).putFile(selectedBannerPicUri)
                //adds a second to give time for the firebase to upload
                delay += 3000
            }

            //set new user model
            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId).set(communityModel).addOnCompleteListener {
                if (it.isSuccessful){
                    Toast.makeText(this, "Community details successfully updated", Toast.LENGTH_SHORT).show()
                    //heads back to main activity with a profile fragment
                    AppUtil().headToMainActivity(this, "community", delay, communityId)
                } else {
                    Toast.makeText(this, "Error in updating community details, please try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (isModified()){
            //hides keyboard
            hideKeyboard()
            //Dialog for saving user profile
            dialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(dialogBinding.root))
                .setGravity(Gravity.CENTER)
                .setMargin(50, 800, 50, 800)
                .setCancelable(true)
                .create()
            dialogBinding.yesBtn.setOnClickListener {
                dialogPlus.dismiss()
                validateCommunityDetails()
            }
            dialogBinding.NoBtn.setOnClickListener {
                super.onBackPressed()
            }

            dialogPlus.show()

        } else {
            super.onBackPressed()
        }
    }
    private fun bindSetOnClickListeners(){
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.saveBtn.setOnClickListener {
            //TODO: Loading start to be implemented
            if (isModified())
                validateCommunityDetails()
            else
                this.finish()
        }

        binding.communityProfileCIV.setOnClickListener {
            isProfilePic = true
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        binding.communityBannerIV.setOnClickListener {
            isProfilePic = false
            ImagePicker.with(this)
                .crop(25f, 10f)
                .compress(1080)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        binding.publicRB.setOnClickListener {
            binding.privateRB.isChecked = false
            currentCommunityType = "Public"
        }

        binding.privateRB.setOnClickListener {
            binding.publicRB.isChecked = false
            currentCommunityType = "Private"
        }
    }
    private fun bindCommunityNameEdtTxtTextWatcher(currentUsername: String){

        binding.communityNameEdtTxt.addTextChangedListener( object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {

                val name = binding.communityNameEdtTxt.text.toString().lowercase()

                if (name != currentUsername){
                    if (name.isEmpty()){
                        binding.communityNameEdtTxt.error = "Community name should not be empty"
                        isCommunityNameValid = false
                    } else {
                        isCommunityNameNotAvailable(name){ communityNameNotAvailable ->
                            if (communityNameNotAvailable){
                                binding.communityNameEdtTxt.error = "Community name not available"
                                isCommunityNameValid = false
                            } else {
                                isCommunityNameValid = true
                            }
                        }
                    }
                } else {
                    isCommunityNameValid = true
                }
            }

        })
    }

    private fun isCommunityNameNotAvailable(communityName: String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveAllCommunityCollection().whereEqualTo("communityName", communityName).get().addOnCompleteListener{
            if (it.isSuccessful){
                if (!it.result.isEmpty){
                    //username already exists
                    callback(true)
                } else {
                    callback(false)
                }
            }else {
                callback(false)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }
}