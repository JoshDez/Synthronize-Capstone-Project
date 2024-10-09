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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CompetitionsAdapter
import com.example.synthronize.adapters.FeedsAdapter
import com.example.synthronize.adapters.FilesAdapter
import com.example.synthronize.databinding.DialogMenuBinding
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
import com.orhanobut.dialogplus.DialogPlus

class ActivitiesFragment(private val mainBinding: FragmentCommunityBinding, private val menuDialog: DialogPlus, private val menuBinding: DialogMenuBinding, private val communityId:String, private val isUserAdmin:Boolean) : Fragment(), OnRefreshListener, OnNetworkRetryListener {

    private lateinit var binding: FragmentActivitiesBinding
    private lateinit var context: Context
    private lateinit var competitionsAdapter: CompetitionsAdapter
    private lateinit var resourcesAdapter: FilesAdapter
    private lateinit var sharedFilesAdapter: FilesAdapter
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
                        searchActivitiesRV(searchQuery)
                    }
                })
            }
        }

    }

    private fun searchActivitiesRV(searchQuery: String) {
        if (searchQuery.isNotEmpty()){
            when(currentTab){
                "competitions" -> {
                    setupCompetitionsRV(searchQuery)
                }
                "resources" -> {
                    setupResourcesRV(searchQuery)
                }
                "shared_files" -> {
                    setupSharedFilesRV(searchQuery)
                }
            }
        } else {
            navigate(currentTab)
        }
    }


    private fun navigate(tab: String, toRefresh:Boolean = false) {
        val unselectedColor = ContextCompat.getColor(context, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(context, R.color.light_teal)
        binding.competitionBtn.setTextColor(unselectedColor)
        binding.resourcesBtn.setTextColor(unselectedColor)
        binding.sharedFilesBtn.setTextColor(unselectedColor)
        binding.competitionsRV.visibility = View.GONE
        binding.resourcesRV.visibility = View.GONE
        binding.sharedFilesRV.visibility = View.GONE


        if (tab == "competitions"){
            currentTab = "competitions"
            binding.competitionBtn.setTextColor(selectedColor)
            binding.addFab.visibility = View.GONE
            binding.competitionsRV.visibility = View.VISIBLE

            if (toRefresh || !::competitionsAdapter.isInitialized)
                setupCompetitionsRV()

            isUserAllowedToPost {isAllowed ->
                if (isAllowed){
                    binding.addFab.visibility = View.VISIBLE
                    binding.addFab.setOnClickListener{
                        val intent = Intent(context, CreateCompetition::class.java)
                        intent.putExtra("communityId", communityId)
                        context.startActivity(intent)
                    }
                }
            }
        }else if (tab == "resources") {
            currentTab = "resources"
            binding.resourcesBtn.setTextColor(selectedColor)
            binding.addFab.visibility = View.GONE
            binding.resourcesRV.visibility = View.VISIBLE

            if (toRefresh || !::resourcesAdapter.isInitialized)
                setupResourcesRV()


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
            currentTab = "shared_files"
            binding.sharedFilesBtn.setTextColor(selectedColor)
            binding.addFab.visibility = View.VISIBLE
            binding.sharedFilesRV.visibility = View.VISIBLE

            if (toRefresh || !::sharedFilesAdapter.isInitialized)
                setupSharedFilesRV()

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

    private fun setupCompetitionsRV(searchQuery: String = ""){
       binding.activitiesRefreshLayout.isRefreshing = true

        var myQuery:Query

        if (searchQuery.isNotEmpty()){
            myQuery = FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId)
                .whereGreaterThanOrEqualTo("competitionName", searchQuery)
                .whereLessThanOrEqualTo("competitionName", searchQuery+"\uf8ff")
        } else {
            myQuery = FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)
        }

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

        binding.competitionsRV.layoutManager = LinearLayoutManager(context)
        competitionsAdapter = CompetitionsAdapter(context, options, isUserAdmin)
        binding.competitionsRV.adapter = competitionsAdapter
        competitionsAdapter.startListening()
    }

    private fun setupResourcesRV(searchQuery: String = ""){
        binding.activitiesRefreshLayout.isRefreshing = true

        var myQuery: Query

        if (searchQuery.isNotEmpty()){
            myQuery = FirebaseUtil().retrieveCommunityFilesCollection(communityId)
                .whereGreaterThanOrEqualTo("caption", searchQuery)
                .whereLessThanOrEqualTo("caption", searchQuery+"\uf8ff")
        } else {
            myQuery = FirebaseUtil().retrieveCommunityFilesCollection(communityId)
                .whereEqualTo("forCompetition", false)
                .whereEqualTo("shareFile", false)
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)
        }

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

        binding.resourcesRV.layoutManager = LinearLayoutManager(context)
        resourcesAdapter = FilesAdapter(context, options)
        binding.resourcesRV.adapter = resourcesAdapter
        resourcesAdapter.startListening()
    }

    private fun setupSharedFilesRV(searchQuery: String = ""){
        binding.activitiesRefreshLayout.isRefreshing = true
        var myQuery: Query

        if (searchQuery.isNotEmpty()){
            myQuery =  FirebaseUtil().retrieveCommunityFilesCollection(communityId)
                .whereGreaterThanOrEqualTo("caption", searchQuery)
                .whereLessThanOrEqualTo("caption", searchQuery+"\uf8ff")
        } else {
            myQuery = FirebaseUtil().retrieveCommunityFilesCollection(communityId)
                .whereEqualTo("forCompetition", false)
                .whereEqualTo("shareFile", true)
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)
        }

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

        binding.sharedFilesRV.layoutManager = LinearLayoutManager(context)
        sharedFilesAdapter = FilesAdapter(context, options)
        binding.sharedFilesRV.adapter = sharedFilesAdapter
        sharedFilesAdapter.startListening()
    }


    override fun onStart() {
        super.onStart()
        if (::resourcesAdapter.isInitialized){
            resourcesAdapter.startListening()
        }
        if (::sharedFilesAdapter.isInitialized){
            sharedFilesAdapter.startListening()
        }
        if (::competitionsAdapter.isInitialized){
            competitionsAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::resourcesAdapter.isInitialized){
            resourcesAdapter.notifyDataSetChanged()
        }
        if (::sharedFilesAdapter.isInitialized){
            sharedFilesAdapter.notifyDataSetChanged()
        }
        if (::competitionsAdapter.isInitialized){
            competitionsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::resourcesAdapter.isInitialized){
            resourcesAdapter.stopListening()
        }
        if (::sharedFilesAdapter.isInitialized){
            sharedFilesAdapter.stopListening()
        }
        if (::competitionsAdapter.isInitialized){
            competitionsAdapter.stopListening()
        }
    }

    override fun onRefresh() {
        Handler().postDelayed({
            binding.searchEdtTxt.setText("")
            binding.searchContainerLL.visibility = View.GONE
            navigate(currentTab, true)
        }, 1000)
    }

    override fun retryNetwork() {
       onRefresh()
    }
}