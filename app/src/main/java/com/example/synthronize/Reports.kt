package com.example.synthronize

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.ReportsAdapter
import com.example.synthronize.databinding.ActivityCommunityReportsBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.ReportModel
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

//Reports activity for admin and moderator within a community and users outside the community
class Reports : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener {
    private lateinit var binding:ActivityCommunityReportsBinding
    private lateinit var reportsAdapter: ReportsAdapter
    private var isPersonalReport:Boolean = false
    private var communityId = ""
    private var currentTab = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //check for internet
        binding.reportsRefreshLayout.setOnRefreshListener(this)
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        //get intent extras
        communityId = intent.getStringExtra("communityId").toString()
        isPersonalReport = intent.getBooleanExtra("isPersonalReport", false)

        if (isPersonalReport){
            //Personal reports made by a member of the community
            binding.toolbarTitleTV.text = "Reports Filed"
            binding.navigationLayout.visibility = View.GONE
            binding.divider2.visibility = View.GONE
            navigate("personal")

        } else {
            //For Moderator And Admin
            navigate("feeds")

            binding.feedsBtn.setOnClickListener {
                navigate("feeds")
            }
            binding.forumsBtn.setOnClickListener {
                navigate("forums")
            }
            binding.marketBtn.setOnClickListener {
                navigate("market")
            }
            binding.activitiesBtn.setOnClickListener {
                navigate("activities")
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }


    private fun navigate(tab:String){
        val unselectedColor = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(this, R.color.light_teal)
        binding.feedsBtn.setTextColor(unselectedColor)
        binding.forumsBtn.setTextColor(unselectedColor)
        binding.marketBtn.setTextColor(unselectedColor)
        binding.activitiesBtn.setTextColor(unselectedColor)


        if (tab == "feeds"){
            setupCommunityReport("Post")
            binding.feedsBtn.setTextColor(selectedColor)
            currentTab = "feeds"

        } else if (tab == "forums") {
            setupCommunityReport("Forum")
            binding.forumsBtn.setTextColor(selectedColor)
            currentTab = "forums"

        } else if (tab == "market") {
            setupCommunityReport("Product")
            binding.marketBtn.setTextColor(selectedColor)
            currentTab = "market"

        } else if (tab == "activities") {
            setupCommunityReport("File")
            binding.activitiesBtn.setTextColor(selectedColor)
            currentTab = "activities"

        } else if (tab == "personal") {
            setupPersonalReport()
            currentTab = "personal"
        }
    }

    private fun setupCommunityReport(reportType:String){
        val query = FirebaseUtil().retrieveCommunityReportsCollection(communityId)
            .whereEqualTo("reportType", reportType)
            .whereEqualTo("reviewed", false)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        val options: FirestoreRecyclerOptions<ReportModel> =
            FirestoreRecyclerOptions.Builder<ReportModel>().setQuery(query, ReportModel::class.java).build()

        binding.reportsRV.layoutManager = LinearLayoutManager(this)
        reportsAdapter = ReportsAdapter(this, options, false, communityId)
        binding.reportsRV.adapter = reportsAdapter
        reportsAdapter.startListening()

    }

    private fun setupPersonalReport(){
        if (communityId == "null" || communityId.isEmpty()){

            //Personal report within a community
            val query = FirebaseUtil().retrieveReportsCollection()
                .whereEqualTo("ownerId", FirebaseUtil().currentUserUid())
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)

            val options: FirestoreRecyclerOptions<ReportModel> =
                FirestoreRecyclerOptions.Builder<ReportModel>().setQuery(query, ReportModel::class.java).build()

            binding.reportsRV.layoutManager = LinearLayoutManager(this)
            reportsAdapter = ReportsAdapter(this, options, true)
            binding.reportsRV.adapter = reportsAdapter
            reportsAdapter.startListening()

        } else {

            //Personal report within a community
            val query = FirebaseUtil().retrieveCommunityReportsCollection(communityId)
                .whereEqualTo("ownerId", FirebaseUtil().currentUserUid())
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)

            val options: FirestoreRecyclerOptions<ReportModel> =
                FirestoreRecyclerOptions.Builder<ReportModel>().setQuery(query, ReportModel::class.java).build()

            binding.reportsRV.layoutManager = LinearLayoutManager(this)
            reportsAdapter = ReportsAdapter(this, options, true, communityId)
            binding.reportsRV.adapter = reportsAdapter
            reportsAdapter.startListening()

        }

    }



    override fun onStart() {
        super.onStart()
        if (::reportsAdapter.isInitialized){
            reportsAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::reportsAdapter.isInitialized){
            reportsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::reportsAdapter.isInitialized){
            reportsAdapter.stopListening()
        }
    }

    override fun onRefresh() {
        binding.reportsRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            navigate(currentTab)
        },1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}