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
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

class CreateCompetition : AppCompatActivity(), OnInstructionModified {
    private lateinit var binding:ActivityCreateCompetitionBinding
    private lateinit var instructionsAdapter: InstructionsAdapter
    private lateinit var existingCompetitionModel: CompetitionModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val instructionMap:HashMap<String, InstructionModel> = HashMap()
    private var idCtr = 0
    private var communityId = ""
    private var competitionId = ""
    private var resultType = "All"

    private var calendar = Calendar.getInstance()
    private var calYear = calendar.get(Calendar.YEAR)
    private var calMonth = calendar.get(Calendar.MONTH)
    private var calDay = calendar.get(Calendar.DAY_OF_MONTH)

    //from adapter
    private var selectedKey:String = ""
    private var selectedInstruction:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCompetitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        competitionId = intent.getStringExtra("competitionId").toString()

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

        if (competitionId == "null" || competitionId.isEmpty()){
            //For New Competition
            bindButtons()
        } else {
            //For Existing Competition to edit
            FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(competitionId).get().addOnSuccessListener {
                existingCompetitionModel = it.toObject(CompetitionModel::class.java)!!
                binding.competitionNameEdtTxt.setText(existingCompetitionModel.competitionName)
                binding.competitionDescEdtTxt.setText(existingCompetitionModel.description)
                binding.competitionRewardsEdtTxt.setText(existingCompetitionModel.rewards)
                communityId = existingCompetitionModel.communityId

                //bind result type
                var result = existingCompetitionModel.results.keys.toList()[0]
                if (result == "All"){
                    binding.allRB.isChecked = true
                    binding.topRB.isChecked = false
                    resultType = "All"
                } else {
                    binding.allRB.isChecked = false
                    binding.topRB.isChecked = true
                    binding.topEdtTxt.setText(existingCompetitionModel.results.keys.toList()[0]
                        .split('/').last().toString())
                    resultType = "Top"
                }

                //bind deadline
                val firebaseTimestamp: Timestamp = existingCompetitionModel.deadline
                val date: Date = firebaseTimestamp.toDate()
                val calendar = Calendar.getInstance()
                calendar.time = date
                calYear = calendar.get(Calendar.YEAR)
                calMonth = calendar.get(Calendar.MONTH)
                calDay = calendar.get(Calendar.DAY_OF_MONTH)
                val stringDate = "${calMonth + 1}/$calDay/$calYear"
                binding.deadlineEdtTxt.setText(stringDate)

                //bind instructions
                val orderedKeys = existingCompetitionModel.instruction.keys.toList().sorted()
                for (key in orderedKeys){
                    val content = existingCompetitionModel.instruction.getValue(key)
                    val instructionModel = InstructionModel(
                        instruction = content[0],
                        imageName = content[1],
                        saved = true
                    )
                    instructionMap[key] = instructionModel
                }

                idCtr = orderedKeys.last().toInt() + 1

                setupInstructionsRV()
                bindButtons()

            }
        }
    }

    private fun bindButtons(){

        if (competitionId != "null" && competitionId.isNotEmpty()){
            binding.postBtn.text = "Save"
            binding.toolbarTitleTV.text = "Edit Competition"
        }


        binding.deadlineEdtTxt.setOnClickListener {

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

        binding.allRB.setOnClickListener {
            if (binding.allRB.isChecked){
                binding.topRB.isChecked = false
                resultType = "All"
            }
        }

        binding.topRB.setOnClickListener {
            if (binding.topRB.isChecked){
                binding.allRB.isChecked = false
                resultType = "Top"
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
        val winnersLimit = binding.topEdtTxt.text.toString()
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
        }  else if ((resultType == "Top" && winnersLimit.isEmpty()) || (resultType == "Top" && winnersLimit.toInt() < 1)){
            Toast.makeText(this, "Result type should at least have 1 or more winners", Toast.LENGTH_SHORT).show()
        } else if (instructionList.isEmpty()){
            Toast.makeText(this, "Please provide instructions", Toast.LENGTH_SHORT).show()
        } else {
            var competitionModel = CompetitionModel()

            if (resultType == "Top"){
                resultType = "$resultType/$winnersLimit"
            }

            if (competitionId == "null" || competitionId.isEmpty()){
                //New Competition
                FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).add(competitionModel).addOnSuccessListener {
                    competitionModel = CompetitionModel(
                        competitionId = it.id,
                        competitionName = competitionName,
                        description = competitionDesc,
                        rewards = rewards,
                        ownerId = FirebaseUtil().currentUserUid(),
                        instruction = getInstructions(),
                        communityId = communityId,
                        results = hashMapOf(resultType to listOf()),
                        deadline = DateAndTimeUtil().convertDateToTimestamp(deadline),
                        createdTimestamp = Timestamp.now()
                    )
                    FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(it.id).set(competitionModel).addOnSuccessListener {
                        Toast.makeText(this, "The competition has been uploaded", Toast.LENGTH_SHORT).show()
                        this.finish()
                    }
                }
            } else {
                //Edited Competition
                competitionModel = CompetitionModel(
                    competitionId = competitionId,
                    competitionName = competitionName,
                    description = competitionDesc,
                    rewards = rewards,
                    ownerId = FirebaseUtil().currentUserUid(),
                    instruction = getInstructions(),
                    communityId = communityId,
                    results = hashMapOf(resultType to listOf()),
                    deadline = DateAndTimeUtil().convertDateToTimestamp(deadline),
                    createdTimestamp = existingCompetitionModel.createdTimestamp
                )

                deleteInstructionImagesFromStorage()

                FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(competitionId).set(competitionModel).addOnSuccessListener {
                    Toast.makeText(this, "The competition has been updated", Toast.LENGTH_SHORT).show()
                    this.finish()
                }
            }

        }
    }

    private fun deleteInstructionImagesFromStorage(){
        val currentImages:ArrayList<String> = arrayListOf()
        val pastImages:ArrayList<String> = arrayListOf()

        for (key in instructionMap.keys.toList().sorted()){
            val tempModel = instructionMap.getValue(key)
            if (tempModel.imageName.isNotEmpty()){
                currentImages.add(tempModel.imageName)
            }
        }
        for (key in existingCompetitionModel.instruction.keys.toList().sorted()){
            val imageName = existingCompetitionModel.instruction.getValue(key)[1]
            if (imageName.isNotEmpty()){
                pastImages.add(imageName)
            }
        }
        for (image in pastImages){
            //past image not included in current images
            if (!currentImages.contains(image)){
                //removes image from firebase storage
                FirebaseUtil().retrieveCommunityContentImageRef(image).delete()
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