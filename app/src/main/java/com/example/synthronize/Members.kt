package com.example.synthronize

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityMembersBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

class Members : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding:ActivityMembersBinding
    private lateinit var membersAdapter: SearchUserAdapter
    private lateinit var adminAdapter: SearchUserAdapter
    private lateinit var communityId:String
    private lateinit var chatroomId:String
    private lateinit var communityModel: CommunityModel
    private lateinit var chatroomModel: ChatroomModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        chatroomId = intent.getStringExtra("chatroomId").toString()

        queryUsers()

        binding.searchEdtTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchQuery = binding.searchEdtTxt.text.toString()
                queryUsers(searchQuery)
            }
        })



    }

    override fun onUserClick(userId: String, isChecked: Boolean) {
        TODO("Not yet implemented")
    }

    private fun queryUsers(searchQuery:String = ""){
        binding.membersLayout.visibility = View.INVISIBLE
        binding.adminLayout.visibility = View.INVISIBLE

        if (communityId.isNotEmpty()){
            //For Community Members
            FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
                communityModel = it.toObject(CommunityModel::class.java)!!

                //Removes admins from members
                val filteredMembersList:ArrayList<String> = ArrayList()
                for (user in communityModel.communityMembers){
                    for (admin in communityModel.communityAdmin){
                        if (user != admin){
                            filteredMembersList.add(user)
                        }
                    }
                }

                //FOR MEMBERS
                val membersQuery:Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", filteredMembersList)
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val membersOptions:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(membersQuery, UserModel::class.java).build()


                //FOR ADMINS
                val adminsQuery:Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", communityModel.communityAdmin)
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val adminsOptions:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(adminsQuery, UserModel::class.java).build()

                //Setup Recyclers
                setupMembersRV(membersOptions)
                setupAdminRV(adminsOptions)

            }
        } else {
            //For Chatroom Members
        }
    }

    private fun setupMembersRV(options:FirestoreRecyclerOptions<UserModel>){
        binding.membersLayout.visibility = View.VISIBLE
        membersAdapter = SearchUserAdapter(this, options, this)
        binding.membersRV.layoutManager = LinearLayoutManager(this)
        binding.membersRV.adapter = membersAdapter
        membersAdapter.startListening()
    }

    private fun setupAdminRV(options:FirestoreRecyclerOptions<UserModel>){
        binding.adminLayout.visibility = View.VISIBLE
        adminAdapter = SearchUserAdapter(this, options, this)
        binding.adminRV.layoutManager = LinearLayoutManager(this)
        binding.adminRV.adapter = adminAdapter
        adminAdapter.startListening()
    }


}