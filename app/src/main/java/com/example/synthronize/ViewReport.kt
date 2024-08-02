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
import com.example.synthronize.model.PostModel
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
                    //TODO with forums etc
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

    private fun bindUserPreview(){
        FirebaseUtil().targetUserDetails(reportModel.reportedId).get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
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

    private fun bindCommunityPreview(){
        FirebaseUtil().retrieveCommunityDocument(reportModel.reportedId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
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

    private fun bindPostPreview() {
        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(reportModel.reportedId).get().addOnSuccessListener {
            val postModel = it.toObject(PostModel::class.java)!!
            binding.imageWithCaptionLayout.visibility = View.VISIBLE
            FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener {user ->
                val reportedUser = user.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(this, reportedUser.userID, binding.reportedUserCIV)
                binding.reportedUsernameTV.text = "@${reportedUser.username}"
                binding.reportedUserFullnameTV.text = reportedUser.fullName
                binding.reportedPostCreatedTS.text = DateAndTimeUtil().getTimeAgo(postModel.createdTimestamp)
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