package com.example.synthronize

import android.app.DownloadManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
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
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.GlideApp
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.util.UUID

class ViewMedia : AppCompatActivity() {

    private lateinit var binding:ActivityViewMediaBinding
    private lateinit var cacheDataSourceFactory: DataSource.Factory
    private lateinit var videoUri: Uri
    private var simpleCache: SimpleCache? = null
    private var player: SimpleExoPlayer? = null
    private var mediaType = ""
    private var filename = ""
    private var isUrl = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaType = intent.getStringExtra("type").toString()
        isUrl = intent.getBooleanExtra("isUrl", false)
        filename = intent.getStringExtra("filename").toString()

        setupCache()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.photoView.setOnClickListener {
            if (binding.mainToolbar.visibility == View.VISIBLE){
                binding.mainToolbar.visibility = View.INVISIBLE
                binding.divider1.visibility = View.INVISIBLE
            } else {
                binding.mainToolbar.visibility = View.VISIBLE
                binding.divider1.visibility = View.VISIBLE
            }
        }

        if (isUrl){
            //Viewing Post
            binding.optionsBtn.visibility = View.VISIBLE
            binding.optionsBtn.setOnClickListener {
                openMenu()
            }

            if (mediaType == "Video"){
                binding.playerView.visibility = View.VISIBLE
                FirebaseUtil().retrieveCommunityContentVideoRef(filename).downloadUrl.addOnSuccessListener {uri ->
                    videoUri = uri
                    initializeExoplayer()

                    //open menu upon long press
                    binding.playerView.setOnLongClickListener {
                        openMenu()
                        true
                    }

                }.addOnFailureListener {
                    Log.e("Firebase Storage", it.toString())
                }

            } else if (mediaType == "Image"){
                binding.photoView.visibility = View.VISIBLE
                GlideApp.with(this)
                    .load(FirebaseUtil().retrieveCommunityContentImageRef(filename))
                    .into(binding.photoView)

                //open menu upon long press
                binding.photoView.setOnLongClickListener {
                    openMenu()
                    true
                }
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
                binding.photoView.visibility = View.VISIBLE
                val imageUri = intent.getParcelableExtra<Uri>("IMAGE_URI")
                if (imageUri != null) {
                    GlideApp.with(this)
                        .load(imageUri)
                        .into(binding.photoView)
                }
            }
        }
    }

    private fun openMenu() {
        val menuDialogBinding = DialogMenuBinding.inflate(layoutInflater)
        val menuDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(menuDialogBinding.root))
            .setCancelable(true)
            .setExpanded(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        menuDialogBinding.option1.visibility = View.VISIBLE
        menuDialogBinding.optionIcon1.setImageResource(R.drawable.baseline_download_24)
        menuDialogBinding.optiontitle1.text = "Download $mediaType"
        menuDialogBinding.optiontitle1.setOnClickListener {
            Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()
            downloadFileFromFirebase()
            menuDialog.dismiss()
        }

        menuDialog.show()
    }



    private fun downloadFileFromFirebase() {
        when(mediaType){
            "Image" -> {
                val storageRef = FirebaseUtil().retrieveCommunityContentImageRef(filename)
                //get file extension
                storageRef.metadata.addOnCompleteListener { metadata ->
                    if (metadata.isSuccessful){
                        val contentType = metadata.result.contentType // e.g., "image/jpeg", "application/pdf"
                        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                        // Get the file's download URL
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Get the Downloads directory
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            // Check for existing files and modify the name if necessary
                            val finalFileName = getUniqueFileName(downloadsDir, "Image_${UUID.randomUUID()}.$extension")
                            // Use DownloadManager to handle the download
                            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val request = DownloadManager.Request(uri)
                            request.setTitle(finalFileName)
                            request.setDescription("File is being downloaded.")
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                finalFileName
                            )
                            // Enqueue the download
                            downloadManager.enqueue(request)
                        }.addOnFailureListener { exception ->
                            // Handle any errors
                            Toast.makeText(this, "Download Failed", Toast.LENGTH_SHORT).show()
                            exception.printStackTrace()
                        }
                    }
                }

            }
            "Video" -> {
                val storageRef = FirebaseUtil().retrieveCommunityContentVideoRef(filename)
                //get file extension
                storageRef.metadata.addOnCompleteListener { metadata ->
                    if (metadata.isSuccessful){
                        val contentType = metadata.result.contentType // e.g., "image/jpeg", "application/pdf"
                        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                        // Get the file's download URL
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Get the Downloads directory
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            // Check for existing files and modify the name if necessary
                            val finalFileName = getUniqueFileName(downloadsDir, "Video_${UUID.randomUUID()}.$extension")
                            // Use DownloadManager to handle the download
                            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val request = DownloadManager.Request(uri)
                            request.setTitle(finalFileName)
                            request.setDescription("File is being downloaded.")
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                finalFileName
                            )
                            // Enqueue the download
                            downloadManager.enqueue(request)
                        }.addOnFailureListener { exception ->
                            // Handle any errors
                            Toast.makeText(this, "Download Failed", Toast.LENGTH_SHORT).show()
                            exception.printStackTrace()
                        }
                    }
                }
            }
        }

    }


    private fun getUniqueFileName(directory: File, fileName: String): String {
        var newFileName = fileName
        var file = File(directory, newFileName)
        var counter = 1

        // Loop to find a unique file name
        while (file.exists()) {
            val fileNameWithoutExtension = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".", "")
            newFileName = if (extension.isNotEmpty()) {
                "$fileNameWithoutExtension ($counter).$extension"
            } else {
                "$fileNameWithoutExtension ($counter)"
            }
            file = File(directory, newFileName)
            counter++
        }
        return newFileName
    }

    override fun onBackPressed() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        Handler(Looper.getMainLooper()).postDelayed({
            releasePlayer()
            simpleCache?.release()
            simpleCache = null
            super.onBackPressed()
        }, 500)
    }


    // Handles screen orientation changes without restarting the activity
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Get the PlayerView layout parameters
        val params = binding.playerView.layoutParams

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Full screen in landscape
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Use FIT or ZOOM
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Default size in portrait
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        // Apply new layout parameters
        binding.playerView.layoutParams = params
    }


    private fun setupCache() {
        // Set up cache eviction policy and database provider for caching
        val cacheSize = 100L * 1024 * 1024 // 100 MB
        val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        val databaseProvider = ExoDatabaseProvider(this)
        val cacheDir = File(cacheDir, "media") // Create a subdirectory for media cache

        // Initialize the SimpleCache with cache directory, evictor, and database provider
        simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)


        // Set up HTTP Data Source Factory for network requests
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()

        // Create a DefaultDataSource.Factory for loading data from upstream
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // Create a CacheDataSource.Factory with the above data source factory and cache
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache!!)
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

            binding.playerView.setControllerVisibilityListener {visibility ->
                when (visibility) {
                    View.VISIBLE -> {
                        // The player controls are visible
                        // shows main toolbar
                        binding.mainToolbar.visibility = View.VISIBLE
                        binding.divider1.visibility = View.VISIBLE
                    }
                    View.GONE -> {
                        // hides main toolbar
                        binding.mainToolbar.visibility = View.GONE
                        binding.divider1.visibility = View.GONE
                    }
                }
            }

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
