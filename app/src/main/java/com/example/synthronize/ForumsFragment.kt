package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.ForumsAdapter
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentForumsBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.ForumModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

class ForumsFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var binding:FragmentForumsBinding
    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var forumsAdapter: ForumsAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
            binding.forumsRefreshLayout.isRefreshing = true
            if (context != null){
                binding.forumsRefreshLayout.setOnRefreshListener(this)
                NetworkUtil(context).checkNetworkAndShowSnackbar(binding.root, this)
                AppUtil().headToMainActivityIfBanned(context, communityId)
                bindButtons()
                setRecyclerView()
            }
        }
    }

    private fun setRecyclerView() {
        binding.forumsRefreshLayout.isRefreshing = true

        val myQuery:Query = FirebaseUtil().retrieveCommunityForumsCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.forumsRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<ForumModel> =
            FirestoreRecyclerOptions.Builder<ForumModel>().setQuery(myQuery, ForumModel::class.java).build()

        recyclerView = binding.threadsRV
        recyclerView.layoutManager = LinearLayoutManager(context)
        forumsAdapter = ForumsAdapter(mainBinding, context, options)
        recyclerView.adapter = forumsAdapter
        forumsAdapter.startListening()
    }

    private fun bindButtons(){
        binding.addThreadFab.setOnClickListener{
            val intent = Intent(context, CreateThread::class.java)
            intent.putExtra("communityId", communityId)
            context.startActivity(intent)
        }
    }
    override fun onStart() {
        super.onStart()
        if (::forumsAdapter.isInitialized){
            forumsAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::forumsAdapter.isInitialized){
            forumsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::forumsAdapter.isInitialized){
            forumsAdapter.stopListening()
        }
    }


    override fun onRefresh() {
        binding.forumsRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            setRecyclerView()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}
