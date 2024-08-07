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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CompetitionsAdapter
import com.example.synthronize.adapters.FeedsAdapter
import com.example.synthronize.adapters.FilesAdapter
import com.example.synthronize.databinding.FragmentActivitiesBinding
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject

class ActivitiesFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var binding: FragmentActivitiesBinding
    private lateinit var context: Context
    private lateinit var competitionsAdapter: CompetitionsAdapter
    private lateinit var filesAdapter: FilesAdapter
    private var currentTab = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
            if (::context.isInitialized){
                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)
                AppUtil().headToMainActivityIfBanned(context, communityId)

                //bind refresh layout
                binding.activitiesRefreshLayout.setOnRefreshListener(this)

                navigate("competitions")

                binding.competitionBtn.setOnClickListener {
                    navigate("competitions")
                }
                binding.resourcesBtn.setOnClickListener {
                    navigate("resources")
                }
                binding.sharedFilesBtn.setOnClickListener {
                    navigate("shared_files")
                }


            }
        }

    }

    private fun navigate(tab: String) {
        val unselectedColor = ContextCompat.getColor(context, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(context, R.color.light_teal)
        binding.competitionBtn.setTextColor(unselectedColor)
        binding.resourcesBtn.setTextColor(unselectedColor)
        binding.sharedFilesBtn.setTextColor(unselectedColor)
        if (tab == "competitions"){
            setupCompetitionsRV()
            currentTab = "competitions"
            binding.competitionBtn.setTextColor(selectedColor)
            binding.addFab.visibility = View.GONE
            isUserAllowedToPost {isAllowed ->
                if (isAllowed){
                    binding.addFab.visibility = View.VISIBLE
                    binding.addFab.setOnClickListener{
                        var model = CompetitionModel()
                        FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).add(model).addOnSuccessListener {
                            model = CompetitionModel(
                                competitionId = it.id,
                                "Test",
                                "This is how you do the competition",
                                "Xbox 369",
                                FirebaseUtil().currentUserUid(),
                                listOf(),
                                Timestamp.now(),
                                hashMapOf(FirebaseUtil().currentUserUid() to "filename")
                            )
                            FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(model.competitionId).set(model).addOnSuccessListener {
                                Toast.makeText(context, "file uploaded successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }else if (tab == "resources") {
            setupResourcesRV()
            currentTab = "resources"
            binding.resourcesBtn.setTextColor(selectedColor)
            binding.addFab.visibility = View.GONE
            isUserAllowedToPost {isAllowed ->
                if (isAllowed){
                    binding.addFab.visibility = View.VISIBLE
                    binding.addFab.setOnClickListener{
                        val intent = Intent(context, CreateUploadFile::class.java)
                        intent.putExtra("communityId", communityId)
                        intent.putExtra("isSharedFiles", false)
                        startActivity(intent)
                    }
                }
            }
        }else if (tab == "shared_files") {
            setupSharedFilesRV()
            currentTab = "shared_files"
            binding.sharedFilesBtn.setTextColor(selectedColor)
            binding.addFab.visibility = View.VISIBLE
            binding.addFab.setOnClickListener{
                val intent = Intent(context, CreateUploadFile::class.java)
                intent.putExtra("communityId", communityId)
                intent.putExtra("isSharedFiles", true)
                startActivity(intent)
            }
        }
    }

    private fun isUserAllowedToPost(callback: (Boolean) -> Unit){
        var isAllowed = false
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {community ->
                val communityModel = community.toObject(CommunityModel::class.java)!!
                if (userModel.userType == "Teacher"){
                    isAllowed = true
                }
                if (AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"), userModel.userID)){
                    isAllowed = true
                }
                callback(isAllowed)
            }.addOnFailureListener {
                callback(isAllowed)
            }
        }.addOnFailureListener {
            callback(isAllowed)
        }
    }

    private fun setupCompetitionsRV(){
       binding.activitiesRefreshLayout.isRefreshing = true

        val myQuery: Query = FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.activitiesRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<CompetitionModel> =
            FirestoreRecyclerOptions.Builder<CompetitionModel>().setQuery(myQuery, CompetitionModel::class.java).build()

        binding.activitiesRV.layoutManager = LinearLayoutManager(context)
        competitionsAdapter = CompetitionsAdapter(context, options)
        binding.activitiesRV.adapter = competitionsAdapter
        competitionsAdapter.startListening()
    }

    private fun setupResourcesRV(){
        binding.activitiesRefreshLayout.isRefreshing = true

        val myQuery: Query = FirebaseUtil().retrieveCommunityFilesCollection(communityId)
            .whereEqualTo("shareFile", false)
            .orderBy("uploadTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.activitiesRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<FileModel> =
            FirestoreRecyclerOptions.Builder<FileModel>().setQuery(myQuery, FileModel::class.java).build()

        binding.activitiesRV.layoutManager = LinearLayoutManager(context)
        filesAdapter = FilesAdapter(context, options)
        binding.activitiesRV.adapter = filesAdapter
        filesAdapter.startListening()
    }

    private fun setupSharedFilesRV(){
        binding.activitiesRefreshLayout.isRefreshing = true

        val myQuery: Query = FirebaseUtil().retrieveCommunityFilesCollection(communityId)
            .whereEqualTo("shareFile", true)
            .orderBy("uploadTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        myQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.activitiesRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<FileModel> =
            FirestoreRecyclerOptions.Builder<FileModel>().setQuery(myQuery, FileModel::class.java).build()

        binding.activitiesRV.layoutManager = LinearLayoutManager(context)
        filesAdapter = FilesAdapter(context, options)
        binding.activitiesRV.adapter = filesAdapter
        filesAdapter.startListening()
    }

    override fun onRefresh() {
        Handler().postDelayed({
            navigate(currentTab)
        }, 1000)
    }

    override fun retryNetwork() {

    }
}