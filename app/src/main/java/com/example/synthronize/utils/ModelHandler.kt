package com.example.synthronize.utils

import com.google.firebase.Timestamp
import com.example.synthronize.model.UserModel

class ModelHandler {

    fun retrieveUserModel(userID:String, callback: (UserModel) -> Unit){
        FirebaseUtil().targetUserDetails(userID).get().addOnSuccessListener {
            //initializes user model
            var userModel = UserModel()
            if (it.exists()){
                var fullName = it.getString("fullName")
                var userID = it.getString("userID")
                var createdTimestamp = it.getTimestamp("createdTimestamp")
                var description = it.getString("description")
                var username = it.getString("username")
                var birthday = it.getString("birthday")

                //Replaces null values with placeholders
                if (fullName == null){
                    fullName = ""
                }
                if (userID == null){
                    userID = ""
                }
                if (createdTimestamp == null){
                    createdTimestamp = Timestamp.now()
                }
                if (description == null){
                    description = ""
                }
                if (username == null){
                    username = ""
                }
                if (birthday == null){
                    birthday = ""
                }

                userModel = UserModel(
                    fullName,
                    createdTimestamp,
                    userID,
                    description,
                    username,
                    birthday
                )
                callback(userModel)
            } else {
                callback(userModel)
            }
        }
    }



}