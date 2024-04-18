package com.example.synthronize.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
}