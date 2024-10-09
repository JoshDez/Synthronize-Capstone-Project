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
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CompetitionsAdapter
import com.example.synthronize.adapters.MarketAdapter
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentMarketBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus

class MarketFragment(private val mainBinding: FragmentCommunityBinding, private val menuDialog: DialogPlus, private val menuBinding: DialogMenuBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var binding: FragmentMarketBinding
    private lateinit var marketAdapter: MarketAdapter
    private lateinit var context: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMarketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
            if (::context.isInitialized){
                NetworkUtil(context).checkNetworkAndShowSnackbar(binding.root, this)
                AppUtil().headToMainActivityIfBanned(context, communityId)
                binding.marketRefreshLayout.setOnRefreshListener(this)
                setupMarketRV()
                binding.addFab.setOnClickListener {
                    val intent = Intent(context, CreateProduct::class.java)
                    intent.putExtra("communityId", communityId)
                    startActivity(intent)
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
                        searchMarketRV(searchQuery)
                    }
                })
            }
        }
    }

    private fun searchMarketRV(searchQuery: String) {
        if (searchQuery.isNotEmpty()){
            binding.marketRefreshLayout.isRefreshing = true

            val myQuery: Query = FirebaseUtil().retrieveCommunityMarketCollection(communityId)
                .whereGreaterThanOrEqualTo("productName", searchQuery)
                .whereLessThanOrEqualTo("productName", searchQuery+"\uf8ff")

            // Add a listener to handle success or failure of the query
            myQuery.addSnapshotListener { _, e ->
                if (e != null) {
                    // Handle the error here (e.g., log the error or show a message to the user)
                    Log.e("Firestore Error", "Error while fetching data", e)
                    return@addSnapshotListener
                } else {
                    binding.marketRefreshLayout.isRefreshing = false
                }
            }

            //set options for firebase ui
            val options: FirestoreRecyclerOptions<ProductModel> =
                FirestoreRecyclerOptions.Builder<ProductModel>().setQuery(myQuery, ProductModel::class.java).build()

            binding.marketRV.layoutManager = LinearLayoutManager(context)
            marketAdapter = MarketAdapter(context, options)
            binding.marketRV.adapter = marketAdapter
            marketAdapter.startListening()
        } else {
            setupMarketRV()
        }
    }

    private fun setupMarketRV() {
        binding.marketRefreshLayout.isRefreshing = true

        val myQuery: Query = FirebaseUtil().retrieveCommunityMarketCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.marketRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<ProductModel> =
            FirestoreRecyclerOptions.Builder<ProductModel>().setQuery(myQuery, ProductModel::class.java).build()

        binding.marketRV.layoutManager = LinearLayoutManager(context)
        marketAdapter = MarketAdapter(context, options)
        binding.marketRV.adapter = marketAdapter
        marketAdapter.startListening()
    }
    override fun onStart() {
        super.onStart()
        if (::marketAdapter.isInitialized){
            marketAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::marketAdapter.isInitialized){
            marketAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::marketAdapter.isInitialized){
            marketAdapter.stopListening()
        }
    }

    override fun onRefresh() {
        Handler().postDelayed({
            binding.searchEdtTxt.setText("")
            binding.searchContainerLL.visibility = View.GONE
            setupMarketRV()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}