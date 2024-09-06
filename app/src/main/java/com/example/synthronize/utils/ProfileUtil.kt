package com.example.synthronize.utils

import android.content.Context
import android.widget.Toast
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.adapters.FilesAdapter
import com.example.synthronize.adapters.ProfileFilesAdapter
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel

//For Profile Fragment and OtherUserProfile

class ProfileUtil {

    fun getUserPosts(context: Context, userId: String, callback: (AllFeedsAdapter) -> Unit){

        val postsList:ArrayList<PostModel> = ArrayList()

        FirebaseUtil().retrieveAllCommunityCollection()
            .whereEqualTo("communityType", "Public")
            .get().addOnSuccessListener { querySnapshot ->
                var communitiesTraversed = 0
                for (document in querySnapshot.documents) {
                    FirebaseUtil().retrieveAllCommunityCollection()
                        .document(document.id) // Access each post within a community
                        .collection("feeds")
                        .whereEqualTo("ownerId", userId)
                        .get()
                        .addOnSuccessListener { feedsSnapshot ->
                            var postsAdded = 0
                            feedsSnapshot.size()

                            //storing every user post to list
                            for (post in feedsSnapshot.documents){
                                var postModel = post.toObject(PostModel::class.java)!!
                                postsList.add(postModel)
                                postsAdded += 1

                                //checks if all the user posts in the community are added
                                if (postsAdded == feedsSnapshot.size()){
                                    //sorts the list by timestamp
                                    postsList.sortByDescending {
                                        it.createdTimestamp
                                    }
                                }
                            }

                            //increments
                            communitiesTraversed += 1

                            //deploys postsRV
                            if (communitiesTraversed == querySnapshot.documents.size){
                                callback(AllFeedsAdapter(context, postsList, false))
                            }

                        }.addOnFailureListener {
                            //returns the adapter with the only available posts
                            callback(AllFeedsAdapter(context, postsList, false))
                        }
                }
            }.addOnFailureListener {
                //returns the adapter with the only available posts
                callback(AllFeedsAdapter(context, postsList, false))
            }
    }
    fun getUserFiles(context: Context, userId: String, callback: (ProfileFilesAdapter) -> Unit){

        val filesList:ArrayList<FileModel> = ArrayList()

        FirebaseUtil().retrieveAllCommunityCollection()
            .whereEqualTo("communityType", "Public")
            .get().addOnSuccessListener { querySnapshot ->
                var communitiesTraversed = 0
                for (document in querySnapshot.documents) {
                    FirebaseUtil().retrieveAllCommunityCollection()
                        .document(document.id) // Access each post within a community
                        .collection("files")
                        .whereEqualTo("ownerId", userId)
                        .whereEqualTo("forCompetition", false)
                        .get()
                        .addOnSuccessListener { feedsSnapshot ->
                            var filesAdded = 0
                            feedsSnapshot.size()

                            //storing every user post to list
                            for (post in feedsSnapshot.documents){
                                var fileModel = post.toObject(FileModel::class.java)!!
                                filesList.add(fileModel)
                                filesAdded += 1
                                //checks if all the user posts in the community are added
                                if (filesAdded == feedsSnapshot.size()){
                                    //sorts the list by timestamp
                                    filesList.sortByDescending {
                                        it.createdTimestamp
                                    }
                                }
                            }

                            //increments
                            communitiesTraversed += 1

                            //deploys postsRV
                            if (communitiesTraversed == querySnapshot.documents.size){
                                callback(ProfileFilesAdapter(context, filesList))
                            }

                        }.addOnFailureListener {
                            //returns the adapter with the only available posts
                            callback(ProfileFilesAdapter(context, filesList))
                        }
                }
            }.addOnFailureListener {
                //returns the adapter with the only available posts
                callback(ProfileFilesAdapter(context, filesList))
            }
    }

    fun getCommunitiesCount(userId:String, callback: (Int) -> Unit){
        val roles = listOf("Admin", "Moderator", "Member")
        FirebaseUtil().retrieveAllCommunityCollection()
            .whereIn("communityMembers.${userId}", roles).get().addOnSuccessListener {
                callback(it.size())
            }.addOnFailureListener {
                callback(0)
        }
    }

    fun getFriendsCount(userId:String, callback: (Int) -> Unit){
        FirebaseUtil().targetUserDetails(userId).get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            callback(user.friendsList.size)
        }.addOnFailureListener {
            callback(0)
        }
    }

    fun getPostsCount(userId:String, callback: (Int) -> Unit){
        FirebaseUtil().retrieveAllCommunityCollection().get()
            .addOnSuccessListener { querySnapshot ->
                var totalPosts = 0
                for (document in querySnapshot.documents) {
                    FirebaseUtil().retrieveAllCommunityCollection()
                        .document(document.id) // Access each document within the collection
                        .collection("feeds")
                        .whereEqualTo("ownerId", userId)
                        .get()
                        .addOnSuccessListener { feedsSnapshot ->
                            totalPosts += feedsSnapshot.size()
                            callback(totalPosts)
                        }
                }
            }
            .addOnFailureListener {
                callback(0)
            }
    }

    fun getFilesCount(userId:String, callback: (Int) -> Unit){
        FirebaseUtil().retrieveAllCommunityCollection().get()
            .addOnSuccessListener { querySnapshot ->
                var totalPosts = 0
                for (document in querySnapshot.documents) {
                    FirebaseUtil().retrieveAllCommunityCollection()
                        .document(document.id) // Access each document within the collection
                        .collection("files")
                        .whereEqualTo("ownerId", userId)
                        .get()
                        .addOnSuccessListener { feedsSnapshot ->
                            totalPosts += feedsSnapshot.size()
                            callback(totalPosts)
                        }
                }
            }
            .addOnFailureListener {
                callback(0)
            }
    }

}