package com.example.synthronize.model

import com.google.firebase.Timestamp

data class ProductModel (
    val productId:String = "",
    val productName:String = "",
    val productDesc:String = "",
    val productPrice:Long = 0,
    val imageList:List<String> = listOf(),
    val sellerId:String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
)