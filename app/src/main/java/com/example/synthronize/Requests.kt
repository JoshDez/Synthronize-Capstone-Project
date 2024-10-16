package com.example.synthronize

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityRequestsBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

class Requests : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityRequestsBinding
    private lateinit var userAdapter: SearchUserAdapter
    private lateinit var communityId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarTitleTV.text = "Join Requests"

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        communityId = intent.getStringExtra("communityId").toString()

        setupRecycleView()
    }

    private fun setupRecycleView() {
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            //gets community model
            val communityModel = it.toObject(CommunityModel::class.java)!!

            //query users
            if (communityModel.joinRequestList.isNotEmpty()){
                val query:Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", communityModel.joinRequestList)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(query, UserModel::class.java).build()

                //setup recyclerview
                binding.requestsRV.layoutManager = LinearLayoutManager(this)
                userAdapter = SearchUserAdapter(this, options, this, "PermitUser")
                binding.requestsRV.adapter = userAdapter
                userAdapter.startListening()
            } else {
                if (::userAdapter.isInitialized)
                    binding.requestsRV.adapter = null
            }
        }

    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        //Accepts or Rejects user
        if (isChecked){
            val updatedMap = hashMapOf<String, Any>(
                "communityMembers.$id" to "Member"
            )
            //accepts user
            FirebaseUtil().retrieveCommunityDocument(communityId).update(updatedMap).addOnSuccessListener {
                FirebaseUtil().retrieveCommunityDocument(communityId).update("joinRequestList", FieldValue.arrayRemove(id)).addOnSuccessListener {
                    Toast.makeText(this, "User Accepted", Toast.LENGTH_SHORT).show()

                    setupRecycleView()
                }
            }
        } else {
            //rejects user
            FirebaseUtil().retrieveCommunityDocument(communityId).update("joinRequestList", FieldValue.arrayRemove(id)).addOnSuccessListener {
                Toast.makeText(this, "User Rejected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::userAdapter.isInitialized)
            userAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (::userAdapter.isInitialized)
            userAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        if (::userAdapter.isInitialized)
            userAdapter.stopListening()
    }


}