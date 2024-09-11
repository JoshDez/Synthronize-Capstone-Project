package com.example.synthronize

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityBanAndBlockListBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class BanAndBlockList : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener, OnItemClickListener {
    private lateinit var binding:ActivityBanAndBlockListBinding
    private lateinit var searchUserAdapter:SearchUserAdapter
    private var communityId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBanAndBlockListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.banAndBlockRefreshLayout.setOnRefreshListener(this)

        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        communityId = intent.getStringExtra("communityId").toString()

        if (communityId == "null" || communityId.isEmpty()){
            //Blocked users
            binding.toolbarTitleTV.text = "Blocked Users"
            setupBlockedUsersRV()
        } else {
            //Banned users from community
            binding.toolbarTitleTV.text = "Banned Users"
            setupBannedMembersRV()
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupBlockedUsersRV() {
        binding.banAndBlockRefreshLayout.isRefreshing = true

        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            //query users in blocklist
            if (userModel.blockList.isNotEmpty()){
                val query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", userModel.blockList)

                val options:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(query, UserModel::class.java).build()

                binding.banAndBlockRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(this, options, this, "BlockedUsers")
                binding.banAndBlockRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            }
            binding.banAndBlockRefreshLayout.isRefreshing = false
        }.addOnFailureListener {
            binding.banAndBlockRefreshLayout.isRefreshing = false
        }
    }

    private fun setupBannedMembersRV() {
        binding.banAndBlockRefreshLayout.isRefreshing = true

        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            //query users in blocklist
            if (communityModel.bannedUsers.isNotEmpty()){
                val query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", communityModel.bannedUsers)

                val options:FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(query, UserModel::class.java).build()

                binding.banAndBlockRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(this, options, this, "BannedUsers")
                binding.banAndBlockRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            }
            binding.banAndBlockRefreshLayout.isRefreshing = false
        }.addOnFailureListener {
            binding.banAndBlockRefreshLayout.isRefreshing = false
        }

        binding.banAndBlockRefreshLayout.isRefreshing = false
    }

    override fun onRefresh() {
        Handler().postDelayed({
            if (communityId == "null" || communityId.isEmpty()){
                //Blocked users
                setupBlockedUsersRV()
            } else {
                //Banned users from community
                setupBannedMembersRV()
            }
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        if (communityId == "null" || communityId.isEmpty()){
            //Blocked Users
            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val warningDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(warningBinding.root))
                .setGravity(Gravity.CENTER)
                .setBackgroundColorResId(R.color.transparent)
                .create()

            warningBinding.messageTV.text = "Do you want to unblock this user?"
            warningBinding.titleTV.text = "Unblock User?"

            warningBinding.yesBtn.setOnClickListener {
                //removes user from the block list
                FirebaseUtil().currentUserDetails().update("blockList", FieldValue.arrayRemove(id)).addOnSuccessListener {
                    warningDialog.dismiss()
                    onRefresh()
                    Toast.makeText(this, "The user has been unblocked", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                }

            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
            }
            warningDialog.show()

        } else {
            //Banned members
            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val warningDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(warningBinding.root))
                .setGravity(Gravity.CENTER)
                .setBackgroundColorResId(R.color.transparent)
                .create()

            warningBinding.messageTV.text = "Do you want to unban this user?"
            warningBinding.titleTV.text = "Unban User?"

            warningBinding.yesBtn.setOnClickListener {
                //removes user from the bannedUsers list of community
                FirebaseUtil().retrieveCommunityDocument(communityId).update("bannedUsers", FieldValue.arrayRemove(id)).addOnSuccessListener {
                    warningDialog.dismiss()
                    onRefresh()
                    Toast.makeText(this, "The user has been unbanned", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                }

            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
            }
            warningDialog.show()
        }
    }
}