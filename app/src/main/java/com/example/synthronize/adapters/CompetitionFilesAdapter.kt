package com.example.synthronize.adapters

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.ViewFile
import com.example.synthronize.databinding.ItemCompetitionFileBinding
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.io.File

class CompetitionFilesAdapter(private val context: Context, options: FirestoreRecyclerOptions<FileModel>,
    private val competitionId:String):
    FirestoreRecyclerAdapter<FileModel, CompetitionFilesAdapter.FileViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCompetitionFileBinding.inflate(inflater, parent, false)
        return FileViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int, model: FileModel) {
        holder.bind(model)
    }


    inner class FileViewHolder(private val binding: ItemCompetitionFileBinding, private val context: Context,
                         private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var fileModel: FileModel

        fun bind(model: FileModel){
            fileModel = model

            if (fileModel.caption.isEmpty()){
                binding.captionTV.visibility = View.GONE
            }

            FirebaseUtil().targetUserDetails(fileModel.ownerId).get().addOnSuccessListener {
                val user = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, fileModel.ownerId, binding.profileCIV)
                binding.usernameTV.text = user.username

                binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(fileModel.createdTimestamp)
                binding.captionTV.text = fileModel.caption
                binding.fileNameTV.text = fileModel.fileName
                displayFileIcon()

                binding.fileLayout.setOnClickListener {
                    downloadFileFromFirebase()
                }

                binding.mainLayout.setOnClickListener {
                    headToViewFile()
                }

                binding.profileCIV.setOnClickListener {
                    headToUserProfile()
                }

                binding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }

                binding.menuBtn.setOnClickListener {
                    DialogUtil().openMenuDialog(context, inflater, "File Submission", fileModel.fileId,
                        fileModel.ownerId, fileModel.communityId, competitionId){}
                }
            }
        }


        private fun headToUserProfile() {
            if (fileModel.ownerId != FirebaseUtil().currentUserUid()){
                val intent = Intent(context, OtherUserProfile::class.java)
                intent.putExtra("userID", fileModel.ownerId)
                context.startActivity(intent)
            }
        }

        private fun downloadFileFromFirebase() {
            // Get the file's download URL
            Toast.makeText(context, "Downloading File...", Toast.LENGTH_SHORT).show()
            FirebaseUtil().retrieveCommunityFileRef(fileModel.fileUrl).downloadUrl.addOnSuccessListener { uri ->

                // Get the Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                // Check for existing files and modify the name if necessary
                val finalFileName = getUniqueFileName(downloadsDir, fileModel.fileName)

                // Use DownloadManager to handle the download
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
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
                exception.printStackTrace()
            }
        }

        private fun displayFileIcon(){
            val extension = fileModel.fileName.split('.').last()

            if (extension == "pdf"){
                binding.fileIconIV.setImageResource(R.drawable.pdf_icon)
            } else if (extension == "docx"){
                binding.fileIconIV.setImageResource(R.drawable.docx_icon)
            } else if (extension == "excel"){
                binding.fileIconIV.setImageResource(R.drawable.excel_icon)
            }
        }


        private fun headToViewFile(){
            val intent = Intent(context, ViewFile::class.java)
            intent.putExtra("communityId", fileModel.communityId)
            intent.putExtra("fileId", fileModel.fileId)
            intent.putExtra("contentType", "File Submission")
            context.startActivity(intent)
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
    }

}