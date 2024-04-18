package com.example.synthronize

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivitySearchBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil

class Search : AppCompatActivity() {
    private lateinit var binding:ActivitySearchBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchUserAdapter: SearchUserAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.searchEdtTxt.requestFocus()

        recyclerView = binding.resultUsersRV
        recyclerView.layoutManager = LinearLayoutManager(this)

        binding.backBtn.setOnClickListener {
            this.finish()
        }

        binding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(searchQuery: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchUsers(searchQuery.toString())
            }
        })
    }

    private fun searchUsers(searchQuery: String){
        binding.usersLinearLayout.visibility = View.INVISIBLE
        val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
            .whereGreaterThanOrEqualTo("fullName", searchQuery)

        val options:FirestoreRecyclerOptions<UserModel> =
            FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

        searchUserAdapter = SearchUserAdapter(this, options)
        recyclerView.adapter = searchUserAdapter
        searchUserAdapter.startListening()

        Handler().postDelayed({
            if (searchUserAdapter.totalItems > 0 && searchQuery.isNotEmpty()){
                binding.usersLinearLayout.visibility = View.VISIBLE
            } else {
                binding.usersLinearLayout.visibility = View.GONE
                searchUserAdapter.stopListening()
            }
        }, 1000)

    }



}