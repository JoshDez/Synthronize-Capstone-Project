package com.example.synthronize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.TypedValueCompat
import com.bumptech.glide.Glide
import com.example.synthronize.databinding.ActivityCreateThreadBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.ForumModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.util.UUID

class CreateThread : AppCompatActivity() {
    private lateinit var binding:ActivityCreateThreadBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri: Uri
    private lateinit var uriHashMap: HashMap<String, Uri>
    private lateinit var communityId:String
    private lateinit var forumId:String
    private lateinit var existingForumsModel: ForumModel
    private var contentList:ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        forumId = intent.getStringExtra("forumId").toString()


        if (forumId == "null" || forumId.isEmpty()){
            //For New Thread
            bindButtons()
            AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.profileCIV)

        } else {
            //For Existing Post to edit
            FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(forumId).get().addOnSuccessListener {
                existingForumsModel = it.toObject(ForumModel::class.java)!!
                binding.captionEdtTxt.setText(existingForumsModel.caption)
                communityId = existingForumsModel.communityId
                AppUtil().setUserProfilePic(this, existingForumsModel.ownerId, binding.profileCIV)
                if (existingForumsModel.contentList.isNotEmpty()){
                    for (filename in existingForumsModel.contentList){
                        contentList = ArrayList(existingForumsModel.contentList)
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
    }

    private fun bindButtons(){

        if (forumId == "null" || forumId.isEmpty()){
            binding.postBtn.text = "Post"
        } else {
            binding.postBtn.text = "Save"
        }

        binding.postBtn.setOnClickListener {
            if (binding.captionEdtTxt.text.toString().isNotEmpty() || ::uriHashMap.isInitialized){
                addThread()
            } else {
                Toast.makeText(this, "Please write your topic", Toast.LENGTH_SHORT).show()
            }
        }

        binding.addImageBtn.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        if (forumId.isNotEmpty() && forumId != "null"){
            binding.toolbarTitleTV.text = "Edit Thread"
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getFileUriFromFirebase(filename: String) {
        // Create a storage reference from the Firebase Storage instance
        val fileType = filename.split('-')[1]
        if (fileType == "Image"){
            FirebaseUtil().retrieveCommunityContentImageRef(filename).downloadUrl.addOnSuccessListener {
                selectedImageUri = it
                addImage(selectedImageUri, filename)
            }.addOnFailureListener {
                Toast.makeText(this, "An error has occurred while downloading, please try again", Toast.LENGTH_SHORT).show()
            }
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
    private fun addThread(){
        val tempModel = ForumModel()
        val caption = binding.captionEdtTxt.text.toString()
        var delay:Long = 1000

        if (caption.isEmpty()){
            Toast.makeText(this, "Please write your topic", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(caption)){
            Toast.makeText(this, "Caption contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (forumId == "null" || forumId.isEmpty()){
            //Upload new forum
            FirebaseUtil().retrieveCommunityForumsCollection(communityId).add(tempModel).addOnSuccessListener {
                if (::uriHashMap.isInitialized){
                    for (data in uriHashMap){
                        //uploads image to the firebase storage
                        val key = data.key.split('-')
                        if (key[1] == "Image") {
                            FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                            contentList.add(data.key)
                            delay += 100
                        }

                    }
                }

                //get new id from firestore and store it in feedId of the ForumModel
                val forumModel = ForumModel(
                    forumId = it.id,
                    ownerId = FirebaseUtil().currentUserUid(),
                    caption = caption,
                    createdTimestamp = Timestamp.now(),
                    communityId = communityId,
                    contentList = contentList
                )
                //uploads new forumModel to firebase
                uploadToFirebase(forumModel.forumId, forumModel, "Your post is uploaded successfully!", delay)

            }.addOnFailureListener {
                Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
            }


        } else {
            //Saving edited form
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

            //Edit forum
            val forumModel = ForumModel(
                forumId = forumId,
                caption = caption,
                contentList = contentList,
                communityId = communityId,
                ownerId = existingForumsModel.ownerId,
                createdTimestamp = existingForumsModel.createdTimestamp,
                upvoteList = existingForumsModel.upvoteList,
                downvoteList = existingForumsModel.downvoteList,
            )

            //uploads new postModel to firebase
            uploadToFirebase(forumId, forumModel, "Your post is saved successfully!", delay)

        }
    }


    private fun uploadToFirebase(forumId:String, forumModel: ForumModel, toastMsg:String, postDelay:Long){
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        if (this.forumId.isNotEmpty() && this.forumId != "null"){
            dialogLoadingBinding.messageTV.text = "Saving..."
        } else {
            dialogLoadingBinding.messageTV.text = "Uploading..."
        }

        loadingDialog.show()

        FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(forumId).set(forumModel).addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                if (this.forumId.isNotEmpty() && this.forumId != "null"){
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


    private fun deleteFilesFromFirebaseStorage(){
        //deletes files that are no longer included in content list while editing
        for (filename in existingForumsModel.contentList){
            val fileType = filename.split('-')[1]
            if (!contentList.contains(filename)){
                if (fileType == "Image"){
                    FirebaseUtil().retrieveCommunityContentImageRef(filename).delete()
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
                ::selectedImageUri.isInitialized
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }
}