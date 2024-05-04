package com.example.synthronize.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.core.util.TypedValueCompat

class ContentUtil {

    //FOR FEED
    fun getImageView(context: Context, imageId:String): ImageView{
        //creates image view
        val imageView = ImageView(context)
        val imageDpToPx = TypedValueCompat.dpToPx(400F, context.resources.displayMetrics)
        val imageParams = LinearLayout.LayoutParams(imageDpToPx.toInt(), imageDpToPx.toInt())
        imageView.layoutParams = imageParams

        //adds image to the image view
        AppUtil().setImageContent(context, imageId, imageView)

        return imageView
    }

    fun createSpaceView(context: Context): View {
        val spaceView = Space(context)
        val heightInDp = 20
        val heightInPixels = (heightInDp * context.resources.displayMetrics.density).toInt()
        spaceView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightInPixels)
        return spaceView
    }

}