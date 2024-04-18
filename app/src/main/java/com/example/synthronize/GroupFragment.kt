// GroupFragment.kt

package com.example.synthronize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.synthronize.databinding.FragmentGroupBinding
import com.google.firebase.firestore.FirebaseFirestore

class GroupFragment : Fragment() {

    private lateinit var binding: FragmentGroupBinding
    private lateinit var groupId: String
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

        // Retrieve the groupId from arguments
        groupId = arguments?.getString("groupId") ?: ""

        // Load default news feed for the selected group
        loadNewsFeed()
    }

    private fun loadNewsFeed() {
        // Query the Firestore collection for news feeds of the selected group
        val feedsCollection = db.collection("groups").document(groupId).collection("feeds")

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
