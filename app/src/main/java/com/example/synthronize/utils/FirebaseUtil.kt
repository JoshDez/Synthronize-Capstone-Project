package com.example.synthronize.utils

import android.content.Context
import android.content.Intent
import com.example.synthronize.Login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
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
    fun retrieveUserProfilePicRef(uid:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("userProfilePicture")
            //name of picture
            .child(uid)
    }
    fun retrieveUserCoverPicRef(uid:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("userCoverPicture")
            //name of picture
            .child(uid)
    }

    fun retrieveCommunityProfilePicRef(uid:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityProfilePicture")
            //name of picture
            .child(uid)
    }

    fun retrieveCommunityContentImageRef(imageId:String): StorageReference {
        return FirebaseStorage.getInstance().reference
            .child("communityContentImage")
            .child(imageId)
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


}