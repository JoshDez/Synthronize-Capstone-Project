package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.adapters.SearchCommunityAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivitySearchBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil

class Search : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivitySearchBinding
    private lateinit var searchUserAdapter: SearchUserAdapter
    private lateinit var searchCommunityAdapter: SearchCommunityAdapter
    private lateinit var searchInCategory: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchInCategory = intent.getStringExtra("searchInCategory").toString()

        binding.searchEdtTxt.requestFocus()

        binding.resultsPostsRV.layoutManager = LinearLayoutManager(this)

        binding.backBtn.setOnClickListener {
            this.finish()
        }

        binding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(searchQuery: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (searchInCategory == "users"){
                    searchUsers(searchQuery.toString())
                } else if (searchInCategory == "communities"){
                    searchCommunities(searchQuery.toString())
                } else {
                    //search in all categories
                    searchUsers(searchQuery.toString())
                    searchCommunities(searchQuery.toString())
                }
            }
        })
    }

    private fun searchUsers(searchQuery: String){
        binding.usersLinearLayout.visibility = View.INVISIBLE
        val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
            .whereGreaterThanOrEqualTo("fullName", searchQuery)

        val options:FirestoreRecyclerOptions<UserModel> =
            FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

        //set up user recycler view
        binding.resultUsersRV.layoutManager = LinearLayoutManager(this)
        searchUserAdapter = SearchUserAdapter(this, options, this)
        binding.resultUsersRV.adapter = searchUserAdapter
        searchUserAdapter.startListening()

        Handler().postDelayed({
            if (searchUserAdapter.getTotalItems() > 0 && searchQuery.isNotEmpty()){
                binding.usersLinearLayout.visibility = View.VISIBLE
            } else {
                binding.usersLinearLayout.visibility = View.GONE
            }
        }, 1000)

    }

    private fun searchCommunities(searchQuery: String){
        binding.communitiesLinearLayout.visibility = View.INVISIBLE
        val myQuery:Query = FirebaseUtil().retrieveAllCommunityCollection()
            .whereGreaterThanOrEqualTo("communityName", searchQuery)

        val options:FirestoreRecyclerOptions<CommunityModel> =
            FirestoreRecyclerOptions.Builder<CommunityModel>().setQuery(myQuery, CommunityModel::class.java).build()

        //set up community recycler view
        binding.resultCommunitiesRV.layoutManager = LinearLayoutManager(this)
        searchCommunityAdapter = SearchCommunityAdapter(this, options)
        binding.resultCommunitiesRV.adapter = searchCommunityAdapter
        searchCommunityAdapter.startListening()

        Handler().postDelayed({
            if (searchCommunityAdapter.getTotalItems() > 0 && searchQuery.isNotEmpty()){
                binding.communitiesLinearLayout.visibility = View.VISIBLE
            } else {
                binding.communitiesLinearLayout.visibility = View.GONE
            }
        }, 1000)
    }

    private fun searchPosts(searchQuery: String){
        //TODO: Search Posts
    }

    override fun onUserClick(userId: String, isChecked:Boolean) {
        val intent = Intent(this, OtherUserProfile::class.java)
        intent.putExtra("userID", userId)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        if (::searchUserAdapter.isInitialized)
            searchUserAdapter.startListening()
        if (::searchCommunityAdapter.isInitialized)
            searchCommunityAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (::searchUserAdapter.isInitialized)
            searchUserAdapter.notifyDataSetChanged()
        if (::searchCommunityAdapter.isInitialized)
            searchCommunityAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::searchUserAdapter.isInitialized)
            searchUserAdapter.stopListening()
        if (::searchCommunityAdapter.isInitialized)
            searchCommunityAdapter.stopListening()
    }



}