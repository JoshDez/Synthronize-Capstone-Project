package com.example.synthronize

import android.content.pm.ActivityInfo
import android.media.browse.MediaBrowser.MediaItem
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import com.example.synthronize.databinding.ActivityViewMediaBinding
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.GlideApp

class ViewMedia : AppCompatActivity() {

    private lateinit var binding:ActivityViewMediaBinding
    private lateinit var player:ExoPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaType = intent.getStringExtra("type")
        val isUrl = intent.getBooleanExtra("isUrl", false)
        val filename = intent.getStringExtra("filename").toString()


        if (isUrl){
            if (mediaType == "Video"){

                binding.playerView.visibility = View.VISIBLE
                initializeExoplayer()
                FirebaseUtil().retrieveCommunityContentVideoRef(filename).downloadUrl.addOnSuccessListener {uri ->
                    val mediaItem = androidx.media3.common.MediaItem.fromUri(uri)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }.addOnFailureListener {
                    Log.e("Firebase Storage", it.toString())
                }

            } else if (mediaType == "Image"){
                binding.imageView.visibility = View.VISIBLE
                GlideApp.with(this)
                    .load(FirebaseUtil().retrieveCommunityContentImageRef(filename))
                    .into(binding.imageView)
            }

        } else {
            //For Create Post
            if (mediaType == "Video"){
                binding.playerView.visibility = View.VISIBLE
                initializeExoplayer()
                val videoUri = intent.getParcelableExtra<Uri>("VIDEO_URI")
                if (videoUri != null) {



                }
            } else if (mediaType == "Image") {
                binding.imageView.visibility = View.VISIBLE
                val imageUri = intent.getParcelableExtra<Uri>("IMAGE_URI")
                if (imageUri != null) {
                    GlideApp.with(this)
                        .load(imageUri)
                        .into(binding.imageView)
                }
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        Handler(Looper.getMainLooper()).postDelayed({
            super.onBackPressed()
        }, 500)
    }

    private fun initializeExoplayer(){
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
    }
}