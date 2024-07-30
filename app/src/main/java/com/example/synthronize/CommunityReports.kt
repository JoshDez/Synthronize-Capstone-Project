package com.example.synthronize

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.RequestsAdapter
import com.example.synthronize.databinding.ActivityCommunityReportsBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil

//Reports activity for admin and moderator within a community
class CommunityReports : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener {
    private lateinit var binding:ActivityCommunityReportsBinding
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
            setupFeedsReport()
            binding.feedsBtn.setTextColor(selectedColor)
            currentTab = "feeds"

        } else if (tab == "forums") {
            setupForumsReport()
            binding.forumsBtn.setTextColor(selectedColor)
            currentTab = "forums"

        } else if (tab == "market") {
            setupMarketReport()
            binding.marketBtn.setTextColor(selectedColor)
            currentTab = "market"

        } else if (tab == "activities") {
            setupActivitiesReport()
            binding.activitiesBtn.setTextColor(selectedColor)
            currentTab = "activities"

        } else if (tab == "personal") {
            setupPersonalReport()
            currentTab = "personal"
        }
    }

    private fun setupPersonalReport() {
        //TODO("Not yet implemented")
    }

    private fun setupActivitiesReport() {
        //TODO("Not yet implemented")
    }

    private fun setupMarketReport() {
        //TODO("Not yet implemented")
    }

    private fun setupForumsReport() {
        //TODO("Not yet implemented")
    }

    private fun setupFeedsReport() {
        //TODO("Not yet implemented")
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