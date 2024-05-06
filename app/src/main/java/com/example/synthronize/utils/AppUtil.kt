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


    //For Images
    fun setUserProfilePic(context:Context, uid: String, civ:CircleImageView, clearCache: Boolean = false){

        val imageRef = FirebaseUtil().retrieveUserProfilePicRef(uid)

        if (isInternetConnected(context)){
            //if online
            GlideApp.with(context)
                //storage reference
                .load(imageRef)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .error(R.drawable.profile_not_selected)
                .apply(RequestOptions.circleCropTransform())
                //image view
                .into(civ)

            if (clearCache){
                // Clear Glide memory cache
                Glide.get(context).clearMemory()
            }
        } else {
            GlideApp.with(context)
                //storage reference
                .load(R.drawable.profile_not_selected)
                .apply(RequestOptions.circleCropTransform())
                //image view
                .into(civ)
        }

    }
    fun setUserCoverPic(context: Context, uid: String, cover:ImageView, clearCache: Boolean = false){
        val imageRef = FirebaseUtil().retrieveUserCoverPicRef(uid)

        if (isInternetConnected(context)){
            //if online
            GlideApp.with(context)
                //storage reference
                .load(imageRef)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .error(R.drawable.community_not_selected)
                //image view
                .into(cover)

            if (clearCache){
                // Clear Glide memory cache
                Glide.get(context).clearMemory()
            }
        } else {
            GlideApp.with(context)
                //storage reference
                .load(R.drawable.baseline_image_24)
                //image view
                .into(cover)
        }
    }

    fun setImageContent(context: Context, imageId: String, imageView:ImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveCommunityContentImageRef(imageId))
            .error(R.drawable.baseline_image_24)
            //image view
            .into(imageView)
    }

    fun setCommunityProfilePic(context:Context, uid: String, civ:CircleImageView, clearCache:Boolean = false){
        val imageRef = FirebaseUtil().retrieveCommunityProfilePicRef(uid)

        if (isInternetConnected(context)){
            //if online
            GlideApp.with(context)
                //storage reference
                .load(imageRef)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .error(R.drawable.community_not_selected)
                .apply(RequestOptions.circleCropTransform())
                //image view
                .into(civ)

            if (clearCache){
                // Clear Glide memory cache
                Glide.get(context).clearMemory()
            }
        } else {
            GlideApp.with(context)
                //storage reference
                .load(R.drawable.community_not_selected)
                .apply(RequestOptions.circleCropTransform())
                //image view
                .into(civ)
        }
    }

    fun setCommunityBannerPic(context:Context, uid: String, imageView:ImageView, clearCache:Boolean = false){
        val imageRef = FirebaseUtil().retrieveCommunityBannerPicRef(uid)

        if (isInternetConnected(context)){
            //if online
            GlideApp.with(context)
                //storage reference
                .load(imageRef)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .error(R.drawable.community_not_selected)
                //image view
                .into(imageView)

            if (clearCache){
                // Clear Glide memory cache
                Glide.get(context).clearMemory()
            }
        } else {
            GlideApp.with(context)
                //storage reference
                .load(R.drawable.baseline_image_24)
                //image view
                .into(imageView)
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