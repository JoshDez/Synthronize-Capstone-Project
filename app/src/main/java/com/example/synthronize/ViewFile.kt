package com.example.synthronize

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CommentAdapter
import com.example.synthronize.databinding.ActivityViewFileBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommentModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import java.io.File

class ViewFile : AppCompatActivity(), OnNetworkRetryListener, OnRefreshListener {
    private lateinit var binding: ActivityViewFileBinding
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var fileModel: FileModel
    private lateinit var communityId:String
    private lateinit var fileId:String
    private lateinit var contentType:String
    private var isLoved:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        contentType = intent.getStringExtra("contentType").toString()
        fileId = intent.getStringExtra("fileId").toString()

        binding.viewFileRefreshLayout.setOnRefreshListener(this)
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        getFileModel()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getFileModel(){
        binding.viewFileRefreshLayout.isRefreshing = true
        FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId).get().addOnSuccessListener {
            fileModel = it.toObject(FileModel::class.java)!!

            ContentUtil().verifyCommunityContentAvailability(fileModel.ownerId, fileModel.communityId) { isAvailable ->
                if (isAvailable){
                    binding.feedTimestampTV.text = DateAndTimeUtil().getTimeAgo(fileModel.createdTimestamp)
                    binding.captionEdtTxt.setText(fileModel.caption)
                    binding.fileNameTV.text = fileModel.fileName
                    displayFileIcon()

                    FirebaseUtil().targetUserDetails(fileModel.ownerId).get().addOnSuccessListener { result ->
                        val user = result.toObject(UserModel::class.java)!!
                        binding.ownerUsernameTV.text = user.username
                        AppUtil().setUserProfilePic(this, user.userID, binding.profileCIV)
                    }

                    binding.kebabMenuBtn.setOnClickListener {
                        DialogUtil().openMenuDialog(this, layoutInflater, contentType,
                            fileModel.fileId, fileModel.ownerId, fileModel.communityId){closeCurrentActivity ->
                            if (closeCurrentActivity){
                                Handler().postDelayed({
                                    onBackPressed()
                                }, 2000)
                            }
                        }
                    }

                    binding.ownerUsernameTV.setOnClickListener {
                        headToUserProfile()
                    }

                    binding.ownerUsernameTV.setOnClickListener {
                        headToUserProfile()
                    }

                    binding.fileLayout.setOnClickListener {
                        downloadFileFromFirebase()
                    }

                    if (contentType == "File Submission"){
                        binding.bottomToolbar.visibility = View.INVISIBLE
                        binding.commentsRV.visibility = View.GONE
                        binding.loveLayout.visibility = View.GONE
                        binding.commentsTV.visibility = View.GONE
                        binding.divider2.visibility = View.GONE
                        binding.divider3.visibility = View.GONE
                        binding.divider4.visibility = View.GONE

                    } else {
                        bindLove()
                        bindComments()
                    }
                    binding.viewFileRefreshLayout.isRefreshing = false
                } else {
                    hideContent()
                }
                binding.viewFileRefreshLayout.isRefreshing = false
            }
        }
    }


    private fun headToUserProfile() {
        if (fileModel.ownerId != FirebaseUtil().currentUserUid()){
            val intent = Intent(this, OtherUserProfile::class.java)
            intent.putExtra("userID", fileModel.ownerId)
            startActivity(intent)
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
            val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
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
    private fun displayFileIcon(){
        val extension = fileModel.fileName.split('.').last()

        if (extension == "pdf"){
            binding.fileIV.setImageResource(R.drawable.pdf_icon)
        } else if (extension == "docx"){
            binding.fileIV.setImageResource(R.drawable.docx_icon)
        } else if (extension == "excel"){
            binding.fileIV.setImageResource(R.drawable.excel_icon)
        }
    }

    private fun bindComments(){
        val commentsReference = FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileId).collection("comments")

        val query: Query = commentsReference.orderBy("commentTimestamp", Query.Direction.ASCENDING)

        val options: FirestoreRecyclerOptions<CommentModel> =
            FirestoreRecyclerOptions.Builder<CommentModel>().setQuery(query, CommentModel::class.java).build()

        binding.commentsRV.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(this, options, commentsReference)
        binding.commentsRV.adapter = commentAdapter
        commentAdapter.startListening()

        binding.sendBtn.setOnClickListener {
            val comment = binding.commentEdtTxt.text.toString()

            if (comment.isEmpty()){
                Toast.makeText(this, "Please type your comment", Toast.LENGTH_SHORT).show()
            } else if(AppUtil().containsBadWord(comment)){
                Toast.makeText(this, "Your comment contains sensitive words", Toast.LENGTH_SHORT).show()
            } else {
                commentAdapter.stopListening()
                val commentModel = CommentModel()
                commentsReference.add(commentModel).addOnCompleteListener {
                    if (it.isSuccessful){
                        val commentModel = CommentModel(
                            commentId = it.result.id,
                            commentOwnerId = FirebaseUtil().currentUserUid(),
                            comment = comment,
                            commentTimestamp = Timestamp.now()
                        )
                        commentsReference.document(commentModel.commentId).set(commentModel).addOnCompleteListener {task ->
                            if (task.isSuccessful){
                                binding.commentEdtTxt.setText("")
                                bindComments()
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

        for (user in fileModel.loveList) {
            if (user == FirebaseUtil().currentUserUid()) {
                binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                isLoved = true
            }
        }
        binding.loveBtn.setOnClickListener {
            if (isLoved) {
                //removes love
                FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId)
                    .document(fileModel.fileId)
                    .update("loveList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        binding.loveBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                        isLoved = false
                        updateFeedStatus()
                    }
            } else {
                //adds love
                FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId)
                    .document(fileModel.fileId)
                    .update("loveList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        binding.loveBtn.setImageResource(R.drawable.baseline_favorite_24)
                        isLoved = true
                        updateFeedStatus()
                    }
            }
        }
    }


    //Updates feed status every user interaction with the feed
    private fun updateFeedStatus(){
        FirebaseUtil().retrieveCommunityFilesCollection(fileModel.communityId)
            .document(fileModel.fileId).get().addOnSuccessListener {
                val tempFileModel = it.toObject(FileModel::class.java)!!
                binding.lovesCountTV.text = tempFileModel.loveList.size.toString()
            }
            .addOnFailureListener {
                //if Offline
                binding.lovesCountTV.text = fileModel.loveList.size.toString()
            }
    }

    private fun hideContent(){
        binding.scrollViewLayout.visibility = View.GONE
        binding.bottomToolbar.visibility = View.INVISIBLE
        binding.divider2.visibility = View.INVISIBLE
        binding.contentNotAvailableLayout.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (::commentAdapter.isInitialized)
            commentAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (::commentAdapter.isInitialized)
            commentAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::commentAdapter.isInitialized)
            commentAdapter.stopListening()
    }

    override fun onRefresh() {
        Handler().postDelayed({
            getFileModel()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}