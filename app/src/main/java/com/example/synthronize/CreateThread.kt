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
import com.example.synthronize.databinding.ActivityCreateThreadBinding
import com.example.synthronize.model.ForumsModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import java.util.UUID

class CreateThread : AppCompatActivity() {
    private lateinit var binding:ActivityCreateThreadBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectedImageUri: Uri
    private lateinit var selectedVideoUri: Uri
    private lateinit var uriHashMap: HashMap<String, Uri>
    private lateinit var communityId:String
    private lateinit var forumsModel: ForumsModel
    private lateinit var postId:String
    private var contentList:ArrayList<String> = ArrayList()
    private var canPost:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCreateThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        postId=intent.getStringExtra("postId").toString()

        if (postId == "null" || postId.isEmpty()){
            //For New Post
            bindButtons()
            AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.profileCIV)
        } else {
            //For Existing Post to edit
            FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(postId).get().addOnSuccessListener {
                forumsModel = it.toObject(ForumsModel::class.java)!!
                binding.captionEdtTxt.setText(forumsModel.caption)
                communityId = forumsModel.communityId
                AppUtil().setUserProfilePic(this, forumsModel.ownerId, binding.profileCIV)
                if (forumsModel.contentList.isNotEmpty()){
                    for (filename in forumsModel.contentList){
                        contentList = ArrayList(forumsModel.contentList)
                        getFileUriFromFirebase(filename)
                    }
                    bindButtons()
                } else {
                    bindButtons()
                }

            }
        }





        //Launcher for user profile pic and user cover pic
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

        //BIND DIALOG
        binding.addImageBtn.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
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


    private fun bindButtons(){
        if (postId == "null" || postId.isEmpty()){
            binding.postBtn.text = "Post"
            binding.postBtn.setOnClickListener {
                if (binding.captionEdtTxt.text.toString().isNotEmpty() || ::uriHashMap.isInitialized){
                    addPost(){isUploaded ->
                        if (isUploaded)
                            Handler().postDelayed({
                                this.finish()
                            }, 2000)
                        else
                            Toast.makeText(this, "Error occurred while uploading", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            binding.postBtn.text = "Save"
            binding.postBtn.setOnClickListener {
                if (binding.captionEdtTxt.text.toString().isNotEmpty() || ::uriHashMap.isInitialized){
                    addPost(){isUploaded ->
                        if (isUploaded)
                            Handler().postDelayed({
                                this.finish()
                            }, 2000)
                        else
                            Toast.makeText(this, "Error occurred while saving", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.addImageBtn.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        binding.backBtn.setOnClickListener {
            //TODO dialog message
            this.finish()
        }
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



    private fun addVideo(selectedVideo: Uri, existingFilename: String = "") {
        //TODO
        //creates video id
        val userId = FirebaseUtil().currentUserUid()
        var filename = existingFilename

        if (filename.isEmpty()){
            filename = "$userId-Video-${UUID.randomUUID()}"
        }

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

        if (existingFilename.isEmpty()){
            //immediately uploads video to the firebase storage
            uploadVideo(filename, selectedVideo, progressBar)
        }

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


    private fun getFileUriFromFirebase(filename: String) {
        // Create a storage reference from the Firebase Storage instance
        val fileType = filename.split('-')[1]
        if (fileType == "Video"){
            FirebaseUtil().retrieveCommunityContentVideoRef(filename).downloadUrl.addOnSuccessListener {
                selectedVideoUri = it
                addVideo(selectedVideoUri, filename)
            }.addOnFailureListener {
                Toast.makeText(this, "An error has occurred while downloading, please try again", Toast.LENGTH_SHORT).show()
            }
        } else if (fileType == "Image"){
            FirebaseUtil().retrieveCommunityContentImageRef(filename).downloadUrl.addOnSuccessListener {
                selectedImageUri = it
                addImage(selectedImageUri)
            }.addOnFailureListener {
                Toast.makeText(this, "An error has occurred while downloading, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun addImage(selectedImage: Uri){
        //creates image id
        val userId = FirebaseUtil().currentUserUid()
        val timestamp = Timestamp.now()
        val imageId = "$userId-Image-$timestamp"

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

        //inserts selected image to Image View
        Glide.with(this).load(selectedImage)
            .into(postImage)
        verticalLayout.addView(postImage)

        //adds image to uri hashmap
        addUriToHashMap(imageId, selectedImageUri)

        //creates cancel button
        val cancelBtn = ImageButton(this)
        cancelBtn.setImageResource(R.drawable.cancel_icon)
        cancelBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        cancelBtn.setOnClickListener {
            binding.mainPostLayout.removeView(verticalLayout)
            //removes image
            removeUriFromHashMap(imageId)
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

    private fun addUriToHashMap(imageId:String, uri: Uri){
        if (!::uriHashMap.isInitialized)
            uriHashMap = HashMap()
        uriHashMap[imageId] = uri
    }

    private fun addPost(callback: (Boolean) -> Unit){
        val caption = binding.captionEdtTxt.text.toString()
        var delay: Long = 1000

        // Check if we're editing or creating a new post
        if (postId == "null" || postId.isEmpty()) {
            // Creating a new post
            FirebaseUtil().retrieveCommunityForumsCollection(communityId).add(ForumsModel()).addOnSuccessListener {
                postId = it.id // Get the new post ID

                // Continue with adding media content and updating the post
                uploadMediaAndSavePost(it.id, caption, callback, delay)
            }.addOnFailureListener {
                callback(false)
            }
        } else {
            // Editing an existing post
            FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(postId).get().addOnSuccessListener {
                uploadMediaAndSavePost(postId, caption, callback, delay)
            }.addOnFailureListener {
                callback(false)
            }
        }
    }

    private fun uploadMediaAndSavePost(postId: String, caption: String, callback: (Boolean) -> Unit, delay: Long) {
        val contentList: ArrayList<String> = ArrayList()

        if (::uriHashMap.isInitialized) {
            for (data in uriHashMap) {
                // Upload media files to Firebase storage
                FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                contentList.add(data.key)
            }
        }

        val threadModel = ForumsModel(
            postId = postId,
            ownerId = FirebaseUtil().currentUserUid(),
            caption = caption,
            createdTimestamp = Timestamp.now(),
            communityId = communityId,
            contentList = contentList
        )

        // Use set() to update the post instead of add()
        FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(postId).set(threadModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Handler().postDelayed({
                    Toast.makeText(this, "Your post is uploaded/updated successfully!", Toast.LENGTH_SHORT).show()
                    callback(true)
                }, delay)
            } else {
                Toast.makeText(this, "Error occurred while updating the post!", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        }
    }

}