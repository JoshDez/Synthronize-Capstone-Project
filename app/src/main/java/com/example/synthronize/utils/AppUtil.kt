package com.example.synthronize.utils
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.MainActivity
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import de.hdodenhof.circleimageview.CircleImageView


class AppUtil {

    //For Images
    fun setGroupChatProfilePic(context:Context, filename: String, civ:CircleImageView){
        FirebaseUtil().retrieveGroupChatProfileRef(filename)
        Glide.with(context)
            .load(FirebaseUtil().retrieveGroupChatProfileRef(filename))
            .error(R.drawable.community_default_profile)
            .apply(RequestOptions.circleCropTransform())
            //image view
            .into(civ)
    }
    fun setUserProfilePic(context:Context, uid: String, civ:CircleImageView){
        //set default user profile first
        GlideApp.with(context)
            .load(R.drawable.user_default_profile)
            .apply(RequestOptions.circleCropTransform())
            .into(civ)

        //set the current user profile
        if (context is Activity && (context.isDestroyed || context.isFinishing)) {
            // The activity is not in a valid state to load images
            return
        } else {
            FirebaseUtil().targetUserDetails(uid).get().addOnSuccessListener {
                var user = it.toObject(UserModel::class.java)!!

                if (user.userMedia.containsKey("profile_photo")){
                    //get the image url from the key
                    var imageUrl = user.userMedia["profile_photo"]!!

                    GlideApp.with(context)
                        //storage reference
                        .load(FirebaseUtil().retrieveUserProfilePicRef(imageUrl))
                        .error(R.drawable.user_default_profile)
                        .apply(RequestOptions.circleCropTransform())
                        //image view
                        .into(civ)
                }
            }
        }
    }

    fun setUserCoverPic(context: Context, uid: String, cover:ImageView){
        FirebaseUtil().targetUserDetails(uid).get().addOnCompleteListener {
            if (it.result.exists()){
                var user = it.result.toObject(UserModel::class.java)!!
                if (user.userMedia.containsKey("profile_cover_photo")){
                    //get the image url from the key
                    var imageUrl = user.userMedia["profile_cover_photo"]!!

                    GlideApp.with(context)
                        //storage reference
                        .load(FirebaseUtil().retrieveUserCoverPicRef(imageUrl))
                        .error(R.drawable.profile_not_selected)
                        //image view
                        .into(cover)
                }
            }
        }
    }

    fun setCommunityProfilePic(context:Context, communityId: String, civ:CircleImageView){
        //set default community profile first
        GlideApp.with(context)
            .load(R.drawable.community_default_profile)
            .apply(RequestOptions.circleCropTransform())
            .into(civ)

        //set the current community profile
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnCompleteListener {
            if (it.result.exists()){
                var community = it.result.toObject(CommunityModel::class.java)!!

                if (community.communityMedia.containsKey("community_photo")){
                    //get the image url from the key
                    var imageUrl = community.communityMedia["community_photo"]!!

                    GlideApp.with(context)
                        //storage reference
                        .load(FirebaseUtil().retrieveCommunityProfilePicRef(imageUrl))
                        .error(R.drawable.community_default_profile)
                        .apply(RequestOptions.circleCropTransform())
                        //image view
                        .into(civ)
                }
            }
        }
    }

