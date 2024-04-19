package com.example.synthronize
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.synthronize.databinding.DialogCreateGroupBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.FirebaseUtil
class CreateGroupDialogFragment : DialogFragment() {

    private lateinit var binding: DialogCreateGroupBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up spinner with options
        val groupTypeOptions = arrayOf("Public", "Private")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, groupTypeOptions)
        binding.groupTypeSpinner.adapter = adapter

        // Set up spinner selection listener
        binding.groupTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem == "Private") {
                    // Show groupCodeEditText when "Private" is selected
                    binding.groupCodeEditText.visibility = View.VISIBLE
                    showCommunityCodePrompt()
                } else if (selectedItem == "Public") {
                    // Hide groupCodeEditText for other selections
                    binding.groupCodeEditText.setText(generateRandomCode(6))
                    binding.groupCodeEditText.visibility = View.GONE

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle nothing selected if needed
            }
        }

        // Set click listener for the create button
        binding.createButton.setOnClickListener {
            // Retrieve input values from EditText fields
            val groupName = binding.groupNameEditText.text.toString()
            val groupDescription = binding.groupDescriptionEditText.text.toString()
            val groupType = binding.groupTypeSpinner.selectedItem.toString()
            val groupCode = binding.groupCodeEditText.text.toString()

            // Perform necessary actions to save group information
            saveGroupToFirestore(groupName, groupDescription, groupType, groupCode)

            // Dismiss the dialog after saving
            dismiss()
        }
    }
    private fun generateRandomCode(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('_', '-', '@', '#', '$', '%', '^', '&')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun saveGroupToFirestore(communityName: String, communityDescription: String, communityType: String, communityCode: String) {
        // Create a HashMap to store group data
        var communityModel = CommunityModel()

        FirebaseUtil().retrieveAllCommunityCollection().add(communityModel).addOnSuccessListener {
            //retrieve new communityId
            val communityId = it.id
            communityModel = CommunityModel(
                communityId = communityId,
                communityName = communityName,
                communityDescription = communityDescription,
                communityType = communityType,
                communityCode = communityCode,
                communityMembers = listOf(FirebaseUtil().currentUserUid()),
                communityAdmin = listOf(FirebaseUtil().currentUserUid())
            )
            //set data to firestore
            FirebaseUtil().retrieveCommunityDocument(communityId).set(communityModel)
        }

    }


    private fun showCommunityCodePrompt() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Custom Community Code")

        // Set up the input field for the group code
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK") { dialog, _ ->
            val groupCode = input.text.toString()
            binding.groupCodeEditText.setText(groupCode)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
