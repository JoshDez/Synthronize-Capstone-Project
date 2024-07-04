package com.example.synthronize.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.core.util.TypedValueCompat
import com.example.synthronize.R
import com.example.synthronize.ViewMedia

class ContentUtil {

    //FOR FEED
    fun getImageView(context: Context, filename:String): ImageView{

        //creates image view
        val imageView = ImageView(context)
        val imageDpToPx = TypedValueCompat.dpToPx(400F, context.resources.displayMetrics)
        val imageParams = LinearLayout.LayoutParams(imageDpToPx.toInt(), imageDpToPx.toInt())
        imageView.layoutParams = imageParams

        setImageContent("Image" ,context, filename, imageView)

        //set onclick listener
        imageView.setOnClickListener {
            val intent = Intent(context, ViewMedia::class.java).apply {
                putExtra("type", "Image")
                putExtra("isUrl", true)
                putExtra("filename", filename)
            }
            context.startActivity(intent)
        }

        return imageView
    }

    fun getVideoThumbnail(context: Context, filename: String): FrameLayout {

        // Create a FrameLayout to hold the video thumbnail and play icon
        val frameLayout = FrameLayout(context)
        val imageDpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400F, context.resources.displayMetrics).toInt()
        val frameParams = LinearLayout.LayoutParams(imageDpToPx, imageDpToPx)
        frameLayout.layoutParams = frameParams

        // Set black background color to the FrameLayout
        frameLayout.setBackgroundColor(Color.BLACK)

        // Create ImageView for the video thumbnail
        val imageView = ImageView(context)
        val imageParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        imageView.layoutParams = imageParams

        // Add the video thumbnail to the image view
        setImageContent("Video" ,context, filename, imageView)

        // Create ImageView for the foreground (play icon)
        val playIconView = ImageView(context)
        val playIconParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        playIconParams.gravity = Gravity.CENTER
        playIconView.layoutParams = playIconParams
        playIconView.setImageResource(R.drawable.play_icon) // Replace with your play icon resource

        //set onclick listener
        imageView.setOnClickListener {
            val intent = Intent(context, ViewMedia::class.java).apply {
                putExtra("type", "Video")
                putExtra("isUrl", true)
                putExtra("filename", filename)
            }
            context.startActivity(intent)
        }

        // Add the image views to the frame layout
        frameLayout.addView(imageView)
        frameLayout.addView(playIconView)

        return frameLayout
    }

    private fun setImageContent(type:String, context: Context, filename: String, imageView:ImageView){
        if (type == "Image"){
            GlideApp.with(context)
                //storage reference
                .load(FirebaseUtil().retrieveCommunityContentImageRef(filename))
                .error(R.drawable.baseline_image_24)
                //image view
                .into(imageView)
        } else if (type == "Video"){
            GlideApp.with(context)
                //storage reference
                .load(FirebaseUtil().retrieveCommunityContentVideoRef(filename))
                .error(R.drawable.baseline_image_24)
                //image view
                .into(imageView)
        }

    }

    fun createSpaceView(context: Context): View {
        val spaceView = Space(context)
        val heightInDp = 20
        val heightInPixels = (heightInDp * context.resources.displayMetrics.density).toInt()
        spaceView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightInPixels)
        return spaceView
    }

}