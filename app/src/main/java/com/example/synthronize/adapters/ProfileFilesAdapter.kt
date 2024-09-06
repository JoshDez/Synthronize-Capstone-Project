package com.example.synthronize.adapters

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.R
import com.example.synthronize.databinding.ItemFileBinding
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import java.io.File

class ProfileFilesAdapter(private val context: Context, private val filesList: ArrayList<FileModel>)
    : RecyclerView.Adapter<ProfileFilesAdapter.FileViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileFilesAdapter.FileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val fileBinding = ItemFileBinding.inflate(inflater, parent, false)
        return FileViewHolder(fileBinding, context, inflater)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.checkAvailabilityBeforeBind(filesList[position])
    }

    override fun getItemCount(): Int {
        return filesList.size
    }
    class FileViewHolder(private val binding: ItemFileBinding, private val context: Context,
                         private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var fileModel: FileModel


        fun checkAvailabilityBeforeBind(model: FileModel){
            ContentUtil().verifyCommunityContentAvailability(model.ownerId, model.communityId){ isAvailable ->
                if(isAvailable){
                    bindFile(model)
                } else {
                    bindContentNotAvailable()
                }
            }
        }
        private fun bindContentNotAvailable(){
            binding.fileLayout.visibility = View.GONE
            binding.captionTV.text = "File Not Available"
        }
        private fun bindFile(model: FileModel){
            fileModel = model

            if (fileModel.caption.isEmpty()){
                binding.captionTV.visibility = View.GONE
            }

            FirebaseUtil().targetUserDetails(fileModel.ownerId).get().addOnSuccessListener {
                val user = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, fileModel.ownerId, binding.profileCIV)
                binding.usernameTV.text = user.username
            }

            binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(fileModel.createdTimestamp)
            binding.captionTV.text = fileModel.caption
            binding.fileNameTV.text = fileModel.fileName
            displayFileIcon()

            binding.fileLayout.setOnClickListener {
                downloadFileFromFirebase()
            }

            binding.menuBtn.setOnClickListener {
                DialogUtil().openMenuDialog(context, inflater, "File", fileModel.fileId,
                    fileModel.ownerId, fileModel.communityId){}
            }
        }

        private fun downloadFileFromFirebase() {
            // Get the file's download URL
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