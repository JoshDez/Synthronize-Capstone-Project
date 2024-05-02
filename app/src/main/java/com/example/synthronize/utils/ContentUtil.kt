package com.example.synthronize.utils

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.util.TypedValueCompat
import com.example.synthronize.databinding.ItemFeedBinding

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

}