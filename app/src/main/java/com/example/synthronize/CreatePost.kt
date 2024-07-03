package com.example.synthronize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.TypedValueCompat
import com.bumptech.glide.Glide
import com.example.synthronize.databinding.ActivityCreatePostBinding
import com.example.synthronize.model.PostModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import java.util.UUID

class CreatePost : AppCompatActivity() {
    private lateinit var binding:ActivityCreatePostBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri: Uri
    private lateinit var selectedVideoUri: Uri
    private lateinit var uriHashMap: HashMap<String, Uri>
    private lateinit var communityId:String
    private var contentList:ArrayList<String> = ArrayList()
    private var canPost:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()

        //Launcher for user content image
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Image is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    selectedImageUri = data.data!!
                    //adds the selected image to the dialog layout
                    addImage(selectedImageUri)
                }
            }
        }

        //Launcher for user content video
        videoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            //Video is selected
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                if (data != null && data.data != null){
                    selectedVideoUri = data.data!!
                    //adds the selected video to the dialog layout
                    addVideo(selectedVideoUri)
                }
            }
        }

        //BIND DIALOG
        binding.addImageBtn.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }
        binding.addVideoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setType("video/*")
            videoPickerLauncher.launch(intent)
        }
        binding.backBtn.setOnClickListener {
            //TODO dialog message
            this.finish()
        }
        binding.postBtn.setOnClickListener {
            if (binding.captionEdtTxt.text.toString().isNotEmpty() || ::uriHashMap.isInitialized){
                addPost(){isUploaded ->
                    if (isUploaded)
                        Handler().postDelayed({
                            this.finish()
                        }, 2000)
                    else
                        Toast.makeText(this, "Error occured while uploading", Toast.LENGTH_SHORT).show()
                }
            }
        }
        AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.profileCIV)
    }

    private fun uploadVideo(filename: String, selectedVideoUri: Uri, progressBar: ProgressBar){
        canPost = false
        FirebaseUtil().retrieveCommunityContentVideoRef(filename).putFile(selectedVideoUri).addOnSuccessListener {
            Toast.makeText(this, "Video Uploaded Successfully", Toast.LENGTH_SHORT).show()
            contentList.add(filename)
            canPost = true
        }.addOnFailureListener {
            Toast.makeText(this, "Failed To Upload Video", Toast.LENGTH_SHORT).show()
        }.addOnProgressListener {
            progressBar.max = Math.toIntExact(it.totalByteCount)
            progressBar.progress = Math.toIntExact(it.bytesTransferred)
        }
    }

    private fun addVideo(selectedVideo: Uri) {
        //TODO
        //creates video id
        val userId = FirebaseUtil().currentUserUid()
        val filename = "$userId-Video-${UUID.randomUUID()}"

        //Creates linear layout for image
        val verticalLayout = LinearLayout(this)
        verticalLayout.orientation = LinearLayout.VERTICAL
        val linearParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        verticalLayout.layoutParams = linearParams

        //creates thumbnail
        val postVideo = ImageView(this)
        val imageDpToPx = TypedValueCompat.dpToPx(400F, resources.displayMetrics)
        val imageParams = LinearLayout.LayoutParams(imageDpToPx.toInt(), imageDpToPx.toInt())
        postVideo.layoutParams = imageParams

        //make the thumbnail clickable
        postVideo.setOnClickListener {
            val intent = Intent(this, ViewMedia::class.java).apply {
                putExtra("type", "Image")
                putExtra("VIDEO_URI", selectedVideo)
            }
            startActivity(intent)
        }


        //inserts selected image to Image View
        Glide.with(this).load(selectedVideo)
            .into(postVideo)
        verticalLayout.addView(postVideo)

        // Adds the ProgressBar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.VISIBLE // Show the progress bar initially
        }
        verticalLayout.addView(progressBar)

        //adds image to uri hashmap
        addUriToHashMap(filename, selectedVideoUri)

        //immediately uploads video to the firebase storage
        uploadVideo(filename, selectedVideo, progressBar)

        //creates cancel button
        val cancelBtn = ImageButton(this)
        cancelBtn.setImageResource(R.drawable.cancel_icon)
        cancelBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        cancelBtn.setOnClickListener {
            binding.mainPostLayout.removeView(verticalLayout)
            //removes image
            removeUriFromHashMap(filename)
        }
        val cancelBtnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cancelBtn.layoutParams = cancelBtnParams
        verticalLayout.addView(cancelBtn)

        //adds the views to the main layout
        binding.mainPostLayout.addView(verticalLayout)
    }

    private fun addImage(selectedImage:Uri){
        //creates image filename
        val userId = FirebaseUtil().currentUserUid()
        val fileName = "$userId-Image-${UUID.randomUUID()}"

        //Creates linear layout for image
        val verticalLayout = LinearLayout(this)
        verticalLayout.orientation = LinearLayout.VERTICAL
        val linearParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        verticalLayout.layoutParams = linearParams

        //creates image view
        val postImage = ImageView(this)
        val imageDpToPx = TypedValueCompat.dpToPx(400F, resources.displayMetrics)
        val imageParams = LinearLayout.LayoutParams(imageDpToPx.toInt(), imageDpToPx.toInt())
        postImage.layoutParams = imageParams

        //make the thumbnail clickable
        postImage.setOnClickListener {
            val intent = Intent(this, ViewMedia::class.java).apply {
                putExtra("type", "Image")
                putExtra("IMAGE_URI", selectedImage)
            }
            startActivity(intent)
        }

        //inserts selected image to Image View
        Glide.with(this).load(selectedImage)
            .into(postImage)
        verticalLayout.addView(postImage)

        //adds image to uri hashmap
        addUriToHashMap(fileName, selectedImageUri)

        //creates cancel button
        val cancelBtn = ImageButton(this)
        cancelBtn.setImageResource(R.drawable.cancel_icon)
        cancelBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        cancelBtn.setOnClickListener {
            binding.mainPostLayout.removeView(verticalLayout)
            //removes image
            removeUriFromHashMap(fileName)
        }
        val cancelBtnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cancelBtn.layoutParams = cancelBtnParams
        verticalLayout.addView(cancelBtn)

        //adds the views to the main layout
        binding.mainPostLayout.addView(verticalLayout)
    }

    private fun removeUriFromHashMap(contentId:String){
        uriHashMap.remove(contentId)
    }

    private fun addUriToHashMap(contentId:String, uri: Uri){
        if (!::uriHashMap.isInitialized)
            uriHashMap = HashMap()
        uriHashMap[contentId] = uri
    }

    private fun addPost(callback: (Boolean) -> Unit){
        val tempModel = PostModel()
        val caption = binding.captionEdtTxt.text.toString()
        var delay:Long = 1000

        if (canPost){
            FirebaseUtil().retrieveCommunityFeedsCollection(communityId).add(tempModel).addOnSuccessListener {

                if (::uriHashMap.isInitialized){
                    for (data in uriHashMap){
                        //data key is imageFileName and value is uri or actual image
                        //uploads image to the firebase storage
                        val key = data.key.split('-')
                        if (key[1] == "Image") {
                            FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                            contentList.add(data.key)
                            delay += 100
                        }

                    }
                }

                //get new id from firestore and store it in feedId of the PostModel
                val postModel = PostModel(
                    postId = it.id,
                    ownerId = FirebaseUtil().currentUserUid(),
                    caption = caption,
                    createdTimestamp = Timestamp.now(),
                    communityId = communityId,
                    contentList = contentList
                )

                //replaces temp model with feeds model
                FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(it.id).set(postModel).addOnCompleteListener {task ->
                    if (task.isSuccessful) {
                        Handler().postDelayed({
                            Toast.makeText(this, "Your post is uploaded successfully!", Toast.LENGTH_SHORT).show()
                            callback(true)
                        }, delay)
                    } else {
                        Toast.makeText(this, "Error has occurred!", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }
            }.addOnFailureListener {
                callback(false)
            }


        } else {
            //the post is not ready to be uploaded
            Toast.makeText(this, "The video is still uploading", Toast.LENGTH_SHORT).show()
        }
    }
}