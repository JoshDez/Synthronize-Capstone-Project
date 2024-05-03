package com.example.synthronize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.synthronize.adapters.FeedsAdapter
import com.example.synthronize.databinding.DialogCreatePostBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentFeedsBinding
import com.example.synthronize.model.FeedsModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class FeedsFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment() {

    private lateinit var binding: FragmentFeedsBinding
    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var feedsAdapter: FeedsAdapter

    //For Create Post Dialog
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri:Uri
    private lateinit var postDialogBinding:DialogCreatePostBinding
    private lateinit var uriHashMap: HashMap<String, Uri>
    private var isNewPost:Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFeedsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()

            if (context != null){
                bindButtons()
                setRecyclerView()

                //Launcher for user profile pic and user cover pic
                imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
                    //Image is selected
                    if (result.resultCode == Activity.RESULT_OK){
                        val data = result.data
                        if (data != null && data.data != null){
                            selectedImageUri = data.data!!
                            //adds the selected image to the dialog layout
                            addImage(postDialogBinding, selectedImageUri)
                        }
                    }
                }
            }

        }

    }

    private fun setRecyclerView() {
        val feedsQuery:Query = FirebaseUtil().retrieveCommunityFeedsCollection(communityId)
            .orderBy("feedTimestamp", Query.Direction.DESCENDING)

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<FeedsModel> =
            FirestoreRecyclerOptions.Builder<FeedsModel>().setQuery(feedsQuery, FeedsModel::class.java).build()

        recyclerView = binding.feedsRV
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedsAdapter = FeedsAdapter(mainBinding, context, options)
        recyclerView.adapter = feedsAdapter
        feedsAdapter.startListening()
    }

    private fun bindButtons(){
        binding.addPostFab.setOnClickListener{
            openCreatePostDialog()
        }
    }

    private fun openCreatePostDialog() {
        //disables notify data set change of the recycler view's adapter in onResume
        isNewPost = true
        postDialogBinding = DialogCreatePostBinding.inflate(layoutInflater)
        val postDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(postDialogBinding.root))
            .setOnDismissListener {
                //enables notify data set change of the recycler view's adapter in onResume
                isNewPost = false
            }
            .create()

        //BIND DIALOG
        postDialogBinding.addImageBtn.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }
        postDialogBinding.backBtn.setOnClickListener {
            //TODO dialog message
            postDialog.dismiss()
        }
        postDialogBinding.postBtn.setOnClickListener {
            if (postDialogBinding.captionEdtTxt.text.toString().isNotEmpty() || ::uriHashMap.isInitialized){
                addPost(){isUploaded ->
                    if (isUploaded)
                        postDialog.dismiss()
                    else
                        Toast.makeText(context, "Error occured while uploading", Toast.LENGTH_SHORT).show()

                }
            }
        }
        AppUtil().setUserProfilePic(context, FirebaseUtil().currentUserUid(), postDialogBinding.profileCIV)
        postDialog.show()
    }
    //Adds Image to the Dialog
    private fun addImage(postDialogBinding: DialogCreatePostBinding, selectedImage:Uri){
        //creates image id
        val userId = FirebaseUtil().currentUserUid()
        val timestamp = Timestamp.now()
        val imageId = "$userId-Image-$timestamp"

        //Creates linear layout for image
        val verticalLayout = LinearLayout(context)
        verticalLayout.orientation = LinearLayout.VERTICAL
        val linearParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        verticalLayout.layoutParams = linearParams

        //creates image view
        val postImage = ImageView(context)
        val imageDpToPx = dpToPx(400F, resources.displayMetrics)
        val imageParams = LinearLayout.LayoutParams(imageDpToPx.toInt(), imageDpToPx.toInt())
        postImage.layoutParams = imageParams

        //inserts selected image to Image View
        Glide.with(this).load(selectedImage)
            .into(postImage)
        verticalLayout.addView(postImage)

        //adds image to uri hashmap
        addUriToHashMap(imageId, selectedImageUri)

        //creates cancel button
        val cancelBtn = ImageButton(context)
        cancelBtn.setImageResource(R.drawable.cancel_icon)
        cancelBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        cancelBtn.setOnClickListener {
            postDialogBinding.mainPostLayout.removeView(verticalLayout)
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
        postDialogBinding.mainPostLayout.addView(verticalLayout)
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
        val tempModel = FeedsModel()
        val contentList:ArrayList<String> = ArrayList()
        val caption = postDialogBinding.captionEdtTxt.text.toString()
        var delay:Long = 1000

        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).add(tempModel).addOnSuccessListener {

            if (::uriHashMap.isInitialized){
                for (data in uriHashMap){
                    //data key is imageId and data value is uri
                    //uploads image to the firebase storage
                    Toast.makeText(context, "SHOOT DITOOOO", Toast.LENGTH_SHORT).show()
                    FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                    contentList.add(data.key)
                    delay += 100
                }
            }

            //get new id from firestore and store it in feedId of the feedsModel
            val feedsModel = FeedsModel(
                feedId = it.id,
                ownerId = FirebaseUtil().currentUserUid(),
                feedCaption = caption,
                feedTimestamp = Timestamp.now(),
                communityIdOfOrigin = communityId,
                contentList = contentList
            )

            //replaces temp model with feeds model
            FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(it.id).set(feedsModel).addOnCompleteListener {task ->
                if (task.isSuccessful) {
                    Handler().postDelayed({
                        Toast.makeText(context, "Your post is uploaded successfully!", Toast.LENGTH_SHORT).show()
                        callback(true)
                    }, delay)
                } else {
                    Toast.makeText(context, "Error has occurred!", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    override fun onStart() {
        super.onStart()
        if (::feedsAdapter.isInitialized){
            feedsAdapter.startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::feedsAdapter.isInitialized){
            feedsAdapter.stopListening()
        }
    }
}
