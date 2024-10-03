package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.example.synthronize.databinding.ActivitySignUpBinding
import com.example.synthronize.databinding.DialogPrivacyPolicyBinding
import com.example.synthronize.databinding.DialogReportBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userModel: UserModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signUpBtn.setOnClickListener {
            val fullName = binding.fullNameEdtTxt.text.toString()
            val email = binding.emailEdtTxt.text.toString()
            val pass = binding.passwordEdtTxt.text.toString()
            val confirmPass = binding.confirmPassEdtTxt.text.toString()

            //Validates User info and credentials before registration
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your Full Name", Toast.LENGTH_SHORT).show()
            } else if(AppUtil().containsBadWord(fullName)) {
                Toast.makeText(this, "Your full name contains sensitive words", Toast.LENGTH_SHORT).show()
            } else if(email.isEmpty()) {
                Toast.makeText(this, "Please enter your Email", Toast.LENGTH_SHORT).show()
            } else if(pass.isEmpty()) {
                Toast.makeText(this, "Please enter your Password", Toast.LENGTH_SHORT).show()
            } else if(AppUtil().containsBadWord(pass)) {
                Toast.makeText(this, "Your password contains sensitive words", Toast.LENGTH_SHORT).show()
            } else if (pass.length < 6) {
                Toast.makeText(this, "Password should at least be more than 6 characters", Toast.LENGTH_SHORT).show()
            } else if(confirmPass.isEmpty()) {
                Toast.makeText(this, "Please enter your Confirm Password", Toast.LENGTH_SHORT).show()
            } else if (confirmPass != pass) {
                Toast.makeText(this, "Passwords are not matched", Toast.LENGTH_SHORT).show()
            } else if (!binding.dataPrivacyCB.isChecked) {
                Toast.makeText(this, "Please agree to the Privacy Policy to complete your registration.", Toast.LENGTH_SHORT).show()
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        //putting user info to UserModel
                        userModel = UserModel(
                            fullName =  fullName,
                            createdTimestamp = Timestamp.now(),
                            userID = FirebaseUtil().currentUserUid(),
                            userType = "Student" //user type student as default
                        )
                        //putting user info to FireStore
                        FirebaseUtil().currentUserDetails().set(userModel).addOnCompleteListener { it1 ->
                            if (it1.isSuccessful) {
                                Toast.makeText(this, "User Registration Is Successful", Toast.LENGTH_SHORT).show()
                                firebaseAuth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
                                    Toast.makeText(this, "Verification link has been sent to your email", Toast.LENGTH_LONG).show()
                                    val intent = Intent(this, Login::class.java)
                                    startActivity(intent)
                                    this.finish()
                                }
                            } else {
                                Toast.makeText(this, "User Registration failed, please try again", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "User Registration failed, please try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

        binding.loginTV.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            this.finish()
        }

        binding.privacyPolicyBtn.setOnClickListener {
            openPrivacyPolicy()
        }
    }

    private fun openPrivacyPolicy(){
        val dialogPrivacyPolicyBinding = DialogPrivacyPolicyBinding.inflate(layoutInflater)
        val dialogPrivacyPolicy = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogPrivacyPolicyBinding.root))
            .setCancelable(true)
            .setExpanded(false)
            .create()

        dialogPrivacyPolicyBinding.backBtn.setOnClickListener {
            dialogPrivacyPolicy.dismiss()
        }

        dialogPrivacyPolicy.show()
    }


}