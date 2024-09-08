package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityCreateUploadFileBinding
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Firebase
import com.google.firebase.Timestamp


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
        } else {
            if (::selectedFileUri.isInitialized){
                if (selectedFileUri != Uri.EMPTY){

                    if (fileId == "null" || fileId.isEmpty()){
                        //New File
                        fileUrl = "$fileName-${Timestamp.now()}"
                        FirebaseUtil().retrieveCommunityFileRef(fileUrl).putFile(selectedFileUri).addOnSuccessListener {
                            var fileModel = FileModel()
                            FirebaseUtil().retrieveCommunityFilesCollection(communityId).add(fileModel).addOnSuccessListener {file ->
                                fileModel = FileModel(
                                    fileId = file.id,
                                    fileName = fileName,
                                    fileUrl = fileUrl,
                                    ownerId = FirebaseUtil().currentUserUid(),
                                    shareFile = isSharedFiles,
                                    forCompetition = forCompetition,
                                    caption = caption,
                                    communityId = communityId,
                                    createdTimestamp = Timestamp.now(),
                                )
                                FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(file.id).set(fileModel).addOnSuccessListener {
                                    Toast.makeText(this, "your file is successfully uploaded", Toast.LENGTH_SHORT).show()
                                    if (forCompetition)
                                        addFileUrlToSubmission(fileUrl)
                                    this.finish()
                                }.addOnFailureListener {
                                    Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener{
                            Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        //Edited File
                        if (fileUrl.isEmpty()){
                            //new file added
                            fileUrl = "$fileName-${Timestamp.now()}"
                            FirebaseUtil().retrieveCommunityFileRef(fileUrl).putFile(selectedFileUri).addOnSuccessListener {
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

                                //deletes the old file
                                FirebaseUtil().retrieveCommunityFileRef(existingFileModel.fileUrl).delete()

                                //updates fileModel
                                FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId).set(fileModel).addOnSuccessListener {
                                    Toast.makeText(this, "your file is successfully updated", Toast.LENGTH_SHORT).show()
                                    if (forCompetition)
                                        addFileUrlToSubmission(fileUrl)
                                    this.finish()
                                }.addOnFailureListener {
                                    Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener{
                                Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                            }
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
                            FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId).set(fileModel).addOnSuccessListener {
                                Toast.makeText(this, "your file is successfully updated", Toast.LENGTH_SHORT).show()
                                this.finish()
                            }.addOnFailureListener {
                                Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
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
                    displayFile(fileName, fileUri)
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
}