package com.example.synthronize.utils

import android.content.Context
import android.widget.Toast
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.ForumModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.ProductModel
import com.orhanobut.dialogplus.DialogPlus

class DeleteCommunity {

    private lateinit var context: Context
    private lateinit var communityId: String
    private lateinit var dialog: DialogPlus
    private var mediaList:ArrayList<String> = arrayListOf()

    fun startCommunityDeleteProcess(context: Context, communityId:String, dialog: DialogPlus){
        this.context = context
        this.communityId = communityId
        this.dialog = dialog
        deleteFeedsMedia()
    }

    private fun showFailedMessage(){
        dialog.dismiss()
        Toast.makeText(context, "Community deletion failed, please try again.", Toast.LENGTH_SHORT).show()
    }

    private fun addListToMediaList(mediaList:List<String>){
        for (media in mediaList){
            this.mediaList.add(media)
        }
    }


    private fun deleteMediaOrFile(isDone: (Boolean) -> Unit){
        var ctr = 1
        var size = mediaList.size

        for (media in mediaList){
            val mediaType = media.split('-')[1]
            if (mediaType == "Image" || mediaType == "ImageInstruction"){
                //deletes image from firebase
                FirebaseUtil().retrieveCommunityContentImageRef(media).delete()
            } else if (mediaType == "Video"){
                //deletes video from firebase
                FirebaseUtil().retrieveCommunityContentVideoRef(media).delete()
            } else {
                //deletes file from firebase
                FirebaseUtil().retrieveCommunityFileRef(media).delete()
            }


            if (ctr == size)
                isDone(true)

            ctr += 1
        }
    }

    //1st Process
    private fun deleteFeedsMedia(){
        Toast.makeText(context, "deleteFeedsMedia", Toast.LENGTH_SHORT).show()
        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){

                    for (post in it.result.documents){
                        val postModel = post.toObject(PostModel::class.java)!!
                        if (postModel.contentList.isNotEmpty()){
                            addListToMediaList(postModel.contentList)
                            if (ctr == size)
                                deleteEventsMedia()
                        } else if (ctr == size)
                            deleteEventsMedia()

                        ctr += 1
                    }
                } else {
                    deleteEventsMedia()
                }
            } else {
                showFailedMessage()
            }
        }
    }

    //2nd Process
    private fun deleteEventsMedia(){
        Toast.makeText(context, "deleteEventsMedia", Toast.LENGTH_SHORT).show()
        FirebaseUtil().retrieveDeletedEventsCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){

                    for (event in it.result.documents){
                        val eventModel = event.toObject(EventModel::class.java)!!
                        if (eventModel.eventImageName.isNotEmpty()){
                            addListToMediaList(listOf(eventModel.eventImageName))
                            if (ctr == size)
                                deleteForumsMedia()
                        } else if (ctr == size)
                            deleteForumsMedia()

                        ctr += 1
                    }
                } else {
                    deleteForumsMedia()
                }
            } else {
                showFailedMessage()
            }
        }
    }
    //3rd Process
    private fun deleteForumsMedia(){
        Toast.makeText(context, "deleteForumsMedia", Toast.LENGTH_SHORT).show()
        FirebaseUtil().retrieveDeletedForumsCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){
                    for (forum in it.result.documents){
                        val forumModel = forum.toObject(ForumModel::class.java)!!
                        if (forumModel.contentList.isNotEmpty()){
                            addListToMediaList(forumModel.contentList)
                            if (ctr == size)
                                deleteProductsMedia()
                        } else if (ctr == size)
                            deleteProductsMedia()

                        ctr += 1
                    }
                } else {
                    deleteProductsMedia()
                }
            } else {
                showFailedMessage()
            }
        }

    }
    //4th Process
    private fun deleteProductsMedia(){
        Toast.makeText(context, "deleteProductsMedia", Toast.LENGTH_SHORT).show()
        FirebaseUtil().retrieveDeletedProductsCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){
                    for (product in it.result.documents){
                        val productModel = product.toObject(ProductModel::class.java)!!
                        if (productModel.imageList.isNotEmpty()){
                            addListToMediaList(productModel.imageList)
                            if (ctr == size)
                                deleteCompetition()
                        } else if (ctr == size)
                            deleteCompetition()

                        ctr += 1
                    }
                } else {
                    deleteCompetition()
                }
            } else {
                showFailedMessage()
            }
        }
    }

    //5th Process
    private fun deleteCompetition(){
        Toast.makeText(context, "deleteCompetition", Toast.LENGTH_SHORT).show()
        FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){
                    for (competition in it.result.documents){
                        val competitionModel = competition.toObject(CompetitionModel::class.java)!!
                        val imageInstructionList:ArrayList<String> = arrayListOf()

                        //Deletes media in the instructions
                        for (instruction in competitionModel.instruction){
                            val imageInstruction = instruction.value[1]
                            if(imageInstruction.isNotEmpty()){
                                imageInstructionList.add(imageInstruction)
                            }
                        }


                        if (imageInstructionList.isNotEmpty()){
                            addListToMediaList(imageInstructionList)
                            if (ctr == size)
                                deleteFiles()
                        } else if (ctr == size)
                            deleteFiles()

                        ctr += 1
                    }
                } else {
                    deleteFiles()
                }
            } else {
                showFailedMessage()
            }
        }
    }
    //6th Process
    private fun deleteFiles(){
        Toast.makeText(context, "deleteFiles", Toast.LENGTH_SHORT).show()
        FirebaseUtil().retrieveCommunityFilesCollection(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                var ctr = 1
                var size = it.result.documents.size

                if (it.result.documents.isNotEmpty()){
                    for (file in it.result.documents){
                        val fileModel = file.toObject(FileModel::class.java)!!
                        if (fileModel.fileUrl.isNotEmpty()){
                            addListToMediaList(listOf(fileModel.fileUrl))
                            if (ctr == size)
                                deleteTextChannels()
                        } else if (ctr == size)
                            deleteTextChannels()

                        ctr += 1
                    }
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
        Toast.makeText(context, "deleteTextChannels", Toast.LENGTH_SHORT).show()
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
        deleteMediaOrFile{isDone ->
            if (isDone){
                Toast.makeText(context, "deleteCommunity", Toast.LENGTH_SHORT).show()
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnCompleteListener {community ->
                    if (community.result.exists()){
                        val communityModel = community.result.toObject(CommunityModel::class.java)!!

                        //deletes community profile
                        if (communityModel.communityMedia.containsKey("community_photo")){
                            var imageUrl = communityModel.communityMedia["community_photo"]!!
                            FirebaseUtil().retrieveCommunityProfilePicRef(imageUrl).delete()
                        }

                        //deletes community banner
                        if (communityModel.communityMedia.containsKey("community_banner_photo")){
                            var imageUrl = communityModel.communityMedia["community_banner_photo"]!!
                            FirebaseUtil().retrieveCommunityBannerPicRef(imageUrl).delete()
                        }

                        //deletes community from community collection
                        FirebaseUtil().retrieveCommunityDocument(communityId).delete().addOnSuccessListener {
                            dialog.dismiss()
                            Toast.makeText(context, "The community is deleted", Toast.LENGTH_SHORT).show()
                            AppUtil().headToMainActivity(context)
                        }.addOnFailureListener {
                            dialog.dismiss()
                            Toast.makeText(context, "Community deletion failed. Please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}