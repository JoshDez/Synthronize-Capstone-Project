package com.example.synthronize

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityAccountManagementBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class AccountManagement : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var context: Context
    private lateinit var binding: ActivityAccountManagementBinding // For the main activity binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate the binding for the main layout
        binding = ActivityAccountManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Using binding instead of findViewById
        binding.changePasswordBtn.setOnClickListener {
            sendPasswordResetEmail()
        }

        binding.deactivateAccountBtn.setOnClickListener {
            val menuBinding = DialogMenuBinding.inflate(layoutInflater)
            val menuDialog = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(menuBinding.root))
                .setMargin(400, 0, 0, 0)
                .setBackgroundColorResId(R.color.transparent)
                .setGravity(Gravity.TOP)
                .setCancelable(true)
                .create()

            menuDialog.dismiss()
            Handler().postDelayed({
                deactivateWarningDialog()
            }, 500)
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun sendPasswordResetEmail() {
        val userEmail = auth.currentUser?.email

        if (userEmail != null) {
            auth.sendPasswordResetEmail(userEmail)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(this, "User email not found.", Toast.LENGTH_LONG).show()
        }
    }

    private fun deactivateWarningDialog(){
        context = this
        val warningDialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val warningDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(warningDialogBinding.root))
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .setCancelable(true)
            .create()

        warningDialogBinding.titleTV.text = "Deactivate Account"
        warningDialogBinding.messageTV.text = "Do you want to deactivate your account? (You can reactivate your account by signing in)"

        warningDialogBinding.yesBtn.setOnClickListener {
            val updates = mapOf(
                "userAccess.Disabled" to "",
                "userAccess.Enabled" to FieldValue.delete()
            )
            FirebaseUtil().currentUserDetails().update(updates).addOnSuccessListener {
                // Logout user
                warningDialog.dismiss()
                FirebaseUtil().logoutUser(context, layoutInflater)
            }
        }

        warningDialogBinding.NoBtn.setOnClickListener {
            warningDialog.dismiss()
        }

        warningDialog.show()
    }
}
