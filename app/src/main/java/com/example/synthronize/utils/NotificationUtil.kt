package com.example.synthronize.utils

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import com.example.synthronize.Chatroom
import com.example.synthronize.model.UserModel
import com.google.auth.oauth2.ServiceAccountCredentials
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream

class NotificationUtil {

    //Sends a notification to the user
    fun sendNotificationToUser(context: Context, contentId:String, contentOwnerId:String, action:String,
                               repeatedAction:String, contentType:String, communityId: String, timestamp:String){
        if (contentOwnerId != FirebaseUtil().currentUserUid()){
            FirebaseUtil().targetUserDetails(contentOwnerId).get().addOnSuccessListener {
                if (it.exists()){
                    //key: contentId  value: List{ "userId"(user who did the action), "action"(comment or love), "repeatedAction"(how many who did the action),
                    // "contentType"(type of content), "communityId"(communityId in which the content belongs to), "timestamp"(timestamp in string form when action executed) }
                    val mapUpdate = hashMapOf<String, Any>(
                        "notifications.$contentId" to listOf(FirebaseUtil().currentUserUid(), action, repeatedAction, contentType, communityId, timestamp, "not_seen")
                    )
                    FirebaseUtil().targetUserDetails(contentOwnerId).update(mapUpdate).addOnSuccessListener {
                        FirebaseUtil().targetUserDetails(contentOwnerId).get().addOnCompleteListener {user ->
                            if (user.result.exists()){
                                val userModel = user.result.toObject(UserModel::class.java)!!
                                //TODO to add is current user admin?
                                sendPushNotifications(context, communityId, action, contentId, contentType, userModel.fcmToken)
                            }
                        }
                    }
                }
            }
        }
    }

    fun sendPushNotificationsForChat(context: Context, userIdList: List<String>, chatroomType:String, chatroomId:String = "", chatroomName:String = "",
                                             senderName:String = "", receiverId:String = "", message:String = "", communityId: String = ""){
        for (user in userIdList){
            FirebaseUtil().targetUserDetails(user).get().addOnCompleteListener {
                if (it.result.exists()){
                    val userModel = it.result.toObject(UserModel::class.java)!!
                    var jsonObject = ""

                    when(chatroomType){
                        //COMMUNITY CHAT
                        "community_chat" -> {
                            jsonObject = """
                                {
                                    "message": {
                                        "token": "${userModel.fcmToken}",
                                        "notification": {
                                            "title": "$chatroomName",
                                            "body": "$senderName: $message"
                                        },
                                        "data": {
                                            "communityId": "$communityId",
                                            "chatroomType": "$chatroomType",
                                            "chatroomId": "$chatroomId",
                                            "chatroomName": "$chatroomName"
                                        }
                                    }
                                }
                            """.trimIndent()
                        }
                        //GROUP CHAT
                        "group_chat" -> {
                            jsonObject = """
                                {
                                    "message": {
                                        "token": "${userModel.fcmToken}",
                                        "notification": {
                                            "title": "$chatroomName",
                                            "body": "$senderName: $message"
                                        },
                                        "data": {
                                            "chatroomId": "$chatroomId",
                                            "chatroomType": "$chatroomType"
                                        }
                                    }
                                }
                            """.trimIndent()
                        }
                        //DIRECT MESSAGE
                        "direct_message" -> {
                            jsonObject = """
                                {
                                    "message": {
                                        "token": "${userModel.fcmToken}",
                                        "notification": {
                                            "title": "$chatroomName!",
                                            "body": "$message"
                                        },
                                        "data": {
                                            "chatroomName": "$chatroomName",
                                            "chatroomType": "$chatroomType",
                                            "userID": "$receiverId"
                                        }
                                    }
                                }
                            """.trimIndent()
                        }
                    }
                    callApi(context, jsonObject)
                }
            }
        }


    }

    private fun sendPushNotifications(context: Context, communityId:String = "", action: String = "", contentId:String = "", contentType:String = "", fcmToken:String = ""){

        FirebaseUtil().currentUserDetails().get().addOnCompleteListener {
            if (it.result.exists()){
                val userModel = it.result.toObject(UserModel::class.java)!!
                try {
                    var body = ""
                    //create message for the body
                    when(action){
                        "Love" -> {
                            //adds action to the notification message
                            body = "${userModel.username} loved your ${contentType.lowercase()}"
                        }
                        "Comment" -> {
                            //adds action to the notification message
                            body = "${userModel.username} commented on your ${contentType.lowercase()}"
                        }
                        "Join" -> {
                            //adds action to the notification message
                            body = "${userModel.username} joined your ${contentType.lowercase()}"
                        }
                        "Share" -> {
                            //adds action to the notification message
                            body = "${userModel.username} shared your ${contentType.lowercase()}"
                        }
                    }
                    //create json
                    val jsonObject = """
                        {
                            "message": {
                                "token": "$fcmToken",
                                "notification": {
                                    "title": "New $action!",
                                    "body": "$body"
                                },
                                "data": {
                                    "communityId": "$communityId",
                                    "action": "$action",
                                    "contentId": "$contentId",
                                    "contentType": "$contentType",
                                }
                            }
                        }
                    """.trimIndent()
                    callApi(context, jsonObject)
                } catch (e:Exception){

                }
            }
        }


    }

    private fun callApi(context: Context, jsonString: String){
        getAccessToken(context){accessToken ->
            var json: MediaType = "application/json; charset=utf-8".toMediaType()
            var client = OkHttpClient()
            var url = "https://fcm.googleapis.com/v1/projects/synthronize/messages:send"

            val body = jsonString.toRequestBody(json)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()


            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        println("Message sent successfully")
                    } else {
                        println("Error sending message: ${response.message}")
                    }
                }
            })
        }
    }

    private fun getAccessToken(context: Context, callback: (String?) -> Unit) {
        AccessTokenTask(context, callback).execute()
    }

    private class AccessTokenTask(
        private val context: Context,
        private val callback: (String?) -> Unit
    ) : AsyncTask<Void, Void, String?>() {

        override fun doInBackground(vararg params: Void?): String? {
            return try {
                val inputStream: InputStream = context.assets.open("synthronize-firebase-adminsdk-ygvk8-7d055bf54c.json")
                val credentials = ServiceAccountCredentials.fromStream(inputStream)
                val scopedCredentials = credentials.createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                scopedCredentials.refreshIfExpired()

                scopedCredentials.accessToken.tokenValue
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: String?) {
            callback(result)
        }
    }

}