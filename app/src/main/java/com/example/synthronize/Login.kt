package com.example.synthronize

import UserLastSeenUpdater
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.synthronize.databinding.ActivityLoginBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginBtn.setOnClickListener {
            val email = binding.emailEdtTxt.text.toString()
            val pass = binding.passwordEdtTxt.text.toString()

            //validate credentials
            if (email.isEmpty()){
                Toast.makeText(this, "Enter your Email", Toast.LENGTH_SHORT).show()
            } else if (pass.isEmpty()){
                Toast.makeText(this, "Enter your Password", Toast.LENGTH_SHORT).show()
            } else {
                signInUser(email, pass)
            }
        }

        binding.resetPassTV.setOnClickListener{
            val email = binding.emailEdtTxt.text.toString()

            //finds the email before sending password reset link to email
            if (email.isEmpty()){
                Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
            }else {
                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener {
                    if (it.isSuccessful){
                        Toast.makeText(this, "Password reset link is sent to your email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.signUpTV.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
            this.finish()
        }
    }

    private fun signInUser(email:String, pass:String){
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

        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful){
                if (firebaseAuth.currentUser?.isEmailVerified == true) {
                    loadingDialog.dismiss()

                    FirebaseUtil().currentUserDetails().get().addOnCompleteListener {currentUser ->
                        if (currentUser.result.exists()){
                            val userModel = currentUser.result.toObject(UserModel::class.java)!!
                            if (userModel.userAccess.containsKey("Disabled")){
                                if(userModel.userAccess["Disabled"].toString().isNotEmpty()){
                                    val date = userModel.userAccess["Disabled"]
                                    openWarningDialog("Banned Account", "Your account has been banned until $date")
                                } else {
                                    openWarningDialog("Deactivated Account", "Your account is currently deactivated, do you want to activate it?")
                                }
                            } else {
                                //starts updating user last seen
                                UserLastSeenUpdater().startUpdating()
                                //head to main activity
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                this.finish()
                            }
                        }
                    }



                } else {
                    Toast.makeText(this, "Your email is not yet verified", Toast.LENGTH_SHORT).show()
                }

            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun openWarningDialog(title:String, message:String){
        val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val warningDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(warningBinding.root))
            .setCancelable(true)
            .setExpanded(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()
        warningBinding.titleTV.text = title
        warningBinding.messageTV.text = message

        if (title != "Deactivated Account"){
            warningBinding.NoBtn.visibility = View.GONE
            warningBinding.yesBtn.visibility = View.GONE
        } else {
            warningBinding.yesBtn.setOnClickListener {
                warningDialog.dismiss()
                val updates = mapOf(
                    "userAccess.Disabled" to FieldValue.delete(),
                    "userAccess.Enabled" to ""
                )
                FirebaseUtil().currentUserDetails().update(updates).addOnSuccessListener {
                    //head to main activity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    this.finish()
                }
            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
            }
        }

        warningDialog.show()
    }
}