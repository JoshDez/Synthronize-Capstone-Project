package com.example.synthronize

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import java.io.File
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.example.synthronize.databinding.ActivityViewMediaBinding
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.GlideApp
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor

class ViewMedia : AppCompatActivity() {

    private lateinit var binding:ActivityViewMediaBinding
    private lateinit var cacheDataSourceFactory: DataSource.Factory
    private lateinit var videoUri: Uri
    private var player: SimpleExoPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaType = intent.getStringExtra("type")
        val isUrl = intent.getBooleanExtra("isUrl", false)
        val filename = intent.getStringExtra("filename").toString()

        setupCache()

        if (isUrl){
            //Viewing Post
            if (mediaType == "Video"){
                binding.playerView.visibility = View.VISIBLE
                FirebaseUtil().retrieveCommunityContentVideoRef(filename).downloadUrl.addOnSuccessListener {uri ->
                    videoUri = uri
                    initializeExoplayer()
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
                val intentUri = intent.getParcelableExtra<Uri>("VIDEO_URI")
                if (intentUri != null) {
                    videoUri = intentUri
                    initializeExoplayer()
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
            releasePlayer()
            super.onBackPressed()
        }, 500)
    }

    private fun setupCache() {
        // Set up cache eviction policy and database provider for caching
        val cacheSize = 100L * 1024 * 1024 // 100 MB
        val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        val databaseProvider = ExoDatabaseProvider(this)

        // Initialize the SimpleCache with cache directory, evictor, and database provider
        val cacheDir = File(cacheDir, "media") // Create a subdirectory for media cache

        // Set up HTTP Data Source Factory for network requests
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()

        // Create a DefaultDataSource.Factory for loading data from upstream
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // Create a CacheDataSource.Factory with the above data source factory and cache
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(SimpleCache(cacheDir, evictor, databaseProvider))
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun initializeExoplayer(){
        if (::videoUri.isInitialized){
            val mediaItem = MediaItem.fromUri(videoUri)
            player = SimpleExoPlayer.Builder(this)
                .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
                .build()

            // Attach player to the player view
            binding.playerView.player = player

//          Prepare the player with the media item
            player?.apply {
                setMediaItem(mediaItem)
                playWhenReady = true
                seekTo(0, 0)
                repeatMode = Player.REPEAT_MODE_OFF
                prepare()
            }
        } else {
            return
        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializeExoplayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 || player == null) {
            initializeExoplayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (player == null) {
            return
        }
        player!!.release()
        player = null
    }
}
