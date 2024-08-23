package com.example.synthronize.interfaces
import com.example.synthronize.databinding.ItemInstructionBinding
import com.example.synthronize.model.InstructionModel

interface OnInstructionModified {
    fun deleteInstruction(key:String)
    fun modifiedInstruction(key: String, instructionModel: InstructionModel)

    fun openImageLauncher(key: String, instruction: String)
}