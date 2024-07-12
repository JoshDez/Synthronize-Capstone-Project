package com.example.synthronize.utils
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
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
    fun setUserProfilePic(context:Context, uid: String, civ:CircleImageView){
        FirebaseUtil().targetUserDetails(uid).get().addOnSuccessListener {
            var user = it.toObject(UserModel::class.java)!!

            if (user.userMedia.containsKey("profile_photo")){
                //get the image url from the key
                var imageUrl = user.userMedia["profile_photo"]!!

                GlideApp.with(context)
                    //storage reference
                    .load(FirebaseUtil().retrieveUserProfilePicRef(imageUrl))
                    .error(R.drawable.profile_not_selected)
                    .apply(RequestOptions.circleCropTransform())
                    //image view
                    .into(civ)
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

    fun setUserCoverPic(context: Context, uid: String, cover:ImageView){
        FirebaseUtil().targetUserDetails(uid).get().addOnSuccessListener {
            var user = it.toObject(UserModel::class.java)!!

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

    fun setCommunityProfilePic(context:Context, communityId: String, civ:CircleImageView){

        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            var community = it.toObject(CommunityModel::class.java)!!

            if (community.communityMedia.containsKey("community_photo")){
                //get the image url from the key
                var imageUrl = community.communityMedia["community_photo"]!!

                GlideApp.with(context)
                    //storage reference
                    .load(FirebaseUtil().retrieveCommunityProfilePicRef(imageUrl))
                    .error(R.drawable.profile_not_selected)
                    .apply(RequestOptions.circleCropTransform())
                    //image view
                    .into(civ)
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
    fun isUserOnList(list: List<String>, userId:String):Boolean{
        for (id in list){
            if (id == userId){
                return true
            }
        }
        return false
    }




    //BUTTON STATES
    fun changeFriendsButtonState(friendButton: MaterialButton, userModel:UserModel){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val myUserModel = it.toObject(UserModel::class.java)!!

            //checks if already friends with user
            if (AppUtil().isUserOnList(userModel.friendsList, FirebaseUtil().currentUserUid())){
                friendButton.text = "Unfriend"
                friendButton.setOnClickListener {
                    userModel.friendsList = userModel.friendsList.filterNot { it == FirebaseUtil().currentUserUid() }
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState(friendButton, userModel)
                    }
                }

            } else if (AppUtil().isUserOnList(userModel.friendRequests, FirebaseUtil().currentUserUid())){
                friendButton.text = "Cancel Request"
                friendButton.setOnClickListener {
                    userModel.friendRequests = userModel.friendRequests.filterNot { it == FirebaseUtil().currentUserUid() }
                    FirebaseUtil().targetUserDetails(userModel.userID).set(userModel).addOnSuccessListener {
                        changeFriendsButtonState(friendButton, userModel)
                    }
                }
            } else if (AppUtil().isUserOnList(myUserModel.friendRequests, userModel.userID)){
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
}