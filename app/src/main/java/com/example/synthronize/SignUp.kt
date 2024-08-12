package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.example.synthronize.databinding.ActivitySignUpBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil

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
                Toast.makeText(this, "Enter Your Full Name", Toast.LENGTH_SHORT).show()
            } else if(email.isEmpty()) {
                Toast.makeText(this, "Enter Your Email", Toast.LENGTH_SHORT).show()
            } else if(pass.isEmpty()) {
                Toast.makeText(this, "Enter Your Password", Toast.LENGTH_SHORT).show()
            } else if (pass.length < 6) {
                Toast.makeText(this, "Password should at least be more than 6 characters", Toast.LENGTH_SHORT).show()
            } else if(confirmPass.isEmpty()) {
                Toast.makeText(this, "Enter Your Confirm Password", Toast.LENGTH_SHORT).show()
            } else if (confirmPass != pass) {
                Toast.makeText(this, "Passwords Are Not Matched", Toast.LENGTH_SHORT).show()
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
    }
}