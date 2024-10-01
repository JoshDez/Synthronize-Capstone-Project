package com.example.synthronize

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.synthronize.databinding.ActivityCreateEventBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.GlideApp
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.text.SimpleDateFormat
import java.util.*

class CreateEvent : AppCompatActivity() {
    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var selectedDate: Calendar
    private lateinit var selectedTime: Calendar
    private lateinit var selectedImageUri: Uri
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var existingEventModel: EventModel
    private var communityId = ""
    private var eventId = ""
    private var imageName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        eventId = intent.getStringExtra("eventId").toString()
        selectedDate = Calendar.getInstance()
        selectedTime = Calendar.getInstance()


        if (eventId == "null" || eventId.isEmpty()) {
            bindButtons()
        } else {
            //Edit Event
            FirebaseUtil().retrieveCommunityEventsCollection(communityId).document(eventId).get().addOnCompleteListener {
                if(it.result.exists()){
                    existingEventModel = it.result.toObject(EventModel::class.java)!!

                    // Populate UI with event data
                    binding.eventNameEdtTxt.setText(existingEventModel.eventName)
                    binding.eventLocationEdtTxt.setText(existingEventModel.eventLocation)
                    binding.eventDateEditText.setText(DateAndTimeUtil().formatTimestampToDateTime(existingEventModel.eventDate))
                    AppUtil().showMoreAndLessWords(existingEventModel.eventDesc, binding.eventDescriptionEdtTxt, 150)

                    //Add image to event cover image view
                    if (existingEventModel.eventImageName.isNotEmpty()){
                        FirebaseUtil().retrieveCommunityContentImageRef(existingEventModel.eventImageName).downloadUrl.addOnSuccessListener {image ->
                            selectedImageUri = image
                            imageName = existingEventModel.eventImageName
                            binding.removeImageBtn.visibility = View.VISIBLE
                            GlideApp.with(this)
                                .load(image)
                                .into(binding.eventsCoverIV)
                        }.addOnFailureListener {
                            Toast.makeText(this, "An error has occurred while downloading, please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                    bindButtons()
                }
            }
        }

        //Launcher for user content image
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    selectedImageUri = data.data!!
                    imageName = "$communityId-Image-${UUID.randomUUID()}"
                    binding.removeImageBtn.visibility = View.VISIBLE
                    GlideApp.with(this)
                        .load(selectedImageUri)
                        .into(binding.eventsCoverIV)

                }
            }
        }

    }

    private fun bindButtons(){
        // Date picker setup
        binding.eventDateEditText.setOnClickListener {
            showDateTimePicker()
        }
        binding.eventsCoverIV.setOnClickListener {
            ImagePicker.with(this)
                .crop(25f, 10f)
                .compress(1080)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }
        binding.createEventBtn.setOnClickListener {
            uploadEvent()
        }
        binding.removeImageBtn.setOnClickListener {
            //removes image
            selectedImageUri = Uri.EMPTY
            imageName = ""
            binding.removeImageBtn.visibility = View.GONE
            GlideApp.with(this)
                .load(R.drawable.baseline_image_24_white)
                .into(binding.eventsCoverIV)
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        if (eventId.isNotEmpty() && eventId != "null"){
            binding.toolbarTitleTV.text = "Edit Event"
            binding.createEventBtn.text = "Save Event"
        }
    }

    private fun uploadEvent(){
        val tempModel = EventModel()
        val eventName = binding.eventNameEdtTxt.text.toString()
        val eventDesc = binding.eventDescriptionEdtTxt.text.toString()
        val eventLocation = binding.eventLocationEdtTxt.text.toString()
        val dateAndTime = binding.eventDateEditText.text.toString()
        var delay:Long = 1000

        //Validation
        if (eventName.isEmpty()){
            Toast.makeText(this, "Please add event name", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(eventName)){
            Toast.makeText(this, "Event name contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (dateAndTime.isEmpty()){
            Toast.makeText(this, "Please add event date and time", Toast.LENGTH_SHORT).show()
        } else if (eventLocation.isEmpty()){
            Toast.makeText(this, "Please add event location", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(eventLocation)){
            Toast.makeText(this, "Event location contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (eventDesc.isEmpty()){
            Toast.makeText(this, "Please add event description", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(eventDesc)){
            Toast.makeText(this, "Event description contains sensitive words", Toast.LENGTH_SHORT).show()
        } else {
            // Convert date and time to Timestamp
            val timestamp = convertToTimestamp(dateAndTime)

            if (eventId == "null" || eventId.isEmpty()){
                //UPLOAD EVENT
                FirebaseUtil().retrieveCommunityEventsCollection(communityId).add(tempModel).addOnSuccessListener {

                    //upload image cover
                    if (::selectedImageUri.isInitialized){
                        if (selectedImageUri != Uri.EMPTY){
                            FirebaseUtil().retrieveCommunityContentImageRef(imageName).putFile(selectedImageUri)
                            delay += 1000
                        }
                    }

                    val eventModel = EventModel(
                        eventId = it.id,
                        eventName = eventName,
                        eventDesc = eventDesc,
                        eventDate = timestamp,
                        eventImageName = imageName,
                        eventLocation = eventLocation,
                        communityId = communityId,
                        eventOwnerId = FirebaseUtil().currentUserUid(),
                        createdTimestamp = Timestamp.now()
                    )


                    //uploads product model to firebase
                    uploadToFirebase(eventModel.eventId, eventModel, "Your event is uploaded successfully!", delay)

                }.addOnFailureListener {
                    Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                }
            } else {
                //EDIT EVENT

                //upload image cover
                if (existingEventModel.eventImageName != imageName){
                    FirebaseUtil().retrieveCommunityContentImageRef(existingEventModel.eventImageName).delete()
                    if (::selectedImageUri.isInitialized){
                        if (selectedImageUri != Uri.EMPTY){
                            FirebaseUtil().retrieveCommunityContentImageRef(imageName).putFile(selectedImageUri)
                            delay += 1000
                        }
                    }
                }

                val eventModel = EventModel(
                    eventId = eventId,
                    eventName = eventName,
                    eventDesc = eventDesc,
                    eventDate = timestamp,
                    eventImageName = imageName,
                    eventLocation = eventLocation,
                    eventParticipants = existingEventModel.eventParticipants,
                    communityId = communityId,
                    eventOwnerId = FirebaseUtil().currentUserUid(),
                    createdTimestamp = Timestamp.now()
                )

                //uploads product model to firebase
                uploadToFirebase(eventModel.eventId, eventModel, "Your event is uploaded successfully!", delay)
            }
        }
    }

    private fun uploadToFirebase(eventId: String, eventModel: EventModel, toastMsg: String, delay: Long) {
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        if (this.eventId.isNotEmpty() && this.eventId != "null"){
            dialogLoadingBinding.messageTV.text = "Saving..."
        } else {
            dialogLoadingBinding.messageTV.text = "Uploading..."
        }

        loadingDialog.show()

        FirebaseUtil().retrieveCommunityEventsCollection(communityId).document(eventId).set(eventModel).addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                Handler().postDelayed({
                    loadingDialog.dismiss()
                    this.finish()
                }, delay)
            } else {
                Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
                this.finish()
            }
        }
    }

    private fun showDateTimePicker() {
        val dateListener = DatePickerDialog.OnDateSetListener { _: DatePicker, year: Int, month: Int, day: Int ->
            selectedDate.set(year, month, day)
            showTimePicker()
        }

        val datePickerDialog = DatePickerDialog(
            this,
            dateListener,
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val timeListener = TimePickerDialog.OnTimeSetListener { _: TimePicker, hourOfDay: Int, minute: Int ->
            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
            selectedTime.set(Calendar.MINUTE, minute)
            binding.eventDateEditText.setText(formatDateTime(selectedDate.time, selectedTime.time))
        }

        val timePickerDialog = TimePickerDialog(
            this,
            timeListener,
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun formatDateTime(date: Date, time: Date): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "${dateFormat.format(date)} ${timeFormat.format(time)}"
    }

    private fun convertToTimestamp(eventDateTime: String): Timestamp {
        val dateTimeFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
        val date = dateTimeFormat.parse(eventDateTime) ?: Date()
        return Timestamp(date)
    }

    override fun onBackPressed() {
        if (isModified()){
            //hides keyboard
            hideKeyboard()
            //Dialog for saving user profile
            val dialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(dialogBinding.root))
                .setGravity(Gravity.CENTER)
                .setBackgroundColorResId(R.color.transparent)
                .setCancelable(true)
                .create()

            dialogBinding.titleTV.text = "Warning"
            dialogBinding.messageTV.text = "Do you want to exit without saving?"

            dialogBinding.yesBtn.setOnClickListener {
                dialogPlus.dismiss()
                super.onBackPressed()
            }
            dialogBinding.NoBtn.setOnClickListener {
                dialogPlus.dismiss()
            }

            dialogPlus.show()
        } else {
            super.onBackPressed()
        }
    }

    private fun isModified(): Boolean {
        return binding.eventNameEdtTxt.text.toString().isNotEmpty() ||
                binding.eventDescriptionEdtTxt.text.toString().isNotEmpty() ||
                binding.eventLocationEdtTxt.text.toString().isNotEmpty() ||
                binding.eventDateEditText.text.toString().isNotEmpty() ||
                ::selectedImageUri.isInitialized
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }
}
