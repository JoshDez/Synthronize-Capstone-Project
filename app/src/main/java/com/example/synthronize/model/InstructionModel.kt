package com.example.synthronize.model

import android.net.Uri

//FOR CREATE COMPETITION CLASS
data class InstructionModel(
    var instruction:String = "",
    var imageName:String = "",
    var imageUri: Uri = Uri.EMPTY,
    var saved:Boolean = false
)
