package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.example.synthronize.databinding.ActivitySignUpBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogPrivacyPolicyBinding
import com.example.synthronize.databinding.DialogReportBinding
import com.example.synthronize.model.AccTypeRequestModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class SignUp : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userModel: UserModel
    private var userType = "Student" //default value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.studentRB.isChecked = true

        firebaseAuth = FirebaseAuth.getInstance()

        binding.nextBtn.setOnClickListener {
            binding.firstSectionLayout.visibility = View.GONE
            binding.secondSectionLayout.visibility = View.VISIBLE
        }

        binding.backBtn.setOnClickListener {
            binding.firstSectionLayout.visibility = View.VISIBLE
            binding.secondSectionLayout.visibility = View.GONE
        }

        binding.studentRB.setOnClickListener {
            if (binding.studentRB.isChecked){
                binding.teacherRB.isChecked = false
                binding.appAdminRB.isChecked = false
                userType = "Student"
                binding.warningMessageTV.visibility = View.INVISIBLE
            }
        }
        binding.teacherRB.setOnClickListener {
            if (binding.teacherRB.isChecked){
                binding.studentRB.isChecked = false
                binding.appAdminRB.isChecked = false
                userType = "Teacher"
                binding.warningMessageTV.visibility = View.VISIBLE
            }
        }
        binding.appAdminRB.setOnClickListener {
            if (binding.appAdminRB.isChecked){
                binding.teacherRB.isChecked = false
                binding.studentRB.isChecked = false
                userType = "AppAdmin"
                binding.warningMessageTV.visibility = View.VISIBLE
            }
        }

        binding.signUpBtn.setOnClickListener {
            //loading
            val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
            val loadingDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(dialogLoadingBinding.root))
                .setCancelable(false)
                .setBackgroundColorResId(R.color.transparent)
                .setGravity(Gravity.CENTER)
                .create()

            dialogLoadingBinding.messageTV.text = "Loading..."

            loadingDialog.show()


            val fullName = binding.fullNameEdtTxt.text.toString()
            val email = binding.emailEdtTxt.text.toString()
            val pass = binding.passwordEdtTxt.text.toString()
            val confirmPass = binding.confirmPassEdtTxt.text.toString()

            //Validates User info and credentials before registration
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your Full Name", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if(AppUtil().containsBadWord(fullName)) {
                Toast.makeText(this, "Your full name contains sensitive words", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if(email.isEmpty()) {
                Toast.makeText(this, "Please enter your Email", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if(pass.isEmpty()) {
                Toast.makeText(this, "Please enter your Password", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if(AppUtil().containsBadWord(pass)) {
                Toast.makeText(this, "Your password contains sensitive words", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if (pass.length < 6) {
                Toast.makeText(this, "Password should at least be more than 6 characters", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if(confirmPass.isEmpty()) {
                Toast.makeText(this, "Please enter your Confirm Password", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if (confirmPass != pass) {
                Toast.makeText(this, "Passwords are not matched", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else if (!binding.dataPrivacyCB.isChecked) {
                Toast.makeText(this, "Please agree to the Privacy Policy to complete your registration.", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        //putting user info to UserModel
                        userModel = UserModel(
                            fullName =  fullName,
                            createdTimestamp = Timestamp.now(),
                            userID = FirebaseUtil().currentUserUid(),
                            userType = "Student"
                        )
                        //putting user info to FireStore
                        FirebaseUtil().currentUserDetails().set(userModel).addOnCompleteListener { it1 ->
                            if (it1.isSuccessful) {
                                loadingDialog.dismiss()
                                Toast.makeText(this, "User Registration Is Successful", Toast.LENGTH_SHORT).show()
                                firebaseAuth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
                                    Toast.makeText(this, "Verification link has been sent to your email", Toast.LENGTH_LONG).show()
                                    val intent = Intent(this, Login::class.java)
                                    startActivity(intent)
                                    this.finish()
                                }

                                if (userType != "Student"){
                                    val tempModel = AccTypeRequestModel()
                                    FirebaseUtil().retrieveAllUserTypeRequests().add(tempModel).addOnSuccessListener {acc ->
                                        val accTypeModel = AccTypeRequestModel(
                                            requestId = acc.id,
                                            userId = firebaseAuth.currentUser!!.uid,
                                            reqAccType = userType
                                        )
                                        FirebaseUtil().retrieveAllUserTypeRequests().document(accTypeModel.requestId).set(accTypeModel)
                                    }

                                }
                            } else {
                                Toast.makeText(this, "User Registration failed, please try again", Toast.LENGTH_SHORT).show()
                                loadingDialog.dismiss()
                            }
                        }
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        loadingDialog.dismiss()
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