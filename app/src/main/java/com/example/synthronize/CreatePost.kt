package com.example.synthronize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.TypedValueCompat
import com.bumptech.glide.Glide
import com.example.synthronize.databinding.ActivityCreatePostBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.util.UUID

class CreatePost : AppCompatActivity() {
    private lateinit var binding:ActivityCreatePostBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri: Uri
    private lateinit var selectedVideoUri: Uri
    private lateinit var uriHashMap: HashMap<String, Uri>
    private lateinit var communityId:String
    private lateinit var postId:String
    private lateinit var existingPostModel: PostModel
    private var contentList:ArrayList<String> = ArrayList()
    private var uploadedVideos:ArrayList<String> = ArrayList()
    private var canPost:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        postId = intent.getStringExtra("postId").toString()


        if (postId == "null" || postId.isEmpty()){
            //For New Post
            bindButtons()
            AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.profileCIV)
        } else {
            //For Existing Post to edit
            FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).get().addOnSuccessListener {
                existingPostModel = it.toObject(PostModel::class.java)!!
                binding.captionEdtTxt.setText(existingPostModel.caption)
                communityId = existingPostModel.communityId
                AppUtil().setUserProfilePic(this, existingPostModel.ownerId, binding.profileCIV)
                if (existingPostModel.contentList.isNotEmpty()){
                    for (filename in existingPostModel.contentList){
                        contentList = ArrayList(existingPostModel.contentList)
                        getFileUriFromFirebase(filename)
                    }
                    bindButtons()
                } else {
                    bindButtons()
                }

            }
        }

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
    }

    private fun bindButtons(){

        if (postId == "null" || postId.isEmpty()){
            binding.postBtn.text = "Post"
        } else {
            binding.postBtn.text = "Save"
        }

        binding.postBtn.setOnClickListener {
            if (binding.captionEdtTxt.text.toString().isNotEmpty() || ::uriHashMap.isInitialized){
                addPost()
            } else {
                Toast.makeText(this, "Please write what's on your mind", Toast.LENGTH_SHORT).show()
            }
        }

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
            onBackPressed()
        }
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
                addImage(selectedImageUri, filename)
            }.addOnFailureListener {
                Toast.makeText(this, "An error has occurred while downloading, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun uploadVideo(filename: String, selectedVideoUri: Uri, progressBar: ProgressBar, cancelBtn:ImageButton, verticalLayout: LinearLayout){
        canPost = false
        progressBar.visibility = View.VISIBLE

        FirebaseUtil().retrieveCommunityContentVideoRef(filename).putFile(selectedVideoUri).addOnSuccessListener {
            contentList.add(filename)
            uploadedVideos.add(filename)
            canPost = true

            //adds functionality to cancel button
            cancelBtn.setOnClickListener {
                binding.mainPostLayout.removeView(verticalLayout)
                //removes video
                removeUriFromHashMap(filename)
                FirebaseUtil().retrieveCommunityContentVideoRef(filename).delete()
            }


        }.addOnFailureListener {
            Toast.makeText(this, "Failed To Upload Video", Toast.LENGTH_SHORT).show()
        }.addOnProgressListener {
            progressBar.max = Math.toIntExact(it.totalByteCount)
            progressBar.progress = Math.toIntExact(it.bytesTransferred)
        }
    }

    private fun addVideo(selectedVideo: Uri, existingFilename: String = "") {
        //creates video id
        val userId = FirebaseUtil().currentUserUid()
        var filename = existingFilename

        if (filename.isEmpty()){
            filename = "$userId-Video-${UUID.randomUUID()}"
        }

        //Creates linear layout for Video
        val verticalLayout = LinearLayout(this)
        verticalLayout.orientation = LinearLayout.VERTICAL
        val linearParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Set inner gravity to center
        verticalLayout.gravity = Gravity.CENTER
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
            .centerCrop()
            .into(postVideo)
        verticalLayout.addView(postVideo)

        // Adds the ProgressBar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false // Use determinate mode to show actual progress
            max = 100 // Set max value
            progressDrawable = ContextCompat.getDrawable(this@CreatePost, R.drawable.custom_progress_bar)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        progressBar.visibility = View.INVISIBLE
        verticalLayout.addView(progressBar)

        //adds image to uri hashmap
        addUriToHashMap(filename, selectedVideoUri)


        //creates cancel button
        val cancelBtn = ImageButton(this)
        cancelBtn.setImageResource(R.drawable.cancel_icon)
        cancelBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        cancelBtn.setOnClickListener {
            Toast.makeText(this, "The video is still uploading..", Toast.LENGTH_SHORT).show()
        }

        val cancelBtnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cancelBtn.layoutParams = cancelBtnParams
        verticalLayout.addView(cancelBtn)

        //adds the views to the main layout
        binding.mainPostLayout.addView(verticalLayout)

        if (existingFilename.isEmpty()){
            //immediately uploads video to the firebase storage
            uploadVideo(filename, selectedVideo, progressBar, cancelBtn, verticalLayout)
        }
    }

    private fun addImage(selectedImage:Uri, existingFilename: String = ""){
        //creates image filename
        val userId = FirebaseUtil().currentUserUid()
        var fileName = existingFilename

        if (fileName.isEmpty()){
            fileName = "$userId-Image-${UUID.randomUUID()}"
        }

        //Creates linear layout for image
        val verticalLayout = LinearLayout(this)
        verticalLayout.orientation = LinearLayout.VERTICAL
        val linearParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Set inner gravity to center
        verticalLayout.gravity = Gravity.CENTER
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
        if (contentList.isNotEmpty()){
            contentList.remove(contentId)
        }
    }

    private fun addUriToHashMap(contentId:String, uri: Uri){
        if (!::uriHashMap.isInitialized)
            uriHashMap = HashMap()
        uriHashMap[contentId] = uri
    }


    private fun uploadToFirebase(postId:String, postModel: PostModel, toastMsg:String, postDelay:Long){
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        if (this.postId.isNotEmpty() && this.postId != "null"){
            dialogLoadingBinding.messageTV.text = "Saving..."
        } else {
            dialogLoadingBinding.messageTV.text = "Uploading..."
        }

        loadingDialog.show()

        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(postId).set(postModel).addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                if (this.postId.isNotEmpty() && this.postId != "null"){
                    deleteFilesFromFirebaseStorage()
                }
                Handler().postDelayed({
                    loadingDialog.dismiss()
                    this.finish()
                }, postDelay)
            } else {
                Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
                this.finish()

            }
        }
    }

    private fun addPost(){
        val tempModel = PostModel()
        val caption = binding.captionEdtTxt.text.toString()
        var delay:Long = 1000

        if (AppUtil().containsBadWord(caption)){
            Toast.makeText(this, "Caption contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (canPost && postId == "null" || postId.isEmpty()){
            //Upload new post
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
                //uploads new postModel to firebase
                uploadToFirebase(postModel.postId, postModel, "Your post is uploaded successfully!", delay)

            }.addOnFailureListener {
                Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
            }


        } else if(canPost){
            //Saving edited post
            if (::uriHashMap.isInitialized){
                for (data in uriHashMap){
                    //data key is imageFileName and value is uri or actual image
                    //uploads image to the firebase storage
                    val key = data.key.split('-')
                    if (key[1] == "Image") {
                        FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                        if (!contentList.contains(data.key)){
                            contentList.add(data.key)
                            delay += 100
                        }
                    }
                }
            }

            //Edit post
            val postModel = PostModel(
                postId = postId,
                caption = caption,
                contentList = contentList,
                communityId = communityId,
                sendPostList = existingPostModel.sendPostList,
                ownerId = existingPostModel.ownerId,
                createdTimestamp = existingPostModel.createdTimestamp,
                loveList = existingPostModel.loveList,
            )


            //uploads new postModel to firebase
            uploadToFirebase(postId, postModel, "Your post is saved successfully!", delay)

        } else {
            //the post is not ready to be uploaded
            Toast.makeText(this, "content is still uploading", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFilesFromFirebaseStorage(){
        //deletes files that are no longer included in content list while editing
        for (filename in existingPostModel.contentList){
            val fileType = filename.split('-')[1]
            if (!contentList.contains(filename)){
                if (fileType == "Image"){
                    FirebaseUtil().retrieveCommunityContentImageRef(filename).delete()
                } else if (fileType == "Video"){
                    FirebaseUtil().retrieveCommunityContentVideoRef(filename).delete()
                }
            }
        }

    }




    override fun onBackPressed() {
        if (isModified()){
            //hides keyboard
            hideKeyboard()
            //Dialog for saving user profile
            val dialogBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val dialogPlus = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(dialogBinding.root))
                .setGravity(Gravity.CENTER)
                .setBackgroundColorResId(R.color.transparent)
                .setCancelable(true)
                .create()

            dialogBinding.titleTV.text = "Warning"
            dialogBinding.messageTV.text = "Do you want to exit without saving?"

            dialogBinding.yesBtn.setOnClickListener {
                //removes uploaded videos from firebase storage
                for (video in uploadedVideos){
                    FirebaseUtil().retrieveCommunityContentVideoRef(video).delete()
                }
                dialogPlus.dismiss()
                super.onBackPressed()
            }
            dialogBinding.NoBtn.setOnClickListener {
                dialogPlus.dismiss()
            }

            dialogPlus.show()
        } else {
            super.onBackPressed()
        }
    }


    private fun isModified(): Boolean {
        return binding.captionEdtTxt.text.toString().isNotEmpty() ||
                ::selectedImageUri.isInitialized || ::selectedVideoUri.isInitialized
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }
}