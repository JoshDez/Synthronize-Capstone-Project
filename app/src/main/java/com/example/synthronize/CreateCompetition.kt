package com.example.synthronize

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityCreateCompetitionBinding
import com.example.synthronize.databinding.ItemInstructionBinding

class CreateCompetition : AppCompatActivity() {
    private lateinit var binding:ActivityCreateCompetitionBinding

    private var canAddInstruction = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCompetitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {

        }

        binding.addInstructionBtn.setOnClickListener {
            if (canAddInstruction){
                addInstruction()
            } else {
                Toast.makeText(this, "Please save the instruction first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addInstruction() {
        canAddInstruction = false

        val instructionBinding = ItemInstructionBinding.inflate(layoutInflater)

        instructionBinding.saveInstructionBtn.setOnClickListener {
            //display
            instructionBinding.instructionTV.visibility = View.VISIBLE
            instructionBinding.editInstructionBtn.visibility = View.VISIBLE

            //hide
            instructionBinding.instructionEdtTxt.visibility = View.GONE
            instructionBinding.saveInstructionBtn.visibility = View.GONE

            canAddInstruction = true
        }

        instructionBinding.editInstructionBtn.setOnClickListener {
            //hide
            instructionBinding.instructionTV.visibility = View.GONE
            instructionBinding.editInstructionBtn.visibility = View.GONE

            //display
            instructionBinding.instructionEdtTxt.visibility = View.VISIBLE
            instructionBinding.saveInstructionBtn.visibility = View.VISIBLE

            canAddInstruction = true
        }

        instructionBinding.deleteInstructionBtn.setOnClickListener {
            binding.instructionsLayout.removeView(instructionBinding.root)

            canAddInstruction = true
        }

        binding.instructionsLayout.addView(instructionBinding.root)

    }
}