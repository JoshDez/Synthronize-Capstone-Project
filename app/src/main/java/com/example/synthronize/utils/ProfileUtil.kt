package com.example.synthronize.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.R
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.adapters.CommunityAdapter
import com.example.synthronize.adapters.FilesAdapter
import com.example.synthronize.adapters.FriendsAdapter
import com.example.synthronize.adapters.ProfileFilesAdapter
import com.example.synthronize.adapters.SearchCommunityAdapter
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.DialogListBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

//For Profile Fragment and OtherUserProfile

class ProfileUtil {

    fun getUserPosts(context: Context, userId: String, searchQuery: String = "", forOtherUserProfile:Boolean = false, callback: (AllFeedsAdapter) -> Unit){

        val postsList:ArrayList<PostModel> = ArrayList()

        var communityQuery:Query = FirebaseUtil().retrieveAllCommunityCollection()

        if (forOtherUserProfile){
            communityQuery = FirebaseUtil().retrieveAllCommunityCollection()
                .whereEqualTo("communityType", "Public")
        }

        communityQuery.get().addOnSuccessListener { querySnapshot ->
                var communitiesTraversed = 0
                for (document in querySnapshot.documents) {
                    var postsQuery:Query

                    if (searchQuery.isNotEmpty()){
                        postsQuery = FirebaseUtil().retrieveAllCommunityCollection()
                            .document(document.id) // Access each post within a community
                            .collection("feeds")
                            .whereEqualTo("ownerId", userId)
                            .whereGreaterThanOrEqualTo("caption", searchQuery)
                            .whereLessThanOrEqualTo("caption", searchQuery+"\uf8ff")
                    } else {
                        postsQuery = FirebaseUtil().retrieveAllCommunityCollection()
                            .document(document.id) // Access each post within a community
                            .collection("feeds")
                            .whereEqualTo("ownerId", userId)
                    }

                    postsQuery.get()
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
    fun getUserFiles(context: Context, userId: String, searchQuery: String = "", forOtherUserProfile:Boolean = false, callback: (ProfileFilesAdapter) -> Unit){

        val filesList:ArrayList<FileModel> = ArrayList()

        var communityQuery:Query = FirebaseUtil().retrieveAllCommunityCollection()

        if (forOtherUserProfile){
            communityQuery = FirebaseUtil().retrieveAllCommunityCollection()
                .whereEqualTo("communityType", "Public")
        }

        communityQuery.get().addOnSuccessListener { querySnapshot ->
                var communitiesTraversed = 0
                for (document in querySnapshot.documents) {
                    var filesQuery:Query

                    if (searchQuery.isNotEmpty()){
                        filesQuery =  FirebaseUtil().retrieveAllCommunityCollection()
                            .document(document.id) // Access each file within a community
                            .collection("files")
                            .whereEqualTo("ownerId", userId)
                            .whereEqualTo("forCompetition", false)
                            .whereGreaterThanOrEqualTo("caption", searchQuery)
                            .whereLessThanOrEqualTo("caption", searchQuery+"\uf8ff")
                    } else {
                        filesQuery =  FirebaseUtil().retrieveAllCommunityCollection()
                            .document(document.id) // Access each file within a community
                            .collection("files")
                            .whereEqualTo("ownerId", userId)
                            .whereEqualTo("forCompetition", false)
                    }

                    filesQuery.get()
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

    fun openUserFriendsListDialog(context: Context, userId: String, layoutInflater:LayoutInflater, onItemClickListener: OnItemClickListener){
        val friendsBinding = DialogListBinding.inflate(layoutInflater)
        val friendsDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(friendsBinding.root))
            .create()

        var searchQuery = ""

        setupFriendsRV(context, userId, searchQuery, friendsBinding.listRV, onItemClickListener)

        friendsBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = friendsBinding.searchEdtTxt.text.toString()
                setupFriendsRV(context, userId, searchQuery, friendsBinding.listRV, onItemClickListener)
            }

        })

        friendsBinding.toolbarTitleTV.text = "Friends"

        friendsBinding.backBtn.setOnClickListener {
            friendsDialog.dismiss()
        }

        friendsDialog.show()
    }

    private fun setupFriendsRV(context: Context, userId: String,  searchQuery:String, friendsRV:RecyclerView, onItemClickListener: OnItemClickListener) {
        if (searchQuery.isNotEmpty()){
            if (searchQuery[0] == '@'){
                //search for username
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereArrayContains("friendsList", userId)
                    .whereGreaterThanOrEqualTo("username", searchQuery.removePrefix("@"))

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                friendsRV.layoutManager = LinearLayoutManager(context)
                val searchUserAdapter = SearchUserAdapter(context = context, options, listener = onItemClickListener)
                friendsRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            } else {
                //search for fullName
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereArrayContains("friendsList", userId)
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                friendsRV.layoutManager = LinearLayoutManager(context)
                val searchUserAdapter = SearchUserAdapter(context = context, options, listener = onItemClickListener)
                friendsRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        } else {
            //query all users
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereArrayContains("friendsList", userId)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            friendsRV.layoutManager = LinearLayoutManager(context)
            val searchUserAdapter = SearchUserAdapter(context = context, options, listener = onItemClickListener)
            friendsRV.adapter = searchUserAdapter
            searchUserAdapter.startListening()
        }
    }

    fun openCommunityListDialog(context: Context, userId: String, layoutInflater:LayoutInflater, onItemClickListener: OnItemClickListener){
        val communityBinding = DialogListBinding.inflate(layoutInflater)
        val communityDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(communityBinding.root))
            .create()

        var searchQuery = ""

        setupCommunityRV(context, userId, searchQuery, communityBinding.listRV, onItemClickListener, communityDialog)

        communityBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = communityBinding.searchEdtTxt.text.toString()
                setupCommunityRV(context, userId, searchQuery, communityBinding.listRV, onItemClickListener, communityDialog)
            }
        })

        communityBinding.toolbarTitleTV.text = "Communities"

        communityBinding.backBtn.setOnClickListener {
            communityDialog.dismiss()
        }

        communityDialog.show()
    }

    fun setupCommunityRV(context: Context, userId: String,  searchQuery:String, communityRV:RecyclerView, listener: OnItemClickListener, communityDialog: DialogPlus){
        //roles
        val roles = listOf("Admin", "Moderator", "Member")

        if (searchQuery.isNotEmpty()){

            //query firestore
            val communityQuery = FirebaseUtil().retrieveAllCommunityCollection()
                .whereIn("communityMembers.${userId}", roles)
                .whereEqualTo("communityName", searchQuery)

            //set options for firebase ui
            val options: FirestoreRecyclerOptions<CommunityModel> =
                FirestoreRecyclerOptions.Builder<CommunityModel>().setQuery(communityQuery, CommunityModel::class.java).build()

            communityRV.layoutManager = LinearLayoutManager(context)
            val communityAdapter = SearchCommunityAdapter(context, options, listener, true, communityDialog, true)
            communityRV.adapter = communityAdapter
            communityAdapter.startListening()

        } else {

            //query firestore
            val communityQuery = FirebaseUtil().retrieveAllCommunityCollection()
                .whereIn("communityMembers.${userId}", roles)

            //set options for firebase ui
            val options: FirestoreRecyclerOptions<CommunityModel> =
                FirestoreRecyclerOptions.Builder<CommunityModel>().setQuery(communityQuery, CommunityModel::class.java).build()

            communityRV.layoutManager = LinearLayoutManager(context)
            val communityAdapter = SearchCommunityAdapter(context, options, listener, true, communityDialog, true)
            communityRV.adapter = communityAdapter
            communityAdapter.startListening()
        }

    }

    fun openFilesDescriptionDialog(context: Context, layoutInflater:LayoutInflater){
        val fileDescBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val fileDescDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(fileDescBinding.root))
            .setCancelable(true)
            .setExpanded(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()
        fileDescBinding.titleTV.text = "User Files"
        fileDescBinding.messageTV.text = "This is the number of files this user has contributed to communities."
        fileDescBinding.NoBtn.visibility = View.GONE
        fileDescBinding.yesBtn.visibility = View.GONE
        fileDescDialog.show()
    }

    fun openPostsDescriptionDialog(context: Context, layoutInflater:LayoutInflater){
        val postDescBinding = DialogWarningMessageBinding.inflate(layoutInflater)
        val postDescDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(postDescBinding.root))
            .setCancelable(true)
            .setExpanded(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()
        postDescBinding.titleTV.text = "User Posts"
        postDescBinding.messageTV.text = "This is the number of posts this user has contributed to communities."
        postDescBinding.NoBtn.visibility = View.GONE
        postDescBinding.yesBtn.visibility = View.GONE
        postDescDialog.show()
    }

}