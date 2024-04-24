package com.example.synthronize.utils
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.MainActivity
import com.example.synthronize.R
import de.hdodenhof.circleimageview.CircleImageView


class AppUtil {
    //For Images
    fun setUserProfilePic(context:Context, uid: String, civ:CircleImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveUserProfilePicRef(uid))
            .error(R.drawable.profile_not_selected)
            .apply(RequestOptions.circleCropTransform())
            //image view
            .into(civ)
    }
    fun setUserCoverPic(context: Context, uid: String, cover:ImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveUserCoverPicRef(uid))
            //image view
            .into(cover)
    }

    fun setCommunityProfilePic(context:Context, uid: String, civ:CircleImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveCommunityProfilePicRef(uid))
            .error(R.drawable.community_not_selected)
            .apply(RequestOptions.circleCropTransform())
            //image view
            .into(civ)
    }

    //For Heading Back to Main Activity
    fun headToMainActivity(context: Context, fragment:String, delay:Long = 0, communityId: String = "") {
        Handler().postDelayed({
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("fragment", fragment)
            intent.putExtra("communityId", communityId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
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
}