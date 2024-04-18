package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.synthronize.databinding.ActivityLoginBinding

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
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful){
                        if (firebaseAuth.currentUser?.isEmailVerified == true) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            this.finish()
                        } else {
                            Toast.makeText(this, "Your email is not yet verified", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
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
}