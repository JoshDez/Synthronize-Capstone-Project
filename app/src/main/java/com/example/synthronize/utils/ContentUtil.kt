package com.example.synthronize.utils

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.util.TypedValueCompat
import com.example.synthronize.databinding.ItemFeedBinding

class ContentUtil {

    //FOR FEED
    fun addContentToViewFlipper(context: Context, feedsBinding: ItemFeedBinding, contentList: List<String>){
        for (content in contentList){
            val temp = content.split('-')
            if (temp[1] == "Image"){
                //If the content is Image
                //adds the image view to the view flipper
                feedsBinding.contentLayout.addView(getImageView(context, content))
            } else if (temp[1] == "Video"){
                //If the content is Video
                //TODO to be implemented
            }
        }
    }

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