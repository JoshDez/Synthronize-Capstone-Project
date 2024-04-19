// GroupFragment.kt

package com.example.synthronize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentGroupBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FirebaseFirestore

class GroupFragment(private val mainBinding: ActivityMainBinding, private val groupID:String) : Fragment() {

    private lateinit var binding: FragmentGroupBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){

            //Retrieve Group Model
            FirebaseUtil().retrieveCommunityDocument(groupID).get().addOnCompleteListener {
                if (it.isSuccessful && it.result.exists()) {
                    val communityModel = it.result.toObject(CommunityModel::class.java)!!
                    mainBinding.toolbarTitleTV.text = communityModel.communityName
                    // Load default news feed for the selected group
                    loadNewsFeed()
                }
            }
        }

    }

    private fun loadNewsFeed() {
        // Query the Firestore collection for news feeds of the selected group
        val feedsCollection = db.collection("groups").document(groupID).collection("feeds")

        // TODO: Implement code to load and display news feeds from Firestore
        // Example:
        feedsCollection.get()
            .addOnSuccessListener { documents ->
                // Process and display news feeds
            }
            .addOnFailureListener { exception ->
                // Handle failure
            }
    }
}
