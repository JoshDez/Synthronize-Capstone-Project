package com.example.synthronize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import com.example.synthronize.model.ForumsModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp

class CreateThread : AppCompatActivity() {
    private lateinit var binding:ActivityCreateThreadBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri: Uri
    private lateinit var uriHashMap: HashMap<String, Uri>
    private lateinit var communityId:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()

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

    private fun addImage(selectedImage:Uri){
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
        val tempModel = ForumsModel()
        val contentList:ArrayList<String> = ArrayList()
        val caption = binding.captionEdtTxt.text.toString()
        var delay:Long = 1000

        FirebaseUtil().retrieveCommunityForumsCollection(communityId).add(tempModel).addOnSuccessListener {

            if (::uriHashMap.isInitialized){
                for (data in uriHashMap){
                    //data key is imageId and data value is uri
                    //uploads image to the firebase storage
                    FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                    contentList.add(data.key)
                    delay += 100
                }
            }

            //get new id from firestore and store it in feedId of the PostModel
            val threadModel = ForumsModel(
                postId = it.id,
                ownerId = FirebaseUtil().currentUserUid(),
                caption = caption,
                createdTimestamp = Timestamp.now(),
                communityId = communityId,
                contentList = contentList
            )

            //replaces temp model with feeds model
            FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(it.id).set(threadModel).addOnCompleteListener { task ->
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
    }
}