package com.example.synthronize.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.synthronize.Login
import com.example.synthronize.model.CommunityModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirebaseUtil {

    //For Authentication
    fun logoutUser(context: Context){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(context, Login::class.java)
        context.startActivity(intent)
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


    //For Community
    fun retrieveAllCommunityCollection():CollectionReference{
        return FirebaseFirestore.getInstance().collection("communities")
    }
    fun retrieveCommunityDocument(communityId:String):DocumentReference{
        return retrieveAllCommunityCollection().document(communityId)
    }

    fun retrieveCommunityFeedsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("feeds")
    }
    fun retrieveCommunityEventsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("events")
    }

    fun retrieveCommunityForumsCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("forums")
    }
    fun retrieveCommunityMarketCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("market")
    }
    fun retrieveCommunityFilesCollection(communityId: String):CollectionReference{
        return retrieveCommunityDocument(communityId).collection("files")
    }

    fun removeUserFromAllCommunityChannels(communityId:String, userId:String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            if (communityModel.communityChannels.isNotEmpty()){
                for (channel in communityModel.communityChannels){
                    //removes user from community channel chatroom
                    FirebaseUtil().retrieveAllChatRoomReferences().document("${communityModel.communityId}-$channel")
                        .update("userIdList", FieldValue.arrayRemove(userId))
                }
                callback(true)
            } else {
                callback(true)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun addUserToAllCommunityChannels(communityId:String, userId:String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            if (communityModel.communityChannels.isNotEmpty()){
                for (channel in communityModel.communityChannels){
                    //removes user from community channel chatroom
                    FirebaseUtil().retrieveAllChatRoomReferences().document("${communityModel.communityId}-$channel")
                        .update("userIdList", FieldValue.arrayUnion(userId))
                }
                callback(true)
            } else {
                callback(true)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

}