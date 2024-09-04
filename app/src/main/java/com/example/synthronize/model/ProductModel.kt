package com.example.synthronize.model

import com.google.firebase.Timestamp

data class ProductModel (
    val productId:String = "",
    val productName:String = "",
    val productDesc:String = "",
    val communityId:String = "",
    val price:Long = 0,
    val imageList:List<String> = listOf(),
    val available:Boolean = true,
    val ownerId:String = "",
    val createdTimestamp: Timestamp = Timestamp.now(),
)