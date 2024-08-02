package com.example.synthronize

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.databinding.FragmentActivitiesBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.NetworkUtil

class ActivitiesFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var binding: FragmentActivitiesBinding
    private lateinit var context: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
            if (context != null){
                NetworkUtil(context).checkNetworkAndShowSnackbar(binding.root, this)
                AppUtil().headToMainActivityIfBanned(context, communityId)
            }
        }

    }

    override fun onRefresh() {

    }

    override fun retryNetwork() {

    }
}