package com.example.synthronize.adapters

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.R
import com.example.synthronize.ViewFile
import com.example.synthronize.databinding.ItemFileBinding
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NotificationUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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
        private var isLoved = false


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

            //SETUP WRAPPER FOR COMMUNITY
            FirebaseUtil().retrieveCommunityDocument(fileModel.communityId).get().addOnSuccessListener {
                val community = it.toObject(CommunityModel::class.java)!!
                binding.wrapperDivider.visibility = View.VISIBLE
                binding.communityWrapperLayout.visibility = View.VISIBLE
                binding.wrapperName.text = community.communityName
                AppUtil().setCommunityProfilePic(context, community.communityId, binding.wrapperCIV)
                AppUtil().changeCommunityButtonStates(context, binding.communityActionBtn, community.communityId, true)
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

            binding.profileCIV.setOnClickListener {
                headToUserProfile()
            }
            binding.usernameTV.setOnClickListener {
                headToUserProfile()
            }

            binding.mainLayout.setOnClickListener {
                headToViewFile()
            }
            binding.commentBtn.setOnClickListener {
                headToViewFile()
            }

            bindLove()
            bindComment()
        }

        private fun headToViewFile(){
            val intent = Intent(context, ViewFile::class.java)
            intent.putExtra("communityId", fileModel.communityId)
            intent.putExtra("fileId", fileModel.fileId)
            intent.putExtra("contentType", "File")
            context.startActivity(intent)
        }

        private fun bindComment() {

            binding.commentEdtTxt.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged( s: CharSequence?, start: Int, count: Int, after: Int ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val comment = binding.commentEdtTxt.text.toString()
                    if (comment.isNotEmpty()){
                        binding.loveLayout.visibility = View.GONE
                        binding.commentLayout.visibility = View.GONE
                        binding.sendBtn.visibility = View.VISIBLE
                    } else {
                        binding.loveLayout.visibility = View.VISIBLE
                        binding.commentLayout.visibility = View.VISIBLE
                        binding.sendBtn.visibility = View.GONE
                    }
                }
            })

            binding.sendBtn.setOnClickListener {
                val comment = binding.commentEdtTxt.text.toString()
                if (comment.isEmpty()){
                    Toast.makeText(context, "Please type your comment", Toast.LENGTH_SHORT).show()
                } else if(AppUtil().containsBadWord(comment)){
                    Toast.makeText(context, "Your comment contains sensitive words", Toast.LENGTH_SHORT).show()
                } else {
                    val commentModel = CommentModel()
                    FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId).collection("comments").add(commentModel).addOnCompleteListener {
                        if (it.isSuccessful){
                            val commentModel = CommentModel(
                                commentId = it.result.id,
                                commentOwnerId = FirebaseUtil().currentUserUid(),
                                comment = comment,
                                commentTimestamp = Timestamp.now()
                            )
                            FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId).collection("comments")
                                .document(commentModel.commentId).set(commentModel).addOnCompleteListener {task ->
                                    if (task.isSuccessful){
                                        binding.commentEdtTxt.setText("")
                                        updateFeedStatus()
                                        Toast.makeText(context, "Comment sent", Toast.LENGTH_SHORT).show()

                                        //gets comments count before sending the notification
                                        FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId).collection("comments").get().addOnSuccessListener { comments ->
                                            //sends notification
                                            NotificationUtil().sendNotificationToUser(context, fileModel.fileId, fileModel.ownerId, "Comment",
                                                "${comments.size()}","File", fileModel.communityId, DateAndTimeUtil().timestampToString(
                                                    Timestamp.now()))
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }

        //FOR LOVE
        private fun bindLove() {
            //default
            binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)

            updateFeedStatus()

            for (user in fileModel.loveList){
                if (user == FirebaseUtil().currentUserUid()){
                    binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                    isLoved = true
                }
            }

            binding.loveBtn.setOnClickListener {
                if (isLoved){
                    //removes love
                    FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId)
                        .update("loveList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                            isLoved = false
                            updateFeedStatus()
                        }
                } else {
                    //adds love
                    FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId)
                        .update("loveList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                            binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                            isLoved = true
                            updateFeedStatus()
                            //sends notification
                            NotificationUtil().sendNotificationToUser(context, fileModel.fileId, fileModel.ownerId, "Love",
                                "${fileModel.loveList.size + 1}","File", fileModel.communityId, DateAndTimeUtil().timestampToString(
                                    Timestamp.now()))
                        }
                }
            }
        }

        //Updates feed status every user interaction with the feed
        private fun updateFeedStatus(){
            FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId)
                .document(fileModel.fileId).get().addOnSuccessListener {
                    val tempFileModel = it.toObject(fileModel::class.java)!!
                    binding.lovesCountTV.text = tempFileModel.loveList.size.toString()
                }
                .addOnFailureListener {
                    //if Offline
                    binding.lovesCountTV.text = fileModel.loveList.size.toString()
                    FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId)
                        .collection("comments").get().addOnSuccessListener {
                            Toast.makeText(context, "${it.size()}", Toast.LENGTH_SHORT).show()
                            binding.commentsCountTV.text = it.size().toString()
                        }.addOnFailureListener {
                            binding.commentsCountTV.text = "0"
                        }
                }
            FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId).document(fileModel.fileId)
                .collection("comments").get().addOnSuccessListener {
                    binding.commentsCountTV.text = it.documents.size.toString()
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