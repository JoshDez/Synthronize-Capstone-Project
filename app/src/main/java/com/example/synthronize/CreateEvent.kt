package com.example.synthronize

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.synthronize.databinding.ActivityCreateEventBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class CreateEvent : AppCompatActivity() {
    private lateinit var binding: ActivityCreateEventBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // To get current user ID
    private lateinit var selectedDate: Calendar
    private lateinit var selectedTime: Calendar
    private var selectedEventCoverUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                selectedEventCoverUri = data.data!!
                // Display the selected image in eventsCoverIV
                Glide.with(this).load(selectedEventCoverUri)
                    .into(binding.eventsCoverIV)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedDate = Calendar.getInstance()
        selectedTime = Calendar.getInstance()

        val communityId = intent.getStringExtra("communityId") ?: ""
        val eventId = intent.getStringExtra("postId") // Get eventId if editing

        if (eventId != null) {
            loadEventData(communityId, eventId) // Load existing event data
        }

        setupDateTimePickers()

        binding.eventsCoverIV.setOnClickListener {
            pickImage()
        }

        binding.createEventBtn.setOnClickListener {
            if (eventId != null) {
                updateEvent(communityId, eventId)
            } else {
                saveEvent(communityId)
            }
        }
    }
    private fun loadEventData(communityId: String, eventId: String) {
        firestore.collection("communities").document(communityId).collection("events").document(eventId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val event = document.data
                    if (event != null) {
                        // Populate UI with event data
                        binding.eventNameEdtTxt.setText(event["eventName"] as? String)
                        binding.eventDescriptionEdtTxt.setText(event["eventDesc"] as? String)
                        binding.eventLocationEdtTxt.setText(event["eventLocation"] as? String)
                        binding.eventDateEditText.setText(formatTimestampToDateTime(event["eventDate"] as? Timestamp))

                        // Load event cover image if available
                        val eventImageList = event["eventImageList"] as? List<String>
                        if (eventImageList?.isNotEmpty() == true) {
                            Glide.with(this).load(eventImageList[0]).into(binding.eventsCoverIV)
                        }
                    }
                }
            }


    }


    private fun updateEvent(communityId: String, eventId: String) {
        val eventName = binding.eventNameEdtTxt.text.toString()
        val eventDesc = binding.eventDescriptionEdtTxt.text.toString()
        val eventLocation = binding.eventLocationEdtTxt.text.toString()
        val eventDateTime = binding.eventDateEditText.text.toString()

        val timestamp = convertToTimestamp(eventDateTime)

        // Prepare event data
        val event = hashMapOf(
            "eventName" to eventName,
            "eventDesc" to eventDesc,
            "eventLocation" to eventLocation,
            "eventDate" to timestamp,
            "updatedTimestamp" to Timestamp.now()
        )

        // Update event in Firestore
        firestore.collection("communities").document(communityId).collection("events").document(eventId)
            .update(event as Map<String, Any>)
            .addOnSuccessListener {
                finish() // Close the activity on successful update
            }
            .addOnFailureListener {
                // Handle the error here
            }
    }

    private fun formatTimestampToDateTime(timestamp: Timestamp?): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = timestamp?.toDate() ?: Date()
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }


    private fun deleteEvent(communityId: String, eventId: String) {
        firestore.collection("communities").document(communityId).collection("events").document(eventId)
            .delete()
            .addOnSuccessListener {
                finish() // Close the activity on successful delete
            }
            .addOnFailureListener {
                // Handle the error here
            }
    }

    private fun setupDateTimePickers() {
        // Date picker setup
        binding.eventDateEditText.setOnClickListener {
            showDateTimePicker()
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

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun saveEvent(communityId: String) {
        val eventName = binding.eventNameEdtTxt.text.toString()
        val eventDesc = binding.eventDescriptionEdtTxt.text.toString()
        val eventLocation = binding.eventLocationEdtTxt.text.toString()
        val eventDateTime = binding.eventDateEditText.text.toString()

        // Generate a unique eventId
        val eventId = firestore.collection("communities").document(communityId).collection("events").document().id

        // Get the current user ID
        val eventOwnerId = auth.currentUser?.uid ?: ""

        // Convert date and time to Timestamp
        val timestamp = convertToTimestamp(eventDateTime)

        // Upload the event cover image if selected
        if (selectedEventCoverUri != null) {
            uploadImageToFirebaseStorage(selectedEventCoverUri!!, eventId) { downloadUrl ->
                val eventImageList: List<String> = listOf(downloadUrl)

                // Create event data
                val event = hashMapOf(
                    "eventId" to eventId,
                    "eventName" to eventName,
                    "eventDesc" to eventDesc,
                    "eventLocation" to eventLocation,
                    "eventDate" to timestamp,
                    "communityId" to communityId,
                    "eventOwnerId" to eventOwnerId,
                    "eventImageList" to eventImageList,
                    "createdTimestamp" to Timestamp.now()
                )

                // Save event to Firestore
                firestore.collection("communities").document(communityId).collection("events").document(eventId).set(event)
                    .addOnSuccessListener {
                        finish() // Close the activity on successful save
                    }
                    .addOnFailureListener {
                        // Handle the error here
                    }
            }
        } else {
            // Create event data without image
            val event = hashMapOf(
                "eventId" to eventId,
                "eventName" to eventName,
                "eventDesc" to eventDesc,
                "eventLocation" to eventLocation,
                "eventDate" to timestamp,
                "communityId" to communityId,
                "eventOwnerId" to eventOwnerId,
                "eventImageList" to emptyList<String>(),
                "createdTimestamp" to Timestamp.now()
            )

            // Save event to Firestore
            firestore.collection("communities").document(communityId).collection("events").document(eventId).set(event)
                .addOnSuccessListener {
                    finish() // Close the activity on successful save
                }
                .addOnFailureListener {
                    // Handle the error here
                }
        }
    }

    private fun uploadImageToFirebaseStorage(uri: Uri, eventId: String, onComplete: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("event_images/$eventId.jpg")
        val uploadTask = storageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                onComplete(downloadUrl.toString())
            }.addOnFailureListener {
                onComplete("")
            }
        }.addOnFailureListener {
            onComplete("")
        }
    }

    private fun convertToTimestamp(eventDateTime: String): Timestamp {
        val dateTimeFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
        val date = dateTimeFormat.parse(eventDateTime) ?: Date()
        return Timestamp(date)
    }
}
