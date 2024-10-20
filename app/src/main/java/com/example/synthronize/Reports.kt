package com.example.synthronize

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.ReportsAdapter
import com.example.synthronize.databinding.ActivityCommunityReportsBinding
import com.example.synthronize.databinding.DialogListBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.ReportModel
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

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
            binding.communityButtons.visibility = View.GONE
            navigate("personal")

        } else {
            binding.feedsBtn.setOnClickListener {
                navigate("feeds")
            }
            binding.eventsBtn.setOnClickListener {
                navigate("events")
            }
            binding.forumsBtn.setOnClickListener {
                navigate("forums")
            }
            binding.marketBtn.setOnClickListener {
                navigate("market")
            }
            binding.competitionsBtn.setOnClickListener {
                navigate("competitions")
            }
            binding.filesBtn.setOnClickListener {
                navigate("files")
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }
    


    private fun navigate(tab:String){
        binding.reportsRV.visibility = View.GONE
        if (tab == "feeds"){
            openListDialog("Post")
            currentTab = "feeds"

        } else if (tab == "events") {
            openListDialog("Event")
            currentTab = "events"

        }  else if (tab == "forums") {
            openListDialog("Forum")
            currentTab = "forums"

        } else if (tab == "market") {
            openListDialog("Product")
            currentTab = "market"

        } else if (tab == "competitions") {
            openListDialog("Competition")
            currentTab = "competitions"

        }  else if (tab == "files") {
            openListDialog("File")
            currentTab = "files"

        } else if (tab == "personal") {
            binding.reportsRV.visibility = View.VISIBLE
            setupPersonalReport()
            currentTab = "personal"
        }
    }

    private fun openListDialog(reportType:String){
        
        val reportsBinding = DialogListBinding.inflate(layoutInflater)
        val reportsDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(reportsBinding.root))
            .create()

        reportsBinding.searchContainerLL.visibility = View.GONE
        reportsBinding.toolbarTitleTV.text = "${reportType}s"



        reportsBinding.backBtn.setOnClickListener {
            reportsDialog.dismiss()
            if (::reportsAdapter.isInitialized)
                reportsAdapter.stopListening()
        }
        
        val query = FirebaseUtil().retrieveCommunityReportsCollection(communityId)
            .whereEqualTo("reportType", reportType)
            .whereEqualTo("reviewed", false)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)


        // Add a listener to handle success or failure of the query
        query.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.reportsRefreshLayout.isRefreshing = false
            }
        }

        val options: FirestoreRecyclerOptions<ReportModel> =
            FirestoreRecyclerOptions.Builder<ReportModel>().setQuery(query, ReportModel::class.java).build()

        reportsBinding.listRV.layoutManager = LinearLayoutManager(this)
        reportsAdapter = ReportsAdapter(this, options, false, communityId)
        reportsBinding.listRV.adapter = reportsAdapter
        reportsAdapter.startListening()

        reportsDialog.show()

    }

    private fun setupPersonalReport(){
        if (communityId == "null" || communityId.isEmpty()){

            binding.reportsRefreshLayout.isRefreshing = false

            //Personal report within a community
            val query = FirebaseUtil().retrieveReportsCollection()
                .whereEqualTo("ownerId", FirebaseUtil().currentUserUid())
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)


            // Add a listener to handle success or failure of the query
            query.addSnapshotListener { _, e ->
                if (e != null) {
                    // Handle the error here (e.g., log the error or show a message to the user)
                    Log.e("Firestore Error", "Error while fetching data", e)
                    return@addSnapshotListener
                } else {
                    binding.reportsRefreshLayout.isRefreshing = false
                }
            }

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

            // Add a listener to handle success or failure of the query
            query.addSnapshotListener { _, e ->
                if (e != null) {
                    // Handle the error here (e.g., log the error or show a message to the user)
                    Log.e("Firestore Error", "Error while fetching data", e)
                    return@addSnapshotListener
                } else {
                    binding.reportsRefreshLayout.isRefreshing = false
                }
            }

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