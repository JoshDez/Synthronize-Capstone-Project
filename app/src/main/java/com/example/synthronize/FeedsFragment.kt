package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.FeedsAdapter
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentFeedsBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.PostModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus

class FeedsFragment(private val mainBinding: FragmentCommunityBinding, private val menuDialog:DialogPlus, private val menuBinding: DialogMenuBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var binding: FragmentFeedsBinding
    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var feedsAdapter: FeedsAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFeedsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
            binding.feedsRefreshLayout.isRefreshing = true
            if (context != null){
                binding.feedsRefreshLayout.setOnRefreshListener(this)
                NetworkUtil(context).checkNetworkAndShowSnackbar(binding.root, this)
                AppUtil().headToMainActivityIfBanned(context, communityId)
                bindButtons()
                setRecyclerView()
            }
        }
    }

    private fun setRecyclerView() {

        binding.feedsRefreshLayout.isRefreshing = true

        val myQuery:Query = FirebaseUtil().retrieveCommunityFeedsCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.feedsRefreshLayout.isRefreshing = false
            }
        }


        //set options for firebase ui
        val options: FirestoreRecyclerOptions<PostModel> =
            FirestoreRecyclerOptions.Builder<PostModel>().setQuery(myQuery, PostModel::class.java).build()

        recyclerView = binding.feedsRV
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedsAdapter = FeedsAdapter(mainBinding, context, options)
        recyclerView.adapter = feedsAdapter
        feedsAdapter.startListening()
    }

    private fun bindButtons(){
        binding.addPostFab.setOnClickListener{
            val intent = Intent(context, CreatePost::class.java)
            intent.putExtra("communityId", communityId)
            context.startActivity(intent)
        }
        //Search button from community fragment
        menuBinding.optiontitle1.setOnClickListener {
            menuDialog.dismiss()
            binding.searchContainerLL.visibility = View.VISIBLE
        }
        binding.cancelBtn.setOnClickListener {
            binding.searchEdtTxt.setText("")
            binding.searchContainerLL.visibility = View.GONE
        }
        binding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchQuery = binding.searchEdtTxt.text.toString()
                searchFeedsRV(searchQuery)
            }
        })
    }
    private fun searchFeedsRV(searchQuery:String){
        if (searchQuery.isNotEmpty()){
            binding.feedsRefreshLayout.isRefreshing = true

            val myQuery:Query = FirebaseUtil().retrieveCommunityFeedsCollection(communityId)
                .whereGreaterThanOrEqualTo("caption", searchQuery)
                .whereLessThanOrEqualTo("caption", searchQuery+"\uf8ff")

            // Add a listener to handle success or failure of the query
            myQuery.addSnapshotListener { _, e ->
                if (e != null) {
                    // Handle the error here (e.g., log the error or show a message to the user)
                    Log.e("Firestore Error", "Error while fetching data", e)
                    return@addSnapshotListener
                } else {
                    binding.feedsRefreshLayout.isRefreshing = false
                }
            }
            //set options for firebase ui
            val options: FirestoreRecyclerOptions<PostModel> =
                FirestoreRecyclerOptions.Builder<PostModel>().setQuery(myQuery, PostModel::class.java).build()

            recyclerView = binding.feedsRV
            recyclerView.layoutManager = LinearLayoutManager(context)
            feedsAdapter = FeedsAdapter(mainBinding, context, options)
            recyclerView.adapter = feedsAdapter
            feedsAdapter.startListening()
        } else {
            setRecyclerView()
        }
    }
    override fun onStart() {
        super.onStart()
        if (::feedsAdapter.isInitialized){
            feedsAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::feedsAdapter.isInitialized){
            feedsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::feedsAdapter.isInitialized){
            feedsAdapter.stopListening()
        }
    }

    override fun onRefresh() {
        binding.feedsRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            binding.searchEdtTxt.setText("")
            binding.searchContainerLL.visibility = View.GONE
            setRecyclerView()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}
