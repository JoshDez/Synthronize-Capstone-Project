package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityAppSettingsBinding
import com.example.synthronize.databinding.DialogFeedbackBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogPrivacyPolicyBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.FeedbackModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class AppSettings : AppCompatActivity() {
    private lateinit var binding: ActivityAppSettingsBinding
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize context
        context = this

        // Set up back button functionality
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.feedback.setOnClickListener {
            openFeedbackDialog()
        }

        // Open reports filed activity
        binding.reportsFiledBtn.setOnClickListener {
            val intent = Intent(this, Reports::class.java)
            intent.putExtra("isPersonalReport", true)
            startActivity(intent)
        }

        // Set up logout button functionality
        binding.logout.setOnClickListener {
            val menuBinding = DialogMenuBinding.inflate(layoutInflater)
            val menuDialog = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(menuBinding.root))
                .setMargin(400, 0, 0, 0)
                .setBackgroundColorResId(R.color.transparent)
                .setGravity(Gravity.TOP)
                .setCancelable(true)
                .create()

            menuDialog.dismiss()

            // Add a small delay before showing the logout confirmation dialog
            Handler().postDelayed({
                logoutWarningDialog()
            }, 500)
        }
        binding.privacy.setOnClickListener {
            openPrivacyPolicy()
        }
        binding.accounts.setOnClickListener{
            val intent = Intent(this, AccountManagement::class.java)
            startActivity(intent)
        }
    }

    private fun openFeedbackDialog() {
        val dialogFeedbackBinding = DialogFeedbackBinding.inflate(layoutInflater)
        val dialogFeedback = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogFeedbackBinding.root))
            .setCancelable(true)
            .setExpanded(false)
            .setGravity(Gravity.BOTTOM)
            .create()
        
        var feedbackType = ""
        var reason = ""

        //options
        dialogFeedbackBinding.bugsRB.setOnClickListener {
            feedbackType = "Bugs"
            unselectRadioButtons(dialogFeedbackBinding, dialogFeedbackBinding.bugsRB)
        }
        dialogFeedbackBinding.glitchesRB.setOnClickListener {
            feedbackType = "Glitches"
            unselectRadioButtons(dialogFeedbackBinding, dialogFeedbackBinding.glitchesRB)
        }
        dialogFeedbackBinding.errorsRB.setOnClickListener {
            feedbackType = "Errors"
            unselectRadioButtons(dialogFeedbackBinding, dialogFeedbackBinding.errorsRB)
        }
        dialogFeedbackBinding.othersRB.setOnClickListener {
            feedbackType = "Others"
            unselectRadioButtons(dialogFeedbackBinding, dialogFeedbackBinding.othersRB)
        }
        
        //Submit Button
        dialogFeedbackBinding.submitBtn.setOnClickListener {
            reason = dialogFeedbackBinding.reasonEdtTxt.text.toString()
            if (feedbackType.isEmpty()){
                Toast.makeText(context, "Please select feedback type", Toast.LENGTH_SHORT).show()
            } else if(reason.isEmpty()){
                Toast.makeText(context, "Please write your reason", Toast.LENGTH_SHORT).show()
            } else if(AppUtil().containsBadWord(reason)){
                Toast.makeText(context, "The reason contains sensitive words", Toast.LENGTH_SHORT).show()
            } else {
                val temp = FeedbackModel()
                FirebaseUtil().retrieveFeedbacksCollection().add(temp).addOnSuccessListener {
                    val feedbackModel = FeedbackModel(
                        feedbackID = it.id,
                        feedbackType = feedbackType,
                        feedbackMessage = reason
                    )
                    FirebaseUtil().retrieveFeedbacksCollection().document(feedbackModel.feedbackID).set(feedbackModel).addOnSuccessListener {
                        Toast.makeText(context, "Your feedback has been sent", Toast.LENGTH_SHORT).show()
                        dialogFeedback.dismiss()
                    }.addOnFailureListener {
                        Toast.makeText(context, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialogFeedbackBinding.downBtn.setOnClickListener {
            dialogFeedback.dismiss()
        }

        dialogFeedback.show()


    }

    private fun unselectRadioButtons(dialogFeedbackBinding: DialogFeedbackBinding, radioButton: RadioButton) {
        dialogFeedbackBinding.reasonEdtTxt.visibility = View.VISIBLE
        if (radioButton != dialogFeedbackBinding.bugsRB){
            dialogFeedbackBinding.bugsRB.isChecked = false
        }
        if (radioButton != dialogFeedbackBinding.glitchesRB){
            dialogFeedbackBinding.glitchesRB.isChecked = false
        }
        if (radioButton != dialogFeedbackBinding.errorsRB){
            dialogFeedbackBinding.errorsRB.isChecked = false
        }
        if (radioButton != dialogFeedbackBinding.othersRB){
            dialogFeedbackBinding.othersRB.isChecked = false
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


    // Method to display a warning dialog before logout
    private fun logoutWarningDialog() {
        val warningDialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val warningDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(warningDialogBinding.root))
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .setCancelable(true)
            .create()

        warningDialogBinding.titleTV.text = "Log out"
        warningDialogBinding.messageTV.text = "Do you want to logout?"

        // Set up 'Yes' button to confirm logout
        warningDialogBinding.yesBtn.setOnClickListener {
            FirebaseUtil().logoutUser(context)
        }

        // Set up 'No' button to cancel the dialog
        warningDialogBinding.NoBtn.setOnClickListener {
            warningDialog.dismiss()
        }

        // Show the warning dialog
        warningDialog.show()
    }
}
