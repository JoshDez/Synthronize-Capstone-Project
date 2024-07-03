package com.example.synthronize.utils
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.synthronize.MainActivity
import com.example.synthronize.R
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import de.hdodenhof.circleimageview.CircleImageView


class AppUtil {

    private fun isInternetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    //TODO Make changes
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
}