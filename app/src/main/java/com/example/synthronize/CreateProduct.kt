package com.example.synthronize

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.TypedValueCompat
import com.bumptech.glide.Glide
import com.example.synthronize.databinding.ActivityCreateProductBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.util.UUID

class CreateProduct : AppCompatActivity() {

    private lateinit var binding:ActivityCreateProductBinding
    private lateinit var communityId:String
    private lateinit var productId:String
    private lateinit var existingProductModel:ProductModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var uriHashMap: HashMap<String, Uri>
    private lateinit var selectedImageUri: Uri
    private var imageList:ArrayList<String> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        productId = intent.getStringExtra("productId").toString()


        if (productId == "null" || productId.isEmpty()){
            //For New Product
            bindButtons()
        } else {
            //For Existing Product to edit
            FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(productId).get().addOnSuccessListener {
                existingProductModel = it.toObject(ProductModel::class.java)!!
                binding.productNameEdtTxt.setText(existingProductModel.productName)
                binding.competitionDescEdtTxt.setText(existingProductModel.productDesc)
                binding.productPriceEdtTxt.setText(existingProductModel.price.toString())
                communityId = existingProductModel.communityId
                if (existingProductModel.imageList.isNotEmpty()){
                    for (filename in existingProductModel.imageList){
                        imageList = ArrayList(existingProductModel.imageList)
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


        if (productId == "null" || productId.isEmpty()){
            binding.uploadBtn.text = "Upload"
        } else {
            binding.uploadBtn.text = "Save"
        }

        binding.uploadBtn.setOnClickListener {
            showLoadingDialog()
        }

        binding.addImageBtn.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }
        binding.backBtn.setOnClickListener {
            this.finish()
        }
    }

    private fun showLoadingDialog(){
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        if (productId.isNotEmpty() && productId != "null"){
            dialogLoadingBinding.messageTV.text = "Saving..."
        } else {
            dialogLoadingBinding.messageTV.text = "Uploading..."
        }

        uploadProduct{isUploaded ->
            if (isUploaded){
                Handler().postDelayed({
                    loadingDialog.dismiss()
                    this.finish()
                }, 2000)
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, "Error occurred while uploading", Toast.LENGTH_SHORT).show()
            }
        }
        loadingDialog.show()
    }
    private fun getFileUriFromFirebase(filename: String) {
        // Create a storage reference from the Firebase Storage instance
        FirebaseUtil().retrieveCommunityContentImageRef(filename).downloadUrl.addOnSuccessListener {
            selectedImageUri = it
            addImage(selectedImageUri, filename)
        }.addOnFailureListener {
            Toast.makeText(this, "An error has occurred while downloading, please try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addImage(selectedImage: Uri, existingFilename: String = ""){
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
            binding.mainProductLayout.removeView(verticalLayout)
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
        binding.mainProductLayout.addView(verticalLayout)
    }

    private fun removeUriFromHashMap(contentId:String){
        uriHashMap.remove(contentId)
        if (imageList.isNotEmpty()){
            imageList.remove(contentId)
        }
    }

    private fun addUriToHashMap(contentId:String, uri: Uri){
        if (!::uriHashMap.isInitialized)
            uriHashMap = HashMap()
        uriHashMap[contentId] = uri
    }

    private fun uploadProduct(callback: (Boolean) -> Unit){
        val tempModel = ProductModel()
        val productName = binding.productNameEdtTxt.text.toString()
        val productDesc = binding.competitionDescEdtTxt.text.toString()
        var delay:Long = 1000
        var price:Long = 0

        if (binding.productPriceEdtTxt.text.toString().isNotEmpty()){
            price += binding.productPriceEdtTxt.text.toString().toLong()
        }

        //Validation
        if (productName.isEmpty()){
            Toast.makeText(this, "Please add product name", Toast.LENGTH_SHORT).show()
            callback(false)
        } else if (AppUtil().containsBadWord(productName)){
            Toast.makeText(this, "Product name contains sensitive words", Toast.LENGTH_SHORT).show()
            callback(false)
        } else if (productDesc.isEmpty()){
            Toast.makeText(this, "Please add product description", Toast.LENGTH_SHORT).show()
            callback(false)
        } else if (AppUtil().containsBadWord(productDesc)){
            Toast.makeText(this, "Product description contains sensitive words", Toast.LENGTH_SHORT).show()
            callback(false)
        } else if (!hasProductImages()){
            Toast.makeText(this, "Please add images of your product", Toast.LENGTH_SHORT).show()
            callback(false)
        } else {
            //Uploads the produce
            if (productId == "null" || productId.isEmpty()){
                //Upload product
                FirebaseUtil().retrieveCommunityMarketCollection(communityId).add(tempModel).addOnSuccessListener {
                    if (::uriHashMap.isInitialized){
                        for (data in uriHashMap){
                            //data key is imageFileName and value is uri or actual image
                            //uploads image to the firebase storage
                            FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                            imageList.add(data.key)
                            delay += 100
                        }
                    }

                    //get new id from firestore and store it in feedId of the PostModel
                    val productModel = ProductModel(
                        productId = it.id,
                        productName = productName,
                        productDesc = productDesc,
                        communityId = communityId,
                        price = price,
                        imageList = imageList,
                        ownerId = FirebaseUtil().currentUserUid(),
                        createdTimestamp = Timestamp.now()
                    )

                    //replaces temp model with feeds model
                    FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(it.id).set(productModel).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Handler().postDelayed({
                                Toast.makeText(this, "Your product is uploaded successfully!", Toast.LENGTH_SHORT).show()
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
                //Saving edited post
                if (::uriHashMap.isInitialized){
                    for (data in uriHashMap){
                        //data key is imageFileName and value is uri or actual image
                        //uploads image to the firebase storage
                        val key = data.key.split('-')
                        if (key[1] == "Image") {
                            FirebaseUtil().retrieveCommunityContentImageRef(data.key).putFile(data.value)
                            if (!imageList.contains(data.key)){
                                imageList.add(data.key)
                                delay += 100
                            }
                        }
                    }
                }

                //Edit post
                val productModel = ProductModel(
                    productId = productId,
                    productName = productName,
                    productDesc = productDesc,
                    communityId = communityId,
                    price = price,
                    imageList = imageList,
                    ownerId = FirebaseUtil().currentUserUid(),
                    createdTimestamp = existingProductModel.createdTimestamp
                )

                FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(productId).set(productModel).addOnSuccessListener {
                    deleteFilesFromFirebaseStorage()
                    Handler().postDelayed({
                        Toast.makeText(this, "Your post is saved successfully!", Toast.LENGTH_SHORT).show()
                        callback(true)
                    }, delay)
                }.addOnFailureListener{
                    callback(false)
                }
            }
        }
    }
    private fun deleteFilesFromFirebaseStorage(){
        //deletes files that are no longer included in content list while editing
        for (filename in existingProductModel.imageList){
            if (!imageList.contains(filename)){
                FirebaseUtil().retrieveCommunityContentImageRef(filename).delete()
            }
        }
    }

    private fun hasProductImages():Boolean {
        if (::uriHashMap.isInitialized){
            if (uriHashMap.size > 0){
                return true
            }
        }
        return false
    }
}