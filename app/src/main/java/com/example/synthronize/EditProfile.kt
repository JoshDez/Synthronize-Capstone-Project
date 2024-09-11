package com.example.synthronize

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.example.synthronize.databinding.ActivityEditProfileBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.util.Calendar

class EditProfile : AppCompatActivity() {
    private lateinit var binding:ActivityEditProfileBinding
    private lateinit var dialogBinding: DialogWarningMessageBinding
    private lateinit var userModel: UserModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedProfilePicUri:Uri
    private lateinit var selectedProfileCoverPicUri:Uri
    private lateinit var selectedImageUri:Uri
    private var isUsernameValid = true
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
    private fun isModified(): Boolean {
        return binding.fullNameEdtTxt.text.toString() != userModel.fullName ||
                binding.usernameEdtTxt.text.toString() != userModel.username ||
                binding.descriptionEdtTxt.text.toString() != userModel.description ||
                binding.birthdayEdtTxt.text.toString() != userModel.birthday ||
                ::selectedProfilePicUri.isInitialized || ::selectedProfileCoverPicUri.isInitialized
    }
    private fun retrieveAndBindCurrentUserDetails() {
        FirebaseUtil().currentUserDetails().get().addOnCompleteListener {
            if (it.isSuccessful && it.result.exists()){

                userModel = it.result.toObject(UserModel::class.java)!!

                //bind user details
                binding.fullNameEdtTxt.setText(userModel.fullName)
                binding.usernameEdtTxt.setText(userModel.username)
                binding.descriptionEdtTxt.setText(userModel.description)
                binding.birthdayEdtTxt.setText(userModel.birthday)

                //adds text watcher to username edit text to validate username
                bindUsernameEdtTxtTextWatcher(userModel.username)

                //bind user profile picture
                AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.userProfileCIV)
                //bind user cover picture
                AppUtil().setUserCoverPic(this, FirebaseUtil().currentUserUid(), binding.userCoverIV)

            }
        }
    }

    private fun validateUserProfileDetails() {

        if (binding.fullNameEdtTxt.text.toString().isEmpty()) {
            binding.fullNameEdtTxt.error = "full name should not be blank"

        } else if (isUsernameValid) {

            //TODO: Loading to be implemented
            //Set User Details to userModel
            userModel.fullName = binding.fullNameEdtTxt.text.toString()
            userModel.username = binding.usernameEdtTxt.text.toString().lowercase()
            userModel.description = binding.descriptionEdtTxt.text.toString()
            userModel.birthday = binding.birthdayEdtTxt.text.toString()
            setCurrentUserDetailsToFirebase()
        }
    }

    private fun setCurrentUserDetailsToFirebase() {
        if (::userModel.isInitialized){
            var delay:Long = 0
            //set new user profile pic
            if (::selectedProfilePicUri.isInitialized){

                var imageUrl = "${FirebaseUtil().currentUserUid()}-${Timestamp.now()}"

                //delete the image from firebase storage
                FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
                    var user = it.toObject(UserModel::class.java)!!
                    if (user.userMedia.containsKey("profile_photo")){
                        FirebaseUtil().retrieveUserProfilePicRef(user.userMedia["profile_photo"]!!).delete()
                    }
                }

                //upload the image to firestore
                FirebaseUtil().retrieveUserProfilePicRef(imageUrl).putFile(selectedProfilePicUri).addOnSuccessListener {
                    val updates = hashMapOf<String, Any>(
                        "userMedia.profile_photo" to imageUrl
                    )
                    FirebaseUtil().currentUserDetails().update(updates).addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Image uploaded successfully")
                    }
                }
                //adds a second to give time for the firebase to upload
                delay += 3000
            }
            //set new user cover pic
            if (::selectedProfileCoverPicUri.isInitialized){
                var imageUrl = "${FirebaseUtil().currentUserUid()}-${Timestamp.now()}"

                //delete the cover image from firebase storage
                FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
                    var user = it.toObject(UserModel::class.java)!!
                    if (user.userMedia.containsKey("profile_cover_photo")){
                        FirebaseUtil().retrieveUserCoverPicRef(user.userMedia["profile_cover_photo"]!!).delete()
                    }
                }

                //upload the image to firestore
                FirebaseUtil().retrieveUserCoverPicRef(imageUrl).putFile(selectedProfileCoverPicUri).addOnSuccessListener {
                    val updates = hashMapOf<String, Any>(
                        "userMedia.profile_cover_photo" to imageUrl
                    )
                    FirebaseUtil().currentUserDetails().update(updates).addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Cover Image uploaded successfully")
                    }
                }
                //adds a second to give time for the firebase to upload
                delay += 3000
            }

            //set new user model
            FirebaseUtil().currentUserDetails().set(userModel).addOnCompleteListener {
                if (it.isSuccessful){
                    Toast.makeText(this, "User details successfully updated", Toast.LENGTH_SHORT).show()
                    //heads back to main activity with a profile fragment
                    AppUtil().headToMainActivity(this, "profile", delay)
                } else {
                    Toast.makeText(this, "Error in updating user details, please try again", Toast.LENGTH_SHORT).show()
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
                .setBackgroundColorResId(R.color.transparent)
                .setCancelable(true)
                .create()
            dialogBinding.yesBtn.setOnClickListener {
                validateUserProfileDetails()
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

                validateUserProfileDetails()
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
        binding.birthdayEdtTxt.setOnClickListener {
            val calendar = Calendar.getInstance()
            val calYear = calendar.get(Calendar.YEAR)
            val calMonth = calendar.get(Calendar.MONTH)
            val calDay = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, DatePickerDialog.OnDateSetListener{_,
                selectedYear, selectedMonth, selectedDay ->

                binding.birthdayEdtTxt.setText("${selectedMonth + 1}/$selectedDay/$selectedYear")

            }, calYear, calMonth, calDay).show()
        }
    }
    private fun bindUsernameEdtTxtTextWatcher(currentUsername: String){

        binding.usernameEdtTxt.addTextChangedListener( object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {

                val username = binding.usernameEdtTxt.text.toString().lowercase()

                if (username != currentUsername){
                    if (username.length < 3){
                        binding.usernameEdtTxt.error = "username should be more than 3 characters"
                        isUsernameValid = false
                    } else if (isUsernameContainsSpecialCharacters(username)){
                        binding.usernameEdtTxt.error = "username should not contain special characters"
                        isUsernameValid = false
                    } else {
                        isUsernameNotAvailable(username){usernameNotAvailable ->
                            if (usernameNotAvailable){
                                binding.usernameEdtTxt.error = "Username is not available"
                                isUsernameValid = false
                            } else {
                                isUsernameValid = true
                            }
                        }
                    }
                } else {
                    isUsernameValid = true
                }
            }

        })
    }

    private fun isUsernameNotAvailable(username: String, callback: (Boolean) -> Unit) {
        FirebaseUtil().allUsersCollectionReference().whereEqualTo("username", username).get().addOnCompleteListener{
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

    private fun isUsernameContainsSpecialCharacters(username: String): Boolean {
        val pattern = Regex("[a-zA-Z0-9_-]+")
        return !pattern.matches(username)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }
}