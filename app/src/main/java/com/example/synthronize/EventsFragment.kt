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
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.EventsAdapter
import com.example.synthronize.adapters.MarketAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentEventsBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener, OnItemClickListener {
    private lateinit var binding: FragmentEventsBinding
    private lateinit var context: Context
    private lateinit var eventsAdapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEventsBinding.inflate(layoutInflater)
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

                binding.eventsRefreshLayout.setOnRefreshListener(this)

                setupEventsRV()

                isUserAdmin{isAdmin ->
                    if (isAdmin){
                        binding.createEventsFab.setOnClickListener{
                            val intent = Intent(context, CreateEvent::class.java)
                            intent.putExtra("communityId", communityId)
                            context.startActivity(intent)
                        }

                    }
                }



            }
        }

    }

    private fun isUserAdmin(callback: (Boolean) -> Unit){
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            val model = it.toObject(CommunityModel::class.java)!!
            if (AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(model.communityMembers, "Admin"), FirebaseUtil().currentUserUid())){
                callback(true)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun setupEventsRV() {
        binding.eventsRefreshLayout.isRefreshing = true

        val myQuery: Query = FirebaseUtil().retrieveCommunityEventsCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.eventsRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<EventModel> =
            FirestoreRecyclerOptions.Builder<EventModel>().setQuery(myQuery, EventModel::class.java).build()

        binding.eventsRV.layoutManager = LinearLayoutManager(context)
        eventsAdapter = EventsAdapter(context, options, this)
        binding.eventsRV.adapter = eventsAdapter
        eventsAdapter.startListening()
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        val intent = Intent(context, OtherUserProfile::class.java)
        intent.putExtra("userID", id)
        startActivity(intent)
    }
    override fun onRefresh() {
        Handler().postDelayed({
            setupEventsRV()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}