    fun setCommunityBannerPic(context:Context, communityId: String, imageView:ImageView){
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            var community = it.toObject(CommunityModel::class.java)!!

            if (community.communityMedia.containsKey("community_banner_photo")){
                //get the image url from the key
                var imageUrl = community.communityMedia["community_banner_photo"]!!

                GlideApp.with(context)
                    //storage reference
                    .load(FirebaseUtil().retrieveCommunityBannerPicRef(imageUrl))
                    .error(R.drawable.baseline_image_24)
                    //image view
                    .into(imageView)
            }
        }
    }

    //For Heading Back to Main Activity
    fun headToMainActivity(context: Context, fragment:String = "", delay:Long = 0, communityId: String = "", hasAnimation:Boolean = true) {
        Handler().postDelayed({
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("fragment", fragment)
            intent.putExtra("communityId", communityId)
            if (hasAnimation)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            else
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_NO_ANIMATION
            context.startActivity(intent)
        }, delay)
    }

    //head back to main activity from community if the user gets banned
    fun headToMainActivityIfBanned(context: Context, communityId: String){
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            if (isIdOnList(communityModel.bannedUsers, FirebaseUtil().currentUserUid())){
                Toast.makeText(context, "You're currently banned from the community", Toast.LENGTH_SHORT).show()
                headToMainActivity(context)
            }
        }
    }

    fun resetMainToolbar(binding:ActivityMainBinding){
        binding.backBtn.visibility = View.GONE
        binding.kebabMenuBtn.visibility = View.GONE
        binding.hamburgerMenuBtn.visibility = View.GONE
        binding.searchBtn.visibility = View.GONE
        binding.toolbarImageCIV.setImageResource(R.drawable.header_logo)

        //resetting main toolbar setOnClickListeners
        binding.searchBtn.setOnClickListener(null)
        binding.kebabMenuBtn.setOnClickListener(null)
        binding.hamburgerMenuBtn.setOnClickListener(null)
    }

    //Head to OtherUserProfile activity
    fun headToUserProfile(context:Context, userId: String) {
        val intent = Intent(context, OtherUserProfile::class.java)
        intent.putExtra("userID", userId)
        context.startActivity(intent)
    }

    //For Slicing Strings
    fun sliceMessage(string:String, intCap:Int):String{
        if (string.length > intCap){
            return "${string.slice(0..intCap)}..."
        } else {
            return string
        }
    }

    //checks if user is on list
    fun isIdOnList(list: List<String>, targetId:String):Boolean{
        for (id in list){
            if (id == targetId){
                return true
            }
        }
        return false
    }
    //checks if user is on a list through Collection
    fun isIdOnList(collection: Collection<String>, targetId:String):Boolean{
        val list:ArrayList<String> = ArrayList()
        for (value in collection){
            list.add(value)
        }

        for (id in list){
            if (id == targetId){
                return true
            }
        }
        return false
    }

    //extracts similar values and return as list of keys
    fun extractKeysFromMapByValue(map:Map<String, String>, value:String):ArrayList<String>{
        val list: ArrayList<String> = ArrayList()
        for (item in map){
            if (item.value == value){
                list += item.key
            }
        }
        return list
    }


    //BUTTON STATES

    //Friend request button
    //TODO change  material button to image button
    fun changeFriendsButtonState(friendButton: MaterialButton, userModel:UserModel){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val myUserModel = it.toObject(UserModel::class.java)!!

            //checks if already friends with user
            if (AppUtil().isIdOnList(userModel.friendsList, FirebaseUtil().currentUserUid())){
                friendButton.text = "Unfriend"
                friendButton.setOnClickListener {
                    userModel.friendsList = userModel.friendsList.filterNot { it == FirebaseUtil().currentUserUid() }
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState(friendButton, userModel)
                    }
                }

            } else if (AppUtil().isIdOnList(userModel.friendRequests, FirebaseUtil().currentUserUid())){
                friendButton.text = "Cancel Request"
                friendButton.setOnClickListener {
                    userModel.friendRequests = userModel.friendRequests.filterNot { it == FirebaseUtil().currentUserUid() }
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState(friendButton, userModel)
                    }
                }
            } else if (AppUtil().isIdOnList(myUserModel.friendRequests, userModel.userID)){
                friendButton.text = "Accept Request"
                friendButton.setOnClickListener {
                    FirebaseUtil().currentUserDetails().update("friendsList", FieldValue.arrayUnion(userModel.userID))
                    FirebaseUtil().targetUserDetails(userModel.userID).update("friendsList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                }
            } else {
                friendButton.text = "Add Friend"
                friendButton.setOnClickListener {
                    userModel.friendRequests = userModel.friendRequests.plus(FirebaseUtil().currentUserUid())
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState(friendButton, userModel)
                    }
                }
            }
        }
    }


    //TODO change  material button to image button
    fun changeCommunityButtonStates(context:Context, communityButton: MaterialButton, communityId: String, isOnPost:Boolean = false){
        communityButton.visibility = View.GONE

        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {community ->
            val communityModel = community.toObject(CommunityModel::class.java)!!

            if (!AppUtil().isIdOnList(communityModel.bannedUsers, FirebaseUtil().currentUserUid())){
                //shows the button if the user is not banned from the community
                FirebaseUtil().currentUserDetails().get().addOnSuccessListener {me ->
                    val myUserModel = me.toObject(UserModel::class.java)!!

                    if (myUserModel.userType == "AppAdmin" && !AppUtil().isIdOnList(communityModel.communityMembers.keys, FirebaseUtil().currentUserUid())) {
                        //if user is an AppAdmin and not yet joined the community
                        communityButton.visibility = View.VISIBLE
                        communityButton.text = "Join"
                        communityButton.setOnClickListener {
                            FirebaseUtil().addUserToCommunity(communityId){isSuccessful ->
                                if (isSuccessful){
                                    FirebaseUtil().addUserToAllCommunityChannels(communityId, FirebaseUtil().currentUserUid()){isSuccessful->
                                        if (isSuccessful){
                                            AppUtil().headToMainActivity(context, "community", 0, communityId)
                                        } else {
                                            Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                    } else if (AppUtil().isIdOnList(communityModel.joinRequestList, FirebaseUtil().currentUserUid())){
                        //If user is already requested to join (Private Community)
                        communityButton.visibility = View.VISIBLE
                        communityButton.text = "Cancel Request"
                        communityButton.setOnClickListener {
                            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                                .update("joinRequestList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                                .addOnSuccessListener {
                                    changeCommunityButtonStates(context, communityButton, communityId)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                }
                        }

                    } else if (AppUtil().isIdOnList(myUserModel.communityInvitations.values, myUserModel.userID)){
                        //If user is invited to join community
                        communityButton.visibility = View.VISIBLE
                        communityButton.text = "Accept"
                        communityButton.setOnClickListener {
                            FirebaseUtil().addUserToCommunity(communityId){isSuccessful ->
                                if (isSuccessful){
                                    FirebaseUtil().addUserToAllCommunityChannels(communityId, FirebaseUtil().currentUserUid()){
                                        changeCommunityButtonStates(context, communityButton, communityId)
                                    }
                                } else {
                                    Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                    } else if (!AppUtil().isIdOnList(communityModel.communityMembers.keys, FirebaseUtil().currentUserUid())){
                        //If user have not yet joined the community
                        communityButton.visibility = View.VISIBLE

                        if (communityModel.communityType == "Private"){
                            //Request to join the private community
                            communityButton.text = "Request to join"
                            communityButton.setOnClickListener {
                                FirebaseUtil().retrieveCommunityDocument(communityId)
                                    .update("joinRequestList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                                    .addOnSuccessListener {
                                        changeCommunityButtonStates(context, communityButton, communityId)
                                        Toast.makeText(context, "Please wait for the admin to take you in", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                    }
                            }

                        } else {
                            //Join the public community
                            communityButton.visibility = View.VISIBLE
                            communityButton.text = "Join"
                            communityButton.setOnClickListener {
                                FirebaseUtil().addUserToCommunity(communityId){isSuccessful ->
                                    if (isSuccessful){
                                        FirebaseUtil().addUserToAllCommunityChannels(communityId, FirebaseUtil().currentUserUid()){isSuccessful->
                                            if (isSuccessful){
                                                AppUtil().headToMainActivity(context, "community", 0, communityId)
                                            } else {
                                                Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else if (AppUtil().isIdOnList(communityModel.communityMembers.keys, FirebaseUtil().currentUserUid())) {
                        //If the user already joined
                        if (!isOnPost){
                            communityButton.visibility = View.VISIBLE
                            communityButton.text = "Enter"
                            communityButton.setOnClickListener {
                                AppUtil().headToMainActivity(context, "community", 0, communityId)
                            }
                        }
                    }
                }
            }
        }
    }

    //Detects any bad words
    fun containsBadWord(input: String): Boolean {
        // List of bad words in English and Filipino
        val badWords = listOf(
            "fuck", "fucking", "fucked", "fucks", "shit", "shitting", "shits", "asshole", "cunt", "bastard",
            "cock", "wanker", "crap", "gyatt", "ass", "cum", "creampie", "cock", "cocksucker", "milf", // English swear words
            "bobo", "puta", "putang", "pota", "potaena", "pakshet", "gago", "gagong", "kupal", "tite", "inamo", "kantot", "kantotan", "burat", "leche", "tarantado", "bakla" // Filipino bad words
        )

        // Convert the input to lowercase and split the string by space
        val words = input.toLowerCase().split(' ')

        // Check if any bad word is present in the input string
        for (badWord in badWords) {
            for (word in words) {
                if (word == badWord) {
                    return true
                }
            }
        }

        return false
    }

}