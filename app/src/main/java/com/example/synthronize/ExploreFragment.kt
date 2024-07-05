package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.ExploreFeedsAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentExploreBinding
import com.example.synthronize.model.PostModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil

class ExploreFragment(private val mainBinding:ActivityMainBinding) : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var exploreFeedsAdapter: ExploreFeedsAdapter
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
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root)

                //reset main toolbar
                AppUtil().resetMainToolbar(mainBinding)

                bindButtons()
                setupRV()
            }
        }

    }

    private fun setupRV() {
        val feedList:ArrayList<PostModel> = ArrayList()
        FirebaseUtil().retrieveAllCommunityCollection().whereEqualTo("communityType", "Public").get().addOnSuccessListener {it ->
            for (document in it.documents){
                val id = document.get("communityId") as String
                FirebaseUtil().retrieveCommunityFeedsCollection(id).get().addOnSuccessListener {feeds ->
                    for (post in feeds.documents){
                        val postModel = post.toObject(PostModel::class.java)!!
                        feedList.add(postModel)
                    }
                    feedList.shuffle()
                    binding.exploreRV.layoutManager = LinearLayoutManager(context)
                    exploreFeedsAdapter = ExploreFeedsAdapter(context, feedList)
                    binding.exploreRV.adapter = exploreFeedsAdapter
                }
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
}