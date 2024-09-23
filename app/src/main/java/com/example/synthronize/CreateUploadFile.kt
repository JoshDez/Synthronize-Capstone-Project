package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityCreateUploadFileBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder


class CreateUploadFile : AppCompatActivity() {
    private lateinit var binding:ActivityCreateUploadFileBinding
    private lateinit var selectedFileUri: Uri
    private lateinit var existingFileModel: FileModel
    private val PICK_FILE_REQUEST = 1
    private var communityId = ""
    private var competitionId = ""
    private var fileId = ""
    private var fileUrl = ""
    private var isSharedFiles = false
    private var forCompetition = false
    private var restrictedFileTypes = listOf("jpeg", "jpg", "png", "gif", "bmp", "webp", "tiff", "svg", "ico", "heic", "mp4",
        "mov", "avi", "mkv", "wmv", "flv", "webm", "m4v", "3gp", "mpeg", "mpg")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateUploadFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        fileId = intent.getStringExtra("fileId").toString()
        competitionId = intent.getStringExtra("competitionId").toString()
        isSharedFiles = intent.getBooleanExtra("isSharedFiles", false)
        forCompetition = intent.getBooleanExtra("forCompetition", false)


        if (fileId == "null" || fileId.isEmpty()){
            //For New Product
            AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.profileCIV)
            bindButtons()
        } else {
            //For Existing Product to edit
            FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId).get().addOnSuccessListener {
                existingFileModel = it.toObject(FileModel::class.java)!!
                AppUtil().setUserProfilePic(this, existingFileModel.ownerId, binding.profileCIV)
                binding.captionEdtTxt.setText(existingFileModel.caption)
                communityId = existingFileModel.communityId
                fileUrl = existingFileModel.fileUrl
                isSharedFiles = existingFileModel.shareFile
                forCompetition = existingFileModel.forCompetition
                binding.toolbarTitleTV.text = "Edit File"
                binding.uploadBtn.text = "Save"

                FirebaseUtil().retrieveCommunityFileRef(existingFileModel.fileUrl).downloadUrl.addOnSuccessListener { fileUri ->
                    displayFile(existingFileModel.fileName, fileUri)
                    bindButtons()
                }
            }
        }

    }

    private fun bindButtons(){

        AppUtil().setUserProfilePic(this, FirebaseUtil().currentUserUid(), binding.profileCIV)

        binding.addFileBtn.setOnClickListener {
            openFileChooser()
        }
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.uploadBtn.setOnClickListener {
            uploadFile()
        }
    }


    private fun uploadFile() {
        val caption = binding.captionEdtTxt.text.toString()
        val fileName = binding.fileNameTV.text.toString()

        if (fileName.isEmpty()){
            Toast.makeText(this, "Please attach your file", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(fileName)){
            Toast.makeText(this, "Your file name contains sensitive words", Toast.LENGTH_SHORT).show()
        } else if (AppUtil().containsBadWord(caption)){
            Toast.makeText(this, "Your caption contains sensitive words", Toast.LENGTH_SHORT).show()
        } else {
            if (::selectedFileUri.isInitialized){
                if (selectedFileUri != Uri.EMPTY){

                    if (fileId == "null" || fileId.isEmpty()){
                        //New File
                        fileUrl = "$fileName-${Timestamp.now()}"
                        val fileModel = FileModel(
                            fileName = fileName,
                            fileUrl = fileUrl,
                            ownerId = FirebaseUtil().currentUserUid(),
                            shareFile = isSharedFiles,
                            forCompetition = forCompetition,
                            caption = caption,
                            communityId = communityId,
                            createdTimestamp = Timestamp.now(),
                        )
                        uploadToFirebase(fileModel = fileModel, hasNewFile = true)
                    } else {
                        //Edited File
                        if (fileUrl.isEmpty()){
                            //new file added
                            fileUrl = "$fileName-${Timestamp.now()}"
                            var fileModel = FileModel(
                                fileId = fileId,
                                fileName = fileName,
                                fileUrl = fileUrl,
                                ownerId = FirebaseUtil().currentUserUid(),
                                shareFile = isSharedFiles,
                                forCompetition = forCompetition,
                                loveList = existingFileModel.loveList,
                                caption = caption,
                                communityId = communityId,
                                createdTimestamp = Timestamp.now(),
                            )
                            uploadToFirebase(fileId, fileModel, hasNewFile = true)

                        } else {
                            var fileModel = FileModel(
                                fileId = fileId,
                                fileName = fileName,
                                fileUrl = fileUrl,
                                ownerId = FirebaseUtil().currentUserUid(),
                                shareFile = isSharedFiles,
                                forCompetition = forCompetition,
                                loveList = existingFileModel.loveList,
                                caption = caption,
                                communityId = communityId,
                                createdTimestamp = Timestamp.now(),
                            )
                            //updates fileModel
                            uploadToFirebase(fileId, fileModel)
                        }

                    }
                } else {
                    Toast.makeText(this, "Please attach your file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun uploadToFirebase(fileId:String = "", fileModel: FileModel = FileModel(), hasNewFile:Boolean = false){
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        if (this.fileId.isNotEmpty() && this.fileId != "null"){
            dialogLoadingBinding.messageTV.text = "Saving..."
        } else {
            dialogLoadingBinding.messageTV.text = "Uploading..."
        }

        loadingDialog.show()

        if (hasNewFile && fileId.isEmpty()){
            //New File
            FirebaseUtil().retrieveCommunityFileRef(fileUrl).putFile(selectedFileUri).addOnSuccessListener {
                FirebaseUtil().retrieveCommunityFilesCollection(communityId).add(fileModel).addOnSuccessListener {file ->
                    var newFileModel = FileModel(
                        fileId = file.id,
                        fileName = fileModel.fileName,
                        fileUrl = fileModel.fileUrl,
                        ownerId = fileModel.ownerId,
                        shareFile = fileModel.shareFile,
                        forCompetition = fileModel.forCompetition,
                        caption = fileModel.caption,
                        communityId = fileModel.communityId,
                        createdTimestamp = fileModel.createdTimestamp,
                    )

                    FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(newFileModel.fileId).set(newFileModel).addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (forCompetition)
                                addFileUrlToSubmission(fileUrl)
                            Toast.makeText(this, "Your file is successfully uploaded", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismiss()
                            this.finish()
                        } else {
                            Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismiss()
                            this.finish()
                        }
                    }

                }.addOnFailureListener {
                    Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener{
                Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
            }

        } else if (hasNewFile) {
            //Has existing fileModel
            FirebaseUtil().retrieveCommunityFileRef(fileUrl).putFile(selectedFileUri).addOnSuccessListener {
                //deletes the old file
                FirebaseUtil().retrieveCommunityFileRef(existingFileModel.fileUrl).delete()
                //updates fileModel
                FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId)
                    .set(fileModel).addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (forCompetition)
                                addFileUrlToSubmission(fileUrl)
                            Toast.makeText(this, "Your file is successfully updated", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismiss()
                            this.finish()
                        } else {
                            Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismiss()
                            this.finish()
                        }
                    }
            }.addOnFailureListener{
                Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
            }

        } else {
            //edits only the caption of the file
            FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId)
                .set(fileModel).addOnCompleteListener {
                if (it.isSuccessful) {
                    if (forCompetition)
                        addFileUrlToSubmission(fileUrl)
                    Toast.makeText(this, "Your file is successfully updated", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismiss()
                    this.finish()
                } else {
                    Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismiss()
                    this.finish()

                }
            }
        }
    }

    private fun addFileUrlToSubmission(fileUrl:String) {
        val updates = hashMapOf<String, Any>(
            "contestants.${FirebaseUtil().currentUserUid()}" to fileUrl
        )
        FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(competitionId).update(updates)
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        val mimeTypes = arrayOf(
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/pdf", // .pdf
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "text/plain", // .txt
            "application/rtf" // .rtf
        )
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val fileUri = data.data
            if (fileUri != null) {
                val fileName = getFileName(this, fileUri)
                if (fileName != null){
                    val extension = fileName.split('.').last()
                    if (!restrictedFileTypes.contains(extension)){
                        displayFile(fileName, fileUri)
                    } else {
                        Toast.makeText(this, "The file type is not accepted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun displayFile(fileName:String, fileUri: Uri) {
        binding.bottomToolbar.visibility = View.INVISIBLE
        binding.divider2.visibility = View.INVISIBLE
        binding.fileLayout.visibility = View.VISIBLE
        binding.fileNameTV.text = fileName
        selectedFileUri = fileUri

        displayFileIcon(fileName)

        binding.removeFileBtn.setOnClickListener{
            binding.divider2.visibility = View.VISIBLE
            binding.bottomToolbar.visibility = View.VISIBLE
            binding.fileLayout.visibility = View.GONE
            binding.fileNameTV.text = ""
            fileUrl = ""
            selectedFileUri = Uri.EMPTY
        }
    }


    private fun displayFileIcon(fileName: String){
        val extension = fileName.split('.').last()
        binding.fileIV.setImageResource(R.drawable.baseline_attach_file_24)

        if (extension == "pdf"){
            binding.fileIV.setImageResource(R.drawable.pdf_icon)
        } else if (extension == "docx"){
            binding.fileIV.setImageResource(R.drawable.docx_icon)
        } else if (extension == "excel"){
            binding.fileIV.setImageResource(R.drawable.excel_icon)
        }
    }


    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            try {
                cursor?.let {
                    if (it.moveToFirst()) {
                        result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
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
                ::selectedFileUri.isInitialized
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.backBtn.windowToken, 0)
    }
}