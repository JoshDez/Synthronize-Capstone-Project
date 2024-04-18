package com.example.synthronize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.example.synthronize.databinding.ActivityEditProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.ModelHandler

class EditProfile : AppCompatActivity() {
    private lateinit var binding:ActivityEditProfileBinding
    private lateinit var userModel: UserModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedProfilePicUri:Uri
    private lateinit var selectedProfileCoverPicUri:Uri
    private lateinit var selectedImageUri:Uri
    private var isProfilePic = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Launcher for user profile pic and user cover pic
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    selectedImageUri = data.data!!
                    if (isProfilePic){
                        //bind new profile pic
                        selectedProfilePicUri = selectedImageUri
                        Glide.with(this).load(selectedProfilePicUri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.userProfileCIV)
                    } else {
                        //bind new cover pic
                        selectedProfileCoverPicUri = selectedImageUri
                        Glide.with(this).load(selectedProfileCoverPicUri)
                            .into(binding.userCoverIV)
                    }
                }
            }
        }

        retrieveAndBindCurrentUserDetails()
        bindSetOnClickListeners()
    }

    private fun bindSetOnClickListeners(){
        binding.backBtn.setOnClickListener {
            if (isModified()){
                //TODO: Dialog to be implemented for saving
                validateUserProfile()
            } else {
                this.finish()
            }
        }

        binding.saveBtn.setOnClickListener {
            //TODO: Loading start to be implemented
            if (isModified())
                validateUserProfile()
            else
                this.finish()
        }

        binding.userProfileCIV.setOnClickListener {
            isProfilePic = true
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        binding.userCoverIV.setOnClickListener {
            isProfilePic = false
            ImagePicker.with(this)
                .crop(25f, 10f)
                .compress(1080)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }

        }
    }


    private fun validateUserProfile() {
        if (binding.fullNameEdtTxt.text.toString().isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
            //TODO: red tint in edtTxt to be implemented
        } else if (binding.usernameEdtTxt.text.toString().isEmpty()) {
            Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
            //TODO: red tint in edtTxt to be implemented

            //TODO:Birthday should be isEmpty
        } else if (binding.birthdayEdtTxt.text.toString().isNotEmpty()) {
            Toast.makeText(this, "Please enter your birthday", Toast.LENGTH_SHORT).show()
            //TODO: red tint in edtTxt to be implemented
        } else {
            //Loading to be implemented

            //Set User Details to userModel
            userModel.fullName = binding.fullNameEdtTxt.text.toString()
            userModel.username = binding.usernameEdtTxt.text.toString()
            userModel.description = binding.descriptionEdtTxt.text.toString()
            //TODO:Birthday
            userModel.birthday = binding.birthdayEdtTxt.text.toString()
            setCurrentUserDetailsToFirebase()
        }
    }

    private fun setCurrentUserDetailsToFirebase() {
        var delay:Long = 0
        //set new user profile pic
        if (::selectedProfilePicUri.isInitialized){
            FirebaseUtil().retrieveUserProfilePicRef(FirebaseUtil().currentUserUid()).putFile(selectedProfilePicUri)
            //adds a second to give time for the firebase to upload
            delay += 1000
        }
        //set new user cover pic
        if (::selectedProfileCoverPicUri.isInitialized){
            FirebaseUtil().retrieveUserCoverPicRef(FirebaseUtil().currentUserUid()).putFile(selectedProfileCoverPicUri)
            //adds a second to give time for the firebase to upload
            delay += 1000
        }

        //set new user model
        FirebaseUtil().currentUserDetails().set(userModel).addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(this, "User details successfully updated", Toast.LENGTH_SHORT).show()
                backToProfile(delay)
            } else {
                Toast.makeText(this, "Error in updating user details, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun retrieveAndBindCurrentUserDetails() {
        ModelHandler().retrieveUserModel(FirebaseUtil().currentUserUid()) {result ->
            //bind user details
            userModel = result
            binding.fullNameEdtTxt.setText(userModel.fullName)
            binding.usernameEdtTxt.setText(userModel.username)
            binding.birthdayEdtTxt.setText(userModel.birthday)
            binding.descriptionEdtTxt.setText(userModel.description)

            //bind user profile picture
            AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.userProfileCIV)
            //bind user cover picture
            AppUtil().setUserCoverPic(this, FirebaseUtil().currentUserUid(), binding.userCoverIV)

        }
    }

    private fun isModified(): Boolean {
        return binding.fullNameEdtTxt.text.toString() != userModel.fullName ||
            binding.usernameEdtTxt.text.toString() != userModel.username ||
            binding.descriptionEdtTxt.text.toString() != userModel.description ||
            binding.birthdayEdtTxt.text.toString() != userModel.birthday ||
                ::selectedProfilePicUri.isInitialized || ::selectedProfileCoverPicUri.isInitialized
    }
    private fun backToProfile(delay:Long) {
        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fragment", "profile")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }, delay)
    }
}