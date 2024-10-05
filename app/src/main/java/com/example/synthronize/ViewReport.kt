package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityViewReportBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.ForumModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.ReportModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.toObject

class ViewReport : AppCompatActivity() {
    private lateinit var binding:ActivityViewReportBinding
    private var communityId = ""
    private var reportId = ""
    private var toReview = false
    private var isCommunityContent = false
    private lateinit var reportModel: ReportModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        reportId = intent.getStringExtra("reportId").toString()
        toReview = intent.getBooleanExtra("toReview", false)
        isCommunityContent = communityId.isNotEmpty() && communityId != "null"

        if (isCommunityContent){
            //Within Community (Feeds, Market, Forums, Files)
            FirebaseUtil().retrieveCommunityReportsCollection(communityId).document(reportId).get().addOnSuccessListener {
                reportModel = it.toObject(ReportModel::class.java)!!

                when(reportModel.reportType){
                    "Post" -> bindPostPreview()
                    "Community" -> bindCommunityPreview()
                    "User" -> bindUserPreview()
                    "Event" -> bindEventPreview()
                    "Forum" -> bindForumPreview()
                    "Product" -> bindProductPreview()
                    "File" -> bindFilePreview()
                    "Competition" -> bindCompetitionPreview()
                }

                bindDetails()

                if (toReview){
                    binding.markAsReviewedBtn.visibility = View.VISIBLE
                    binding.markAsReviewedBtn.setOnClickListener {
                        FirebaseUtil().retrieveCommunityReportsCollection(communityId).document(reportId).update("reviewed", true).addOnSuccessListener {
                            Toast.makeText(this, "Report is successfully reviewed", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                        }.addOnFailureListener {
                            Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {

                    if (reportModel.reviewed){
                        binding.reviewedMessageTV.visibility = View.VISIBLE
                    } else {
                        binding.deleteReportBtn.visibility = View.VISIBLE
                        binding.markAsReviewedBtn.setOnClickListener {
                            FirebaseUtil().retrieveCommunityReportsCollection(communityId).document(reportId).delete().addOnSuccessListener {
                                Toast.makeText(this, "Report is successfully deleted", Toast.LENGTH_SHORT).show()
                                onBackPressed()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Failed to delete report, please try again", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                }
            }
        } else {
            //Outside Community (User, Community)
            FirebaseUtil().retrieveReportsCollection().document(reportId).get().addOnSuccessListener {
                reportModel = it.toObject(ReportModel::class.java)!!
                bindDetails()

                if (reportModel.reportType == "Community"){
                    bindCommunityPreview()
                } else if (reportModel.reportType == "User"){
                    bindUserPreview()
                }

                if (reportModel.reviewed){
                    binding.reviewedMessageTV.visibility = View.VISIBLE
                } else {
                    binding.deleteReportBtn.visibility = View.VISIBLE
                    binding.deleteReportBtn.setOnClickListener {
                        FirebaseUtil().retrieveReportsCollection().document(reportId).delete().addOnSuccessListener {
                            Toast.makeText(this, "Report is successfully deleted", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to delete report, please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }
        }
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun bindForumPreview() {
        FirebaseUtil().retrieveCommunityForumsCollection(communityId).document(reportModel.reportedId).get().addOnCompleteListener { forum ->
            if (forum.result.exists()) {
                val forumModel = forum.result.toObject(ForumModel::class.java)!!

                binding.imageWithCaptionLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(forumModel.ownerId).get().addOnCompleteListener {user ->
                    if (user.isSuccessful){
                        val reportedUser = user.result.toObject(UserModel::class.java)!!
                        AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                        binding.reportedUsernameTV.text = "@${reportedUser.username}"
                        binding.reportedUserFullnameTV.text = reportedUser.fullName
                        binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(forumModel.createdTimestamp)
                    }
                }
                if (forumModel.contentList.isNotEmpty()){
                    ContentUtil().setImageContent(this, forumModel.contentList[0], binding.contentIV)
                } else {
                    binding.contentIV.visibility = View.GONE
                }
                binding.contentCaptionTV.text = forumModel.caption

                binding.imageWithCaptionLayout.setOnClickListener {
                    val intent = Intent(this, ViewThread::class.java)
                    intent.putExtra("communityId", communityId )
                    intent.putExtra("forumId", forumModel.forumId)
                    startActivity(intent)
                }

            }
        }
    }

    private fun bindEventPreview() {
        FirebaseUtil().retrieveCommunityEventsCollection(communityId).document(reportModel.reportedId).get().addOnCompleteListener { event ->
            if (event.result.exists()) {
                val eventModel = event.result.toObject(EventModel::class.java)!!

                binding.imageWithCaptionLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(eventModel.eventOwnerId).get().addOnCompleteListener {user ->
                    if (user.isSuccessful){
                        val reportedUser = user.result.toObject(UserModel::class.java)!!
                        AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                        binding.reportedUsernameTV.text = "@${reportedUser.username}"
                        binding.reportedUserFullnameTV.text = reportedUser.fullName
                        binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(eventModel.createdTimestamp)
                    }
                }

                if (eventModel.eventImageName.isNotEmpty()){
                    ContentUtil().setImageContent(this, eventModel.eventImageName, binding.contentIV)
                } else {
                    binding.contentIV.visibility = View.GONE
                }

                binding.contentCaptionTV.text = eventModel.eventName

                binding.imageWithCaptionLayout.setOnClickListener {
                    val intent = Intent(this, ViewEvent::class.java)
                    intent.putExtra("communityId", communityId)
                    intent.putExtra("eventId", eventModel.eventId)
                    startActivity(intent)
                }
            }
        }
    }

    private fun bindFilePreview(){
        FirebaseUtil().retrieveCommunityFilesCollection(communityId).document(reportModel.reportedId).get().addOnCompleteListener {file ->
            if (file.result.exists() && file.isSuccessful){
                val fileModel = file.result.toObject(FileModel::class.java)!!
                binding.imageWithCaptionLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(fileModel.ownerId).get().addOnCompleteListener {user ->
                    if (user.isSuccessful){
                        val reportedUser = user.result.toObject(UserModel::class.java)!!
                        AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                        binding.reportedUsernameTV.text = "@${reportedUser.username}"
                        binding.reportedUserFullnameTV.text = reportedUser.fullName
                        binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(fileModel.createdTimestamp)
                    }
                }

                val extension = fileModel.fileName.split('.').last()

                if (extension == "pdf"){
                    binding.contentIV.setImageResource(R.drawable.pdf_icon)
                } else if (extension == "docx"){
                    binding.contentIV.setImageResource(R.drawable.docx_icon)
                } else if (extension == "excel"){
                    binding.contentIV.setImageResource(R.drawable.excel_icon)
                } else {
                    binding.contentIV.setImageResource(R.drawable.file_icon)
                }

                binding.contentCaptionTV.text = fileModel.fileName

                binding.imageWithCaptionLayout.setOnClickListener {
                    val intent = Intent(this, ViewFile::class.java)
                    intent.putExtra("communityId", communityId)
                    intent.putExtra("fileId", fileModel.fileId)
                    if (fileModel.forCompetition){
                        intent.putExtra("contentType", "File Submission")
                    } else {
                        intent.putExtra("contentType", "File")
                    }
                    startActivity(intent)

                }
            }
        }
    }
    private fun bindCompetitionPreview(){
        FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(reportModel.reportedId).get().addOnCompleteListener {competition ->
            if (competition.result.exists() && competition.isSuccessful){
                val competitionModel = competition.result.toObject(CompetitionModel::class.java)!!
                binding.imageWithCaptionLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(competitionModel.ownerId).get().addOnCompleteListener {user ->
                    if (user.isSuccessful){
                        val reportedUser = user.result.toObject(UserModel::class.java)!!
                        AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                        binding.reportedUsernameTV.text = "@${reportedUser.username}"
                        binding.reportedUserFullnameTV.text = reportedUser.fullName
                        binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(competitionModel.createdTimestamp)
                    }
                }

                binding.contentIV.visibility = View.GONE
                binding.contentCaptionTV.text = "${competitionModel.competitionName} (${competitionModel.description})"

                binding.imageWithCaptionLayout.setOnClickListener {
                    val intent = Intent(this, ViewCompetition::class.java)
                    intent.putExtra("competitionId", competitionModel.competitionId)
                    intent.putExtra("communityId", competitionModel.communityId)
                    intent.putExtra("isUserAdmin", true)
                    startActivity(intent)
                }
            }
        }
    }

    private fun bindProductPreview() {
        FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(reportModel.reportedId).get().addOnCompleteListener {product ->
            if (product.result.exists() && product.isSuccessful){
                val productModel = product.result.toObject(ProductModel::class.java)!!
                binding.imageWithCaptionLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(productModel.ownerId).get().addOnCompleteListener {user ->
                    if (user.isSuccessful){
                        val reportedUser = user.result.toObject(UserModel::class.java)!!
                        AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                        binding.reportedUsernameTV.text = "@${reportedUser.username}"
                        binding.reportedUserFullnameTV.text = reportedUser.fullName
                        binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(productModel.createdTimestamp)
                    }
                }
                if (productModel.imageList.isNotEmpty()){
                    ContentUtil().setImageContent(this, productModel.imageList[0], binding.contentIV)
                } else {
                    binding.contentIV.visibility = View.GONE
                }
                binding.contentCaptionTV.text = productModel.productName

                binding.imageWithCaptionLayout.setOnClickListener {
                    val intent = Intent(this, ViewProduct::class.java)
                    intent.putExtra("communityId", communityId)
                    intent.putExtra("productId", productModel.productId)
                    startActivity(intent)
                }
            }
        }
    }

    private fun bindUserPreview(){
        FirebaseUtil().targetUserDetails(reportModel.reportedId).get().addOnCompleteListener {
            if (it.result.exists() && it.isSuccessful){
                val userModel = it.result.toObject(UserModel::class.java)!!
                binding.profileCommunityLayout.visibility = View.VISIBLE

                AppUtil().setUserProfilePic(this, userModel.userID, binding.communityProfileCIV)
                AppUtil().setUserCoverPic(this, userModel.userID, binding.bannerCoverIV)

                binding.communityProfileNameTV.text = userModel.fullName
                binding.communityProfileUserNameTV.text = "@${userModel.username}"
                binding.communityProfileDescriptionTV.text = userModel.description

                binding.profileCommunityLayout.setOnClickListener {
                    AppUtil().headToUserProfile(this, userModel.userID)
                }
            }
        }
    }

    private fun bindCommunityPreview(){
        FirebaseUtil().retrieveCommunityDocument(reportModel.reportedId).get().addOnCompleteListener {community ->
            if (community.result.exists() && community.isSuccessful){
                val communityModel = community.result.toObject(CommunityModel::class.java)!!
                binding.profileCommunityLayout.visibility = View.VISIBLE
                binding.communityProfileUserNameTV.visibility = View.GONE

                AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.communityProfileCIV)
                AppUtil().setCommunityBannerPic(this, communityModel.communityId, binding.bannerCoverIV)

                binding.communityProfileNameTV.text = communityModel.communityName
                binding.communityProfileDescriptionTV.text = communityModel.communityDescription

                binding.profileCommunityLayout.setOnClickListener {
                    DialogUtil().openCommunityPreviewDialog(this, layoutInflater, communityModel)
                }
            }
        }

    }

    private fun bindPostPreview() {
        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(reportModel.reportedId).get().addOnCompleteListener {post ->
            if (post.result.exists() && post.isSuccessful){
                val postModel = post.result.toObject(PostModel::class.java)!!
                binding.imageWithCaptionLayout.visibility = View.VISIBLE
                FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnCompleteListener {user ->
                    if (user.isSuccessful){
                        val reportedUser = user.result.toObject(UserModel::class.java)!!
                        AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                        binding.reportedUsernameTV.text = "@${reportedUser.username}"
                        binding.reportedUserFullnameTV.text = reportedUser.fullName
                        binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(postModel.createdTimestamp)
                    }
                }
                if (postModel.contentList.isNotEmpty()){
                    ContentUtil().setImageContent(this, postModel.contentList[0], binding.contentIV)
                } else {
                    binding.contentIV.visibility = View.GONE
                }
                binding.contentCaptionTV.text = postModel.caption

                binding.imageWithCaptionLayout.setOnClickListener {
                    val intent = Intent(this, ViewPost::class.java)
                    intent.putExtra("communityId", communityId)
                    intent.putExtra("postId", postModel.postId)
                    startActivity(intent)
                }
            }
        }
    }

    private fun bindDetails() {
        FirebaseUtil().targetUserDetails(reportModel.ownerId).get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!

            AppUtil().setUserProfilePic(this, userModel.userID, binding.userCircleImageView)
            binding.fullnameTV.text = userModel.fullName
            binding.usernameTV.text = "@${userModel.username}"
            binding.createdTimestampTV.text = DateAndTimeUtil().formatTimestampToDate(reportModel.createdTimestamp)
            binding.reasonTV.text = "The ${reportModel.reportType} is reported as '${reportModel.reason}'"
        }
    }
}