package com.example.synthronize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.AllFeedsAdapter
import com.example.synthronize.adapters.SearchCommunityAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivitySearchBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil

class Search : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivitySearchBinding
    private lateinit var searchUserAdapter: SearchUserAdapter
    private lateinit var searchCommunityAdapter: SearchCommunityAdapter
    private lateinit var searchFeedAdapter: AllFeedsAdapter
    private lateinit var searchInCategory: String
    private var userLayoutOpen = true
    private var communityLayoutOpen = true
    private var postLayoutOpen = true

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

            override fun afterTextChanged(p0: Editable?) {
                val searchQuery = binding.searchEdtTxt.text.toString()

                if (searchInCategory == "users"){
                    searchUsers(searchQuery)
                } else if (searchInCategory == "communities"){
                    searchCommunities(searchQuery)
                } else if(searchInCategory == "feeds"){
                    searchPosts(searchQuery)
                } else {
                    //search in all categories
                    searchUsers(searchQuery)
                    searchCommunities(searchQuery)
                    searchPosts(searchQuery)
                }
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(searchQuery: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun searchUsers(searchQuery: String){
        binding.usersLinearLayout.visibility = View.INVISIBLE

        var myQuery: Query = FirebaseUtil().allUsersCollectionReference()

        if (searchQuery.isNotEmpty()){
            if (searchQuery[0] == '@'){
                //search user by username
                myQuery = FirebaseUtil().allUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("username", searchQuery.removePrefix("@"))
                    .whereLessThanOrEqualTo("username", searchQuery.removePrefix("@")+"\uf8ff")
            } else {
                //search user by full name
                myQuery = FirebaseUtil().allUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)
                    .whereLessThanOrEqualTo("fullName", searchQuery+"\uf8ff")
            }
        }

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
                binding.userHeaderLayout.setOnClickListener {
                    if (userLayoutOpen){
                        userLayoutOpen = false
                        binding.resultUsersRV.visibility = View.GONE
                        binding.userArrowIV.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
                    } else {
                        userLayoutOpen = true
                        binding.resultUsersRV.visibility = View.VISIBLE
                        binding.userArrowIV.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
                    }
                }
            } else {
                binding.usersLinearLayout.visibility = View.GONE
            }
        }, 1000)
    }

    private fun searchCommunities(searchQuery: String){
        binding.communitiesLinearLayout.visibility = View.INVISIBLE

        val myQuery:Query = FirebaseUtil().retrieveAllCommunityCollection()
            .whereGreaterThanOrEqualTo("communityName", searchQuery)
            .whereLessThanOrEqualTo("communityName", searchQuery+"\uf8ff")

        val options:FirestoreRecyclerOptions<CommunityModel> =
            FirestoreRecyclerOptions.Builder<CommunityModel>().setQuery(myQuery, CommunityModel::class.java).build()

        //set up community recycler view
        binding.resultCommunitiesRV.layoutManager = LinearLayoutManager(this)
        searchCommunityAdapter = SearchCommunityAdapter(this, options, this)
        binding.resultCommunitiesRV.adapter = searchCommunityAdapter
        searchCommunityAdapter.startListening()

        Handler().postDelayed({
            if (searchCommunityAdapter.getTotalItems() > 0 && searchQuery.isNotEmpty()){
                binding.communitiesLinearLayout.visibility = View.VISIBLE
                binding.communityHeaderLayout.setOnClickListener {
                    if (communityLayoutOpen){
                        communityLayoutOpen = false
                        binding.resultCommunitiesRV.visibility = View.GONE
                        binding.communityArrowIV.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
                    } else {
                        communityLayoutOpen = true
                        binding.resultCommunitiesRV.visibility = View.VISIBLE
                        binding.communityArrowIV.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
                    }
                }
            } else {
                binding.communitiesLinearLayout.visibility = View.GONE
            }
        }, 1000)
    }

    private fun searchPosts(searchQuery: String){
        binding.postsLinearLayout.visibility = View.INVISIBLE
        val feedList:ArrayList<PostModel> = ArrayList()
        FirebaseUtil().retrieveAllCommunityCollection().whereEqualTo("communityType", "Public").get().addOnSuccessListener {it ->
            for (document in it.documents){
                val id = document.get("communityId") as String

                FirebaseUtil().retrieveCommunityFeedsCollection(id)
                    .whereGreaterThanOrEqualTo("caption", searchQuery)
                    .whereLessThanOrEqualTo("caption", searchQuery+"\uf8ff").get().addOnSuccessListener {feeds ->
                    for (post in feeds.documents){
                        val postModel = post.toObject(PostModel::class.java)!!
                        feedList.add(postModel)
                    }
                    binding.resultsPostsRV.layoutManager = LinearLayoutManager(this)
                    searchFeedAdapter = AllFeedsAdapter(this, feedList, isExploreTab = false, removeBackground = true)
                    binding.resultsPostsRV.adapter = searchFeedAdapter
                    Handler().postDelayed({
                        if (searchFeedAdapter.itemCount > 0 && searchQuery.isNotEmpty()){
                            binding.postsLinearLayout.visibility = View.VISIBLE
                            binding.postsHeaderLayout.setOnClickListener {
                                if (postLayoutOpen){
                                    postLayoutOpen = false
                                    binding.resultsPostsRV.visibility = View.GONE
                                    binding.postsArrowIV.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
                                } else {
                                    postLayoutOpen = true
                                    binding.resultsPostsRV.visibility = View.VISIBLE
                                    binding.postsArrowIV.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
                                }
                            }
                        } else {
                            binding.postsLinearLayout.visibility = View.GONE
                        }
                    }, 2000)
                }
            }
        }
    }

    override fun onItemClick(id: String, isChecked:Boolean) {
        val intent = Intent(this, OtherUserProfile::class.java)
        intent.putExtra("userID", id)
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
        if (::searchFeedAdapter.isInitialized)
            searchFeedAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::searchUserAdapter.isInitialized)
            searchUserAdapter.stopListening()
        if (::searchCommunityAdapter.isInitialized)
            searchCommunityAdapter.stopListening()
    }



}