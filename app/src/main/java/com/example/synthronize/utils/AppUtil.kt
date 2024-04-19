package com.example.synthronize.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.MainActivity
import de.hdodenhof.circleimageview.CircleImageView


class AppUtil {
    //For Images
    fun setUserProfilePic(context:Context, uid:String, civ:CircleImageView){
        FirebaseUtil().retrieveUserProfilePicRef(uid).downloadUrl.addOnCompleteListener {
            if (it.isSuccessful){
                Glide.with(context).load(it.result)
                    .apply(RequestOptions.circleCropTransform())
                    .into(civ)
            }
        }
    }
    fun setUserCoverPic(context: Context, uid: String, cover:ImageView){
        FirebaseUtil().retrieveUserCoverPicRef(uid).downloadUrl.addOnCompleteListener {
            if (it.isSuccessful){
                Glide.with(context)
                    .load(it.result)
                    .into(cover)
            }
        }
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
    //For Strings
    fun sliceMessage(string:String, intCap:Int):String{
        if (string.length > intCap){
            return "${string.slice(0..intCap)}..."
        } else {
            return string
        }
    }
}