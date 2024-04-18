package com.example.synthronize

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.GroupAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentGroupSelectionBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.FirebaseUtil

class CommunitySelectionFragment(private val mainBinding: ActivityMainBinding) : Fragment() {
    private lateinit var binding: FragmentGroupSelectionBinding
    private lateinit var groupAdapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGroupSelectionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "COMMUNITIES"

        //If the fragment is added
        if (isAdded){
            //TODO: ADDED
            // Initialize RecyclerView and adapter
            groupAdapter = GroupAdapter(requireContext())
            binding.groupSelectionRV.apply {
                adapter = groupAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            // Set up Add Group FAB
            binding.addGroupFab.setOnClickListener {
                val dialogFragment = CreateGroupDialogFragment()
                dialogFragment.show(childFragmentManager, "CreateGroupDialogFragment")
            }

            // Fetch groups from Firestore
            fetchCommunitiesFromFirestore()
        }
    }

    private fun fetchCommunitiesFromFirestore() {
        FirebaseUtil().retrieveAllCommunityCollection().get()
            .addOnSuccessListener { documents ->
                val communityList = mutableListOf<CommunityModel>()
                for (document in documents) {
                    val communityModel = document.toObject(CommunityModel::class.java)
                    communityList.add(communityModel)
                }
                groupAdapter.setData(communityList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to fetch groups: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}