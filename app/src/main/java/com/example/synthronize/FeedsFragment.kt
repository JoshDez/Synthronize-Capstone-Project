package com.example.synthronize

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.adapters.FeedsAdapter
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentFeedsBinding
import com.example.synthronize.model.FeedsModel
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

class FeedsFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment() {

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

            if (context != null){
                bindButtons()
                setRecyclerView()
            }

        }

    }

    private fun setRecyclerView() {
        val feedsQuery:Query = FirebaseUtil().retrieveCommunityFeedsCollection(communityId)
            .orderBy("feedTimestamp", Query.Direction.DESCENDING)

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<FeedsModel> =
            FirestoreRecyclerOptions.Builder<FeedsModel>().setQuery(feedsQuery, FeedsModel::class.java).build()

        recyclerView = binding.feedsRV
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedsAdapter = FeedsAdapter(mainBinding, context, options)
        recyclerView.adapter = feedsAdapter
        feedsAdapter.startListening()
    }

    private fun bindButtons(){
        binding.addPostFab.setOnClickListener{
            addTest()
        }
    }

    private fun addTest(){

        val feedsModel = FeedsModel(
            feedOwnerId = FirebaseUtil().currentUserUid(),
            feedImages = listOf(FirebaseUtil().currentUserUid()),
            feedCaption = "This is my profile",
            feedTimestamp = Timestamp.now(),
            communityIdOfOrigin = communityId,
        )

        FirebaseUtil().retrieveCommunityFeedsCollection(communityId).add(feedsModel).addOnSuccessListener {
            //get new id from firestore and store it in feedId of the feedsModel
            val feedModelWithId = feedsModel.copy(feedId = it.id)
            FirebaseUtil().retrieveCommunityFeedsCollection(communityId).document(it.id).set(feedModelWithId).addOnSuccessListener {
                Toast.makeText(context, "Your post is uploaded successfully!", Toast.LENGTH_SHORT).show()
            }
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
}