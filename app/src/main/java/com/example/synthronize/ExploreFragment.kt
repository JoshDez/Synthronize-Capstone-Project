package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentExploreBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil

class ExploreFragment(private val mainBinding:ActivityMainBinding) : Fragment(), OnRefreshListener, OnNetworkRetryListener {
    private lateinit var allFeedsAdapter: AllFeedsAdapter
    private lateinit var binding:FragmentExploreBinding
    private lateinit var context:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentExploreBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "EXPLORE"
        if (isAdded && isVisible && !isDetached && !isRemoving){
            context = requireContext()
            if (::context.isInitialized){
                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)
                //reset main toolbar
                AppUtil().resetMainToolbar(mainBinding)
                binding.exploreRefreshLayout.setOnRefreshListener(this)
                bindButtons()
                setupRV()
            }
        }

    }

    private fun setupRV() {
        val feedList:ArrayList<PostModel> = ArrayList()
        binding.exploreRefreshLayout.isRefreshing = true
        FirebaseUtil().retrieveAllCommunityCollection()
            .whereEqualTo("communityType", "Public")
            .get().addOnSuccessListener {
            for (document in it.documents){
                val communityModel = document.toObject(CommunityModel::class.java)!!
                if (!AppUtil().isIdOnList(communityModel.bannedUsers, FirebaseUtil().currentUserUid())){
                    FirebaseUtil().retrieveCommunityFeedsCollection(communityModel.communityId).get().addOnSuccessListener {feeds ->
                        for (post in feeds.documents){
                            val postModel = post.toObject(PostModel::class.java)!!
                            feedList.add(postModel)
                        }
                        feedList.shuffle()
                        binding.exploreRV.layoutManager = LinearLayoutManager(context)
                        allFeedsAdapter = AllFeedsAdapter(context, feedList)
                        binding.exploreRV.adapter = allFeedsAdapter
                    }
                }
                binding.exploreRefreshLayout.isRefreshing = false
            }
        }



    }

    private fun bindButtons(){
        mainBinding.searchBtn.visibility = View.VISIBLE
        mainBinding.searchBtn.setOnClickListener {
            val intent = Intent(context, Search::class.java)
            startActivity(intent)
        }
    }

    override fun onRefresh() {
        binding.exploreRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            setupRV()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}