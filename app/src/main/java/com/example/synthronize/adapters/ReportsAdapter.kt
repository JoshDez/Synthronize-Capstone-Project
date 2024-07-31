package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.ViewReport
import com.example.synthronize.databinding.ItemReportBinding
import com.example.synthronize.model.ReportModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp

class ReportsAdapter(private val context: Context, options: FirestoreRecyclerOptions<ReportModel>,
                     private var isPersonalReport:Boolean = false, private var communityId:String = ""):
    FirestoreRecyclerAdapter<ReportModel, ReportsAdapter.ReportsViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemReportBinding.inflate(inflater, parent, false)
        return ReportsViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: ReportsViewHolder, position: Int, model: ReportModel) {
        holder.bind(model)
    }


    inner class ReportsViewHolder(private val binding: ItemReportBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        private lateinit var reportModel: ReportModel

        fun bind(model: ReportModel){
            reportModel = model
            FirebaseUtil().targetUserDetails(reportModel.ownerId).get().addOnSuccessListener {
                val userModel = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, userModel.userID, binding.userCircleImageView)
                binding.fullnameTV.text = userModel.username
                binding.createdTimestampTV.text = DateAndTimeUtil().formatTimestampToDate(reportModel.createdTimestamp)

                when(reportModel.reportType){
                    "Forums" -> binding.reasonTV.text = "Reported a forum as ${reportModel.reason}"
                    "Feeds" ->  binding.reasonTV.text = "Reported a post as ${reportModel.reason}"
                    "Market" -> binding.reasonTV.text = "Reported a product as ${reportModel.reason}"
                    "Files" ->  binding.reasonTV.text = "Reported a file as ${reportModel.reason}"
                    "Community" ->  binding.reasonTV.text = "Reported a community as ${reportModel.reason}"
                    "User" ->  binding.reasonTV.text = "Reported a user as ${reportModel.reason}"
                }

                if (!isPersonalReport){
                    //within community
                    binding.reportLayout.setOnClickListener {
                        headToViewReport(true)
                    }
                } else {
                    binding.reportLayout.setOnClickListener {
                        headToViewReport(false)
                    }
                }
            }
        }

        private fun headToViewReport(toReview:Boolean){
            val intent = Intent(context, ViewReport::class.java)
            intent.putExtra("communityId", communityId)
            intent.putExtra("reportId", reportModel.reportId)
            intent.putExtra("toReview", toReview)
            intent.putExtra("", toReview)
            context.startActivity(intent)
        }

    }

}