package com.example.synthronize

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityViewMediaBinding
import com.example.synthronize.utils.GlideApp

class ViewMedia : AppCompatActivity() {

    private lateinit var binding:ActivityViewMediaBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaType = intent.getStringExtra("type")

        if (mediaType == "Video"){
            binding.videoView.visibility = View.VISIBLE
            val videoUri = intent.getParcelableExtra<Uri>("VIDEO_URI")
            if (videoUri != null) {
                val mediaController = MediaController(this)
                mediaController.setAnchorView(binding.videoView)

                binding.videoView.setMediaController(mediaController)
                binding.videoView.setVideoURI(videoUri)
                binding.videoView.requestFocus()
                binding.videoView.start()
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



        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }
}