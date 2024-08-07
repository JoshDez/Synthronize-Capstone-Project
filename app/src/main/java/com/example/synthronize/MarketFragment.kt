package com.example.synthronize

import android.content.Context
import android.os.Bundle
import android.os.Handler
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

class MarketFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

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
                    marketAdapter.stopListening()
                    var productModel = ProductModel()
                    FirebaseUtil().retrieveCommunityMarketCollection(communityId).add(productModel).addOnSuccessListener {
                        productModel = ProductModel(
                            it.id,
                            "Xbox 360",
                            "The product is 10 years old but still working perfectly",
                            30000,
                            listOf(),
                            FirebaseUtil().currentUserUid(),
                            Timestamp.now(),
                        )
                        FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(productModel.productId).set(productModel).addOnSuccessListener {
                            Toast.makeText(context, "Product is successfully uploaded", Toast.LENGTH_SHORT).show()
                            onRefresh()
                        }
                    }
                }
            }
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

    override fun onRefresh() {
        Handler().postDelayed({
            setupMarketRV()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}