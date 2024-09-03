package com.example.synthronize.utils
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.CreatePost
import com.example.synthronize.adapters.ChatroomAdapter
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.DialogForwardContentBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogReportBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.ReportModel
import com.example.synthronize.model.UserModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
//ALERT DIALOGS AND METHODS
class DialogUtil: OnItemClickListener {

    //OPENS DIALOG FOR REPORT
    fun openReportDialog(context: Context, inflater: LayoutInflater, reportType:String, idToReport:String, communityId:String = ""){
        var reasonToReport = ""

        val dialogReportBinding = DialogReportBinding.inflate(inflater)
        val dialogReport = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogReportBinding.root))
            .setMargin(0, 200, 0, 0)
            .setCancelable(true)
            .setExpanded(false)
            .setGravity(Gravity.BOTTOM)
            .create()

        if (reportType == "User"){
            //hide some options for user
            dialogReportBinding.selfharmLayout.visibility = View.GONE
        } else if (reportType == "Community"){
            //hide some options for community
            dialogReportBinding.selfharmLayout.visibility = View.GONE
            dialogReportBinding.impersonationLayout.visibility = View.GONE
        } else {
            //hide some options for community content
            dialogReportBinding.impersonationLayout.visibility = View.GONE
        }

        //options
        dialogReportBinding.spamRB.setOnClickListener {
            reasonToReport = "Spam"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.spamRB)
        }
        dialogReportBinding.inappropriateRB.setOnClickListener {
            reasonToReport = "Inappropriate Content"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.inappropriateRB)
        }
        dialogReportBinding.misinfoRB.setOnClickListener {
            reasonToReport = "Misinformation"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.misinfoRB)
        }
        dialogReportBinding.harassOrBullyRB.setOnClickListener {
            reasonToReport = "Harassment or Bullying"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.harassOrBullyRB)
        }
        dialogReportBinding.hateSpeechRB.setOnClickListener {
            reasonToReport = "Hate Speech"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.hateSpeechRB)
        }
        dialogReportBinding.violenceRB.setOnClickListener {
            reasonToReport = "Violence or Threats"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.violenceRB)
        }
        dialogReportBinding.nudityRB.setOnClickListener {
            reasonToReport = "Nudity or Sexual Content"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.nudityRB)
        }
        dialogReportBinding.selfHarmRB.setOnClickListener {
            reasonToReport = "Self-Harm"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.selfHarmRB)
        }
        dialogReportBinding.scamRB.setOnClickListener {
            reasonToReport = "Scam or Fraud"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.scamRB)
        }
        dialogReportBinding.impersonationRB.setOnClickListener {
            reasonToReport = "Impersonation"
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.impersonationRB)
        }

        //Other option
        dialogReportBinding.othersRB.setOnClickListener {
            reasonToReport = "Other"
            dialogReportBinding.otherEdtTxt.visibility = View.VISIBLE
            unselectRadioButtons(dialogReportBinding, dialogReportBinding.othersRB)
        }

        //Submit Button
        dialogReportBinding.submitBtn.setOnClickListener {
            if (reasonToReport == "Other"){
                val reason = dialogReportBinding.otherEdtTxt.text.toString()
                if (reason.isNotEmpty() && reason != "null"){
                    sendReportToFirebase(dialogReport, context, reportType, idToReport, reasonToReport, communityId)
                } else {
                    Toast.makeText(context, "Please specify your reason", Toast.LENGTH_SHORT).show()
                }
            } else if(reasonToReport.isNotEmpty()){
                sendReportToFirebase(dialogReport, context, reportType, idToReport, reasonToReport, communityId)
            } else {
                Toast.makeText(context, "Select your reason", Toast.LENGTH_SHORT).show()
            }
        }
        dialogReportBinding.downBtn.setOnClickListener {
            dialogReport.dismiss()
        }
        dialogReport.show()
    }

    private fun sendReportToFirebase(dialogReport:DialogPlus, context: Context, reportType:String, idToReport:String, reasonToReport:String,communityId:String = ""){
        var reportModel = ReportModel()

        if (reportType == "Community" || reportType == "User"){
            //For Web Admin
            FirebaseUtil().retrieveReportsCollection().add(reportModel).addOnSuccessListener {
                reportModel = ReportModel(
                    reportId = it.id,
                    reportType = reportType,
                    ownerId = FirebaseUtil().currentUserUid(),
                    reportedId = idToReport,
                    reason = reasonToReport,
                    createdTimestamp = Timestamp.now()
                )
                FirebaseUtil().retrieveReportsCollection().document(it.id).set(reportModel).addOnSuccessListener {
                    Toast.makeText(context, "Report successfully sent", Toast.LENGTH_SHORT).show()
                    dialogReport.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to send report", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            //For within Community
            FirebaseUtil().retrieveCommunityReportsCollection(communityId).add(reportModel).addOnSuccessListener {
                reportModel = ReportModel(
                    reportId = it.id,
                    reportType = reportType,
                    ownerId = FirebaseUtil().currentUserUid(),
                    reportedId = idToReport,
                    reason = reasonToReport,
                    createdTimestamp = Timestamp.now()
                )
                FirebaseUtil().retrieveCommunityReportsCollection(communityId).document(it.id).set(reportModel).addOnSuccessListener {
                    Toast.makeText(context, "Report successfully sent", Toast.LENGTH_SHORT).show()
                    dialogReport.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to send report", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun unselectRadioButtons(dialogReportBinding: DialogReportBinding, selectedRadioButton: RadioButton){
        dialogReportBinding.spamRB.isChecked = false
        dialogReportBinding.inappropriateRB.isChecked = false
        dialogReportBinding.misinfoRB.isChecked = false
        dialogReportBinding.harassOrBullyRB.isChecked = false
        dialogReportBinding.hateSpeechRB.isChecked = false
        dialogReportBinding.violenceRB.isChecked = false
        dialogReportBinding.nudityRB.isChecked = false
        dialogReportBinding.selfHarmRB.isChecked = false
        dialogReportBinding.othersRB.isChecked = false
        dialogReportBinding.scamRB.isChecked = false
        dialogReportBinding.impersonationRB.isChecked = false
        dialogReportBinding.otherEdtTxt.visibility = View.GONE

        //selected Radio Button
        if (selectedRadioButton == dialogReportBinding.othersRB){
            //if the option is Other
            selectedRadioButton.isChecked = true
            dialogReportBinding.otherEdtTxt.visibility = View.VISIBLE
        } else {
            selectedRadioButton.isChecked = true
        }
    }


    //OPENS DIALOG FOR SEND POST
    fun openForwardContentDialog(context:Context, inflater: LayoutInflater, postId:String, communityIdOfPost:String){
        //prepares dialog
        val dialogForwardContentBinding = DialogForwardContentBinding.inflate(inflater)
        val forwardContentDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogForwardContentBinding.root))
            .setMargin(0, 300, 0, 0)
            .setCancelable(true)
            .setExpanded(false)
            .setGravity(Gravity.BOTTOM)
            .create()

        //query
        val myQuery: Query = FirebaseUtil().retrieveAllChatRoomReferences()
            .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
        val options:FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(myQuery, ChatroomModel::class.java).build()
        val chatroomAdapter = ChatroomAdapter(context, options, postId, communityIdOfPost)

        dialogForwardContentBinding.resultsRV.layoutManager = LinearLayoutManager(context)
        dialogForwardContentBinding.resultsRV.adapter = chatroomAdapter
        chatroomAdapter.startListening()

        dialogForwardContentBinding.headerLayout.setOnClickListener {
            forwardContentDialog.dismiss()
        }
        dialogForwardContentBinding.downBtn.setOnClickListener {
            forwardContentDialog.dismiss()
        }

        forwardContentDialog.show()
    }






    //OPENS DIALOG FOR FEED MENU
    fun openMenuDialog(context:Context, inflater: LayoutInflater, contentType:String, contentId:String, contentOwnerId:String, communityId:String, extraId:String = ""){
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            val menuDialogBinding = DialogMenuBinding.inflate(inflater)
            val menuDialog = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(menuDialogBinding.root))
                .setCancelable(true)
                .setExpanded(false)
                .setGravity(Gravity.CENTER)
                .create()

            if (contentOwnerId == FirebaseUtil().currentUserUid() ||
                AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"), FirebaseUtil().currentUserUid())){
                //TODO for moderator
                //displays delete post option if the user is the owner or admin of the community
                menuDialogBinding.option1.visibility = View.VISIBLE
                menuDialogBinding.optiontitle1.text = "Delete $contentType"
                menuDialogBinding.optiontitle1.setOnClickListener {
                    menuDialog.dismiss()
                    Handler().postDelayed({
                        val warningDialogBinding = DialogWarningMessageBinding.inflate(inflater)
                        val warningDialog = DialogPlus.newDialog(context)
                            .setContentHolder(ViewHolder(warningDialogBinding.root))
                            .setCancelable(true)
                            .setMargin(100, 800, 100, 800)
                            .setGravity(Gravity.CENTER)
                            .create()

                        warningDialogBinding.titleTV.text = "Delete $contentType"
                        warningDialogBinding.messageTV.text = "Do you want to delete this ${contentType.lowercase()}?"
                        warningDialogBinding.yesBtn.setOnClickListener {
                            deleteContent(contentType, contentId, communityId, extraId)
                            warningDialog.dismiss()
                        }
                        warningDialogBinding.NoBtn.setOnClickListener {
                            warningDialog.dismiss()
                        }

                        warningDialog.show()

                    }, 500)
                }

                //displays edit post option if the user is the owner or admin of the community
                menuDialogBinding.option2.visibility = View.VISIBLE
                menuDialogBinding.optiontitle2.text = "Edit $contentType"
                menuDialogBinding.optiontitle2.setOnClickListener {
                    menuDialog.dismiss()
                    Handler().postDelayed({
                        editContent(context, contentType, contentId, communityId, extraId)
                    }, 500)
                }
            } else {
                menuDialogBinding.option1.visibility = View.VISIBLE
                menuDialogBinding.optiontitle1.text = "Report $contentType"
                menuDialogBinding.optiontitle1.setOnClickListener {
                    menuDialog.dismiss()
                    Handler().postDelayed({
                        DialogUtil().openReportDialog(context, inflater, contentType, contentId, communityId)
                    }, 500)
                }
            }
            menuDialog.show()
        }
    }

    private fun editContent(context:Context, contentType:String, contentId:String, communityId:String, extraId: String = ""){
        when(contentType){
            "Post" -> {
                val intent = Intent(context, CreatePost::class.java)
                intent.putExtra("postId", contentId)
                intent.putExtra("communityId", communityId)
                context.startActivity(intent)
            }
            "Competition" -> {
                //FOR COMPETITION
            }
            "File Submission" -> {
                //FOR COMPETITION FILE
            }
            "File" -> {
                //FOR FILES
            }
        }
    }

    private fun deleteContent(contentType:String, contentId:String, communityId:String, extraId: String = ""){
        when(contentType){
            "Post" -> {
                FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(contentId).get().addOnSuccessListener {
                    val postModel = it.toObject(PostModel::class.java)!!
                    //Deletes media
                    deleteMediaOrFile(postModel.contentList)
                    //Deletes Post
                    FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(contentId).delete()
                }
            }
            "Competition" -> {
                FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(contentId).get().addOnSuccessListener {
                    val competitionModel = it.toObject(CompetitionModel::class.java)!!
                    val imageInstructionList:ArrayList<String> = arrayListOf()
                    val fileUrlList:ArrayList<String> = arrayListOf()

                    //Deletes media in the instructions
                    for (instruction in competitionModel.instruction){
                        val imageInstruction = instruction.value[1]
                        if(imageInstruction.isNotEmpty()){
                            imageInstructionList.add(imageInstruction)
                        }
                    }
                    //Deletes file from the firebase
                    for (file in competitionModel.contestants){
                        val fileUrl = file.value
                        if (fileUrl.isNotEmpty()){
                            fileUrlList.add(fileUrl)
                        }
                    }
                    deleteMediaOrFile(fileUrlList)
                    //Deletes all fileModels from Firebase
                    if (fileUrlList.isNotEmpty()){
                        FirebaseUtil().retrieveCommunityFilesCollection(communityId)
                            .whereIn("fileUrl", fileUrlList).get().addOnSuccessListener {fileModels ->
                                for (fileModel in fileModels){
                                    FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(fileModel.id).delete()
                                }
                            }
                    }
                    //Deletes Image Instructions
                    deleteMediaOrFile(imageInstructionList)
                    //Deletes Competition
                    FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(contentId).delete()


                }
            }
            "Product" -> {
                FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(contentId).get().addOnSuccessListener {
                    val productModel = it.toObject(ProductModel::class.java)!!
                    //Deletes File from firebase storage
                    deleteMediaOrFile(productModel.imageList)
                    //Deletes file model from firestore database
                    FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(contentId).delete()
                }
            }
            "File" -> {
                FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(contentId).get().addOnSuccessListener {
                    val fileModel = it.toObject(FileModel::class.java)!!
                    //Deletes File from firebase storage
                    deleteMediaOrFile(listOf(fileModel.fileUrl))
                    //Deletes file model from firestore database
                    FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(contentId).delete()
                }
            }
            "File Submission" -> {
                //FOR FILE SUBMISSION FOR COMPETITION

                //the extraId is used as competitionId
                FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(extraId).get().addOnSuccessListener {
                    val competitionModel = it.toObject(CompetitionModel::class.java)!!

                    FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(contentId).get().addOnSuccessListener {file ->
                        val fileModel = file.toObject(FileModel::class.java)!!

                        //Deletes file from the firestore database
                        for (contestant in competitionModel.contestants){
                            val fileUrl = contestant.value
                            if (fileUrl == fileModel.fileUrl){
                                val updates = mapOf(
                                    "contestants.${contestant.key}" to ""
                                )
                                FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(extraId).update(updates)
                            }
                        }

                        //Deletes File from firebase storage
                        deleteMediaOrFile(listOf(fileModel.fileUrl))
                    }
                }
            }
        }
    }

    private fun deleteMediaOrFile(mediaList:List<String>){
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
        }
    }









    //OPENS COMMUNITY PREVIEW DIALOG
    fun openCommunityPreviewDialog(context:Context, layoutInflater: LayoutInflater, communityModel: CommunityModel){
        val dialogPlusBinding = DialogCommunityPreviewBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogPlusBinding.root))
            .setGravity(Gravity.CENTER)
            .setExpanded(false)
            .create()

        //Community details
        dialogPlusBinding.communityNameTV.text = communityModel.communityName
        dialogPlusBinding.communityDescriptionTV.text = communityModel.communityDescription
        dialogPlusBinding.totalMembersCountTV.text = "${communityModel.communityMembers.size}"
        getFriendsJoinedCount(communityModel.communityMembers.keys.toList()){count->
            dialogPlusBinding.friendsJoinedCountTV.text = count.toString()
        }
        dialogPlusBinding.createdDateTV.text =  DateAndTimeUtil().formatTimestampToDate(communityModel.communityCreatedTimestamp)
        AppUtil().setCommunityProfilePic(context, communityModel.communityId, dialogPlusBinding.communityProfileCIV)
        AppUtil().setCommunityBannerPic(context, communityModel.communityId, dialogPlusBinding.communityBannerIV)
        AppUtil().changeCommunityButtonStates(context, dialogPlusBinding.communityActionBtn, communityModel.communityId)

        Handler().postDelayed({
            dialogPlus.show()
        }, 500)
    }

    private fun getFriendsJoinedCount(membersList: List<String>, callback: (Int) -> Unit) {
        var count = 0
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            for (friend in userModel.friendsList){
                if (AppUtil().isIdOnList(membersList, friend)){
                    count += 1
                }
            }
            callback(count)
        }.addOnFailureListener {
            callback(count)
        }

    }



    override fun onItemClick(id: String, isChecked: Boolean) {
        TODO("Not yet implemented")
    }

}