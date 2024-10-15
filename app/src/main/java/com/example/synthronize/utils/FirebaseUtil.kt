package com.example.synthronize.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.synthronize.Login
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirebaseUtil {

    //For Authentication
    fun logoutUser(context: Context){
        FirebaseMessaging.getInstance().deleteToken().addOnSuccessListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(context, Login::class.java)
            context.startActivity(intent)
        }
    }
    fun currentUserUid(): String {
        return FirebaseAuth.getInstance().uid!!
    }
    fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().uid != null
    }

    //For retrieving users documents
    fun currentUserDetails(): DocumentReference {
        return FirebaseFirestore.getInstance().collection("users").document(currentUserUid())
    }
    fun targetUserDetails(targetUid:String): DocumentReference {
        return FirebaseFirestore.getInstance().collection("users").document(targetUid)
    }
    fun allUsersCollectionReference(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("users")
    }
    fun retrieveAllUserTypeRequests(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("account_type_requests")
    }


    //For Chat Function
    fun retrieveChatRoomReference(chatroomID:String): DocumentReference {
        return FirebaseFirestore.getInstance().collection("chatroom").document(chatroomID)
    }

    fun retrieveAllChatRoomReferences(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("chatroom")
    }

    fun retrieveChatsFromChatroom(chatroomID: String): CollectionReference {
        return retrieveChatRoomReference(chatroomID).collection("chats")
    }

    //For Firebase Storage
    fun retrieveUserProfilePicRef(url:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("userProfilePicture")
            //name of picture
            .child(url)
    }
    fun retrieveUserCoverPicRef(url:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("userCoverPicture")
            //name of picture
            .child(url)
    }

    fun retrieveCommunityProfilePicRef(url:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityProfilePicture")
            //name of picture
            .child(url)
    }

    fun retrieveGroupChatProfileRef(url:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("groupChatProfilePicture")
            //name of picture
            .child(url)
    }

    fun retrieveCommunityBannerPicRef(url:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityBannerPicture")
            //name of picture
            .child(url)
    }

    fun retrieveCommunityContentImageRef(imageFilename:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityContentImage")
            .child(imageFilename)
    }

    fun retrieveCommunityContentVideoRef(videoFilename:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityContentVideo")
            .child(videoFilename)
    }
    fun retrieveCommunityFileRef(filename:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityFiles")
            .child(filename)
    }


    //For Community
    fun isUserAppAdmin(userId: String, callback: (Boolean) -> Unit){
        FirebaseUtil().targetUserDetails(userId).get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            if (userModel.userType == "AppAdmin"){
                callback(true)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }
    fun addUserToCommunity(communityId: String, callback: (Boolean) -> Unit){
        val newMapValue = mapOf(
            "communityMembers.${FirebaseUtil().currentUserUid()}" to "Member"
        )
        FirebaseUtil().retrieveCommunityDocument(communityId)
            .update(newMapValue).addOnSuccessListener {
                callback(true)
            }.addOnFailureListener {
                callback(false)
            }
    }
    fun retrieveReportsCollection():CollectionReference{
        return FirebaseFirestore.getInstance().collection("reports")
    }
    fun retrieveAllCommunityCollection():CollectionReference{
        return FirebaseFirestore.getInstance().collection("communities")
    }
    fun retrieveCommunityDocument(communityId:String):DocumentReference{
        return retrieveAllCommunityCollection().document(communityId)
    }
    fun retrieveCommunityFeedsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("feeds")
    }
    fun retrieveCommunityReportsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("reports")
    }
    fun retrieveCommunityEventsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("events")
    }
    fun retrieveCommunityForumsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("forums")
    }
    fun retrieveCommunityForumsCommentCollection(communityId: String, postId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("forums").document(postId).collection("comments")
    }
    fun retrieveCommunityMarketCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("market")
    }
    fun retrieveCommunityFilesCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("files")
    }
    fun retrieveCommunityCompetitionsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("competitions")
    }
    fun removeUserFromAllCommunityChannels(communityId:String, userId:String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveAllChatRoomReferences().whereEqualTo("communityId", communityId).get().addOnSuccessListener {channels ->
            for (channel in channels.documents){
                val chatroom = channel.toObject(ChatroomModel::class.java)!!
                FirebaseUtil().retrieveChatRoomReference(chatroom.chatroomId).update("userIdList", FieldValue.arrayRemove(userId))
                FirebaseUtil().retrieveChatRoomReference(chatroom.chatroomId).update("chatroomAdminList", FieldValue.arrayRemove(userId))
            }
            callback(true)
        }.addOnFailureListener {
            callback(false)
        }
    }
    fun addUserToAllCommunityChannels(communityId:String, userId:String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveAllChatRoomReferences().whereEqualTo("communityId", communityId).get().addOnSuccessListener {channels ->
            for (channel in channels.documents){
                val chatroom = channel.toObject(ChatroomModel::class.java)!!
                FirebaseUtil().retrieveChatRoomReference(chatroom.chatroomId).update("userIdList", FieldValue.arrayUnion(userId))
            }
            callback(true)
        }.addOnFailureListener {
            callback(false)
        }
    }
}