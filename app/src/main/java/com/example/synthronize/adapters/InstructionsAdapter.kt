package com.example.synthronize.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.synthronize.R
import com.example.synthronize.databinding.ItemInstructionBinding
import com.example.synthronize.interfaces.OnInstructionModified
import com.example.synthronize.model.InstructionModel
import com.example.synthronize.utils.FirebaseUtil

//FOR CREATE COMPETITIONS CLASS
class InstructionsAdapter(private val context: Context, private val instructionMap: HashMap<String, InstructionModel>,
                          private val listener:OnInstructionModified, private val toEdit:Boolean = false):
    RecyclerView.Adapter<InstructionsAdapter.InstructionViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionsAdapter.InstructionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemInstructionBinding.inflate(inflater, parent, false)
        return InstructionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InstructionsAdapter.InstructionViewHolder, position: Int) {
        val keys = instructionMap.keys.toList().sorted()
        holder.bind(keys[position])
    }

    override fun getItemCount(): Int {
        return instructionMap.size
    }


    inner class InstructionViewHolder(private val instructionBinding: ItemInstructionBinding): RecyclerView.ViewHolder(instructionBinding.root){
        private lateinit var instructionModel: InstructionModel
        fun bind(key:String){
            instructionModel = instructionMap[key]!!

            refreshInstruction()

            if (toEdit){
                //buttons
                instructionBinding.saveInstructionBtn.setOnClickListener {
                    val newInstruction = instructionBinding.instructionEdtTxt.text.toString()
                    if (newInstruction.isNotEmpty()){
                        instructionModel.instruction = newInstruction
                        instructionModel.saved = true
                        listener.modifiedInstruction(key, instructionModel)
                        refreshInstruction()
                    }
                }

                instructionBinding.editInstructionBtn.setOnClickListener {
                    instructionModel.saved = false
                    listener.modifiedInstruction(key, instructionModel)
                    refreshInstruction()
                }

                instructionBinding.deleteInstructionBtn.setOnClickListener {
                    listener.deleteInstruction(key)
                }

                instructionBinding.addImageLayout.setOnClickListener {
                    listener.openImageLauncher(key, instructionBinding.instructionEdtTxt.text.toString())
                }
            } else {
                instructionBinding.deleteInstructionBtn.visibility = View.GONE
                instructionBinding.addImageLayout.visibility = View.GONE
                instructionBinding.editInstructionBtn.visibility = View.GONE
            }
        }

        private fun refreshInstruction(){
            if (instructionModel.instruction.isNotEmpty()){
                instructionBinding.instructionTV.text = "${position + 1}.) ${instructionModel.instruction}"
                instructionBinding.instructionEdtTxt.setText(instructionModel.instruction)
            }

            if (instructionModel.saved){
                //hide
                instructionBinding.instructionEdtTxt.visibility = View.GONE
                instructionBinding.saveInstructionBtn.visibility = View.GONE
                instructionBinding.addImageLayout.visibility = View.GONE
                //display
                instructionBinding.instructionTV.visibility = View.VISIBLE
                instructionBinding.editInstructionBtn.visibility = View.VISIBLE

                displayInstructionImage()
            } else {
                //hide
                instructionBinding.instructionTV.visibility = View.GONE
                instructionBinding.editInstructionBtn.visibility = View.GONE
                //display
                instructionBinding.instructionEdtTxt.visibility = View.VISIBLE
                instructionBinding.saveInstructionBtn.visibility = View.VISIBLE

                displayInstructionImage()
            }
        }

        private fun displayInstructionImage(){
            if (instructionModel.imageName.isNotEmpty()){
                instructionBinding.addImageLayout.visibility = View.GONE
                instructionBinding.instructionIV.visibility = View.VISIBLE
                if (instructionModel.imageUri == Uri.EMPTY){
                    //download image from firebase storage
                    Glide.with(context)
                        .load(FirebaseUtil().retrieveCommunityContentImageRef(instructionModel.imageName))
                        .error(R.drawable.baseline_image_24)
                        .into(instructionBinding.instructionIV)
                } else {
                    Glide.with(context)
                        .load(instructionModel.imageUri)
                        .error(R.drawable.baseline_image_24)
                        .into(instructionBinding.instructionIV)
                }
            }
        }


    }
}