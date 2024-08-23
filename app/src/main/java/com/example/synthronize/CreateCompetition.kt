package com.example.synthronize

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.InstructionsAdapter
import com.example.synthronize.databinding.ActivityCreateCompetitionBinding
import com.example.synthronize.interfaces.OnInstructionModified
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.InstructionModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import java.util.Calendar

class CreateCompetition : AppCompatActivity(), OnInstructionModified {
    private lateinit var binding:ActivityCreateCompetitionBinding
    private lateinit var instructionsAdapter: InstructionsAdapter
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val instructionMap:HashMap<String, InstructionModel> = HashMap()
    private var idCtr = 0
    private var communityId = ""
    //from adapter
    private var selectedKey:String = ""
    private var selectedInstruction:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCompetitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()

        //Launcher for community profile pic
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    val tempModel = instructionMap.getValue(selectedKey)
                    tempModel.imageUri = data.data!!
                    tempModel.imageName = "${FirebaseUtil().currentUserUid()}-ImageInstruction-${generateRandomKey()}"
                    tempModel.instruction = selectedInstruction
                    instructionMap[selectedKey] = tempModel
                    setupInstructionsRV()
                }
            }
        }

        binding.deadlineEdtTxt.setOnClickListener {
            val calendar = Calendar.getInstance()
            val calYear = calendar.get(Calendar.YEAR)
            val calMonth = calendar.get(Calendar.MONTH)
            val calDay = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, DatePickerDialog.OnDateSetListener{ _, selectedYear, selectedMonth, selectedDay ->

                calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)

                val selectedTimestamp = Timestamp(calendar.time)

                val currentTimestamp = Timestamp.now()

                if (selectedTimestamp > currentTimestamp) {
                    // Display or use the timestamp
                    val stringDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                    binding.deadlineEdtTxt.setText(stringDate)
                } else {
                    Toast.makeText(this, "Please select a future date", Toast.LENGTH_SHORT).show()
                }
            }, calYear, calMonth, calDay).show()
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.addInstructionBtn.setOnClickListener {
            if (checkIfCanAddInstruction()){
                //adds new instruction
                instructionMap[idCtr.toString()] = InstructionModel()
                setupInstructionsRV()
                idCtr += 1
            } else {
                Toast.makeText(this, "Please save the instruction first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.postBtn.setOnClickListener {
            uploadCompetition()
        }
    }

    private fun uploadCompetition() {
        val competitionName = binding.competitionNameEdtTxt.text.toString()
        val competitionDesc = binding.competitionDescEdtTxt.text.toString()
        val rewards = binding.competitionRewardsEdtTxt.text.toString()
        val deadline = binding.deadlineEdtTxt.text.toString()
        val instructionList = getInstructions()

        if (competitionName.isEmpty() || competitionName.length < 3){
            Toast.makeText(this, "The competition name should at least have more than 3 characters", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(competitionName)){
            Toast.makeText(this, "The competition name contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (competitionDesc.isEmpty() || competitionDesc.length < 3){
            Toast.makeText(this, "The competition description should at least have more than 3 characters", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(competitionDesc)){
            Toast.makeText(this, "The competition description contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (rewards.isEmpty() || rewards.length < 2){
            Toast.makeText(this, "The competition rewards should at least have more than 2 characters", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(rewards)){
            Toast.makeText(this, "The competition rewards contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (deadline.isEmpty()){
            Toast.makeText(this, "Please select the deadline of the competition", Toast.LENGTH_SHORT).show()
        } else if (instructionList.isEmpty()){
            Toast.makeText(this, "Please provide instructions", Toast.LENGTH_SHORT).show()
        } else {
            var competitionModel = CompetitionModel()
            FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).add(competitionModel).addOnSuccessListener {
                competitionModel = CompetitionModel(
                    competitionId = it.id,
                    competitionName = competitionName,
                    description = competitionDesc,
                    rewards = rewards,
                    ownerId = FirebaseUtil().currentUserUid(),
                    instruction = getInstructions(),
                    deadline = DateAndTimeUtil().convertDateToTimestamp(deadline),
                    createdTimestamp = Timestamp.now()
                )
                FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(it.id).set(competitionModel).addOnSuccessListener {
                    Toast.makeText(this, "The competition has been uploaded", Toast.LENGTH_SHORT).show()
                    this.finish()
                }
            }
        }
    }

    private fun getInstructions():HashMap<String, List<String>>{
        var map:HashMap<String, List<String>> = HashMap()

        val sortedKeys = instructionMap.keys.toList().sorted()

        for (key in sortedKeys){
            val tempModel = instructionMap.getValue(key)

            val instruction = tempModel.instruction
            val imageName = tempModel.imageName

            if (imageName.isNotEmpty()){
                FirebaseUtil().retrieveCommunityContentImageRef(imageName).putFile(tempModel.imageUri)
            }

            map[key] = listOf(instruction, imageName)
        }
        return map
    }

    private fun setupInstructionsRV(){
        binding.instructionsRV.layoutManager = LinearLayoutManager(this)
        instructionsAdapter = InstructionsAdapter(this, instructionMap, this, true)
        binding.instructionsRV.adapter = instructionsAdapter
        binding.instructionsRV.smoothScrollToPosition(instructionMap.size)
    }

    private fun checkIfCanAddInstruction(): Boolean {
        for (instruction in instructionMap){
            val instructionModel = instruction.value
            if (!instructionModel.saved){
                return false
            }
        }
        return true
    }

    private fun generateRandomKey(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10)
            .map { allowedChars.random() }
            .joinToString("")
    }


    //OnInstructionModified
    override fun deleteInstruction(key: String) {
        instructionMap.remove(key)
        setupInstructionsRV()
    }

    override fun modifiedInstruction(key: String, model: InstructionModel) {
        instructionMap[key] = model
        setupInstructionsRV()
    }

    override fun openImageLauncher(key: String, instruction: String) {
        selectedKey = key
        selectedInstruction = instruction
        ImagePicker.with(this).cropSquare().compress(512)
            .maxResultSize(512, 512)
            .createIntent {
                imagePickerLauncher.launch(it)
            }
    }
}