package com.example.synthronize.utils

import android.content.Context
import android.widget.Toast
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.FileModel
import com.google.firebase.storage.FirebaseStorage
import com.orhanobut.dialogplus.DialogPlus

class DeleteCommunity {

    private lateinit var context: Context
    private lateinit var communityId: String
    private lateinit var dialog: DialogPlus
    private val storage = FirebaseStorage.getInstance()
    private var totalDeletes = 0
    private var failedDeletes = 0

    fun startCommunityDeleteProcess(context: Context, communityId:String, dialog: DialogPlus){
        this.context = context
        this.communityId = communityId
        this.dialog = dialog
        deleteAllCommunityMedia()
    }
    private fun deleteAllCommunityMedia() {
        val folders = listOf(
            "communityContentImage",
            "communityContentVideo",
            "communityBannerPicture",
            "communityProfilePicture"
        )

        totalDeletes = 0
        failedDeletes = 0
        val totalFolders = folders.size

        // Iterate over each folder and delete files containing the communityId
        folders.forEach { folder ->
            val folderRef = storage.reference.child(folder)
            val prefix = "$communityId-"

            folderRef.listAll().addOnSuccessListener { listResult ->
                val itemsToDelete = listResult.items.filter { it.name.startsWith(prefix) }
                val itemCount = itemsToDelete.size

                // Check if there are no items to delete
                if (itemCount == 0) {
                    onFolderDeletionComplete(++totalDeletes, failedDeletes, totalFolders)
                    return@addOnSuccessListener
                }

                // Attempt to delete each item
                itemsToDelete.forEach { item ->
                    item.delete()
                        .addOnSuccessListener { onFileDeletionSuccess(++totalDeletes, itemCount, folder) }
                        .addOnFailureListener { onFileDeletionFailure(++failedDeletes, totalDeletes, itemCount, folder) }
                }
            }.addOnFailureListener {
                // Handle failure to list items in folder
                showFailedMessage("Failed to list items in $folder.")
            }
        }
    }

    private fun onFileDeletionSuccess(totalDeletes: Int, itemCount: Int, folder: String) {
        if (totalDeletes == itemCount) {
            dialog.dismiss()
            checkOverallDeletion()
        }
    }

    private fun onFileDeletionFailure(failedDeletes: Int, totalDeletes: Int, itemCount: Int, folder: String) {
        if (failedDeletes + totalDeletes == itemCount) {
            dialog.dismiss()
            checkOverallDeletion()
        }
    }

    private fun onFolderDeletionComplete(totalDeletes: Int, failedDeletes: Int, totalFolders: Int) {
        if (totalDeletes + failedDeletes == totalFolders) {
            checkOverallDeletion()
        }
    }

    private fun checkOverallDeletion() {
        if (failedDeletes == 0) {
            deleteFiles()
        } else {
            showFailedMessage("Some community media files couldn't be deleted.")
        }
    }

    private fun showFailedMessage(message: String = "Community deletion failed, please try again.") {
        dialog.dismiss()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    //6th Process
    private fun deleteFiles(){
        FirebaseUtil().retrieveCommunityFilesCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                if (it.result.documents.isNotEmpty()){
                    var fileLists:ArrayList<String> = arrayListOf()

                    for (file in it.result.documents){
                        val fileModel = file.toObject(FileModel::class.java)!!
                        if (fileModel.fileUrl.isNotEmpty())
                            fileLists.add(fileModel.fileUrl)
                    }
                    for (file in fileLists){
                        FirebaseUtil().retrieveCommunityFileRef(file).delete()
                    }
                    deleteTextChannels()
                } else {
                    deleteTextChannels()
                }
            } else {
                showFailedMessage()
            }
        }
    }

    //7th Process
    private fun deleteTextChannels(){
        FirebaseUtil().retrieveAllChatRoomReferences().whereEqualTo("communityId", communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){
                    for (channel in it.result.documents){
                        val chatModel = channel.toObject(ChatroomModel::class.java)!!
                        FirebaseUtil().retrieveChatRoomReference(chatModel.chatroomId).delete().addOnCompleteListener {
                            if (ctr == size)
                                deleteCommunity()
                            ctr += 1
                        }
                    }
                } else {
                    deleteCommunity()
                }

            } else {
                showFailedMessage()
            }
        }
    }
    //Final Process
    private fun deleteCommunity() {
        // Delete community document
        FirebaseUtil().retrieveCommunityDocument(communityId).delete()
            .addOnSuccessListener {
                dialog.dismiss()
                Toast.makeText(context, "The community has been deleted", Toast.LENGTH_SHORT).show()
                AppUtil().headToMainActivity(context)
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(context, "Community deletion failed. Please try again", Toast.LENGTH_SHORT).show()
            }
    }
}