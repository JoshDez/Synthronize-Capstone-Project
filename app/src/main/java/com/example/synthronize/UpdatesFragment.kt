package com.example.synthronize

import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.RequestsAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentUpdatesBinding
import com.example.synthronize.interfaces.NotificationOnDataChange
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil

class UpdatesFragment(private val mainBinding: ActivityMainBinding): Fragment(), NotificationOnDataChange, OnRefreshListener, OnNetworkRetryListener {
    // TODO: Rename and change types of parameters
    private lateinit var binding: FragmentUpdatesBinding
    private lateinit var requestsAdapter: RequestsAdapter
    private lateinit var context: Context
    private var currentTab = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUpdatesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "UPDATES"

        if (isAdded){
            context = requireContext()
            if (::context.isInitialized){

                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)

                //bind refresh layout
                binding.notificationsRefreshLayout.setOnRefreshListener(this)

                //reset main toolbar
                AppUtil().resetMainToolbar(mainBinding)

                bindButtons()
                navigate("notifications")

                binding.notificationsBtn.setOnClickListener {
                    navigate("notifications")
                }
                binding.communityInvitationsBtn.setOnClickListener {
                    navigate("community_invitations")
                }
                binding.friendRequestsBtn.setOnClickListener {
                    navigate("friend_requests")
                }
            }
        }
    }

    private fun navigate(tab:String){
        binding.notificationsIconIV.setImageResource(R.drawable.notifications_not_selected)
        binding.communityInviteIconIV.setImageResource(R.drawable.community_invitations_not_selected)
        binding.friendRequestIconIV.setImageResource(R.drawable.friend_requests_not_selected)


        if (tab == "notifications"){
            setupNotifications()
            binding.notificationsIconIV.setImageResource(R.drawable.notifications_selected)
            currentTab = "notifications"

        } else if (tab == "community_invitations") {
            setupRVForCommunityInvitations()
            binding.communityInviteIconIV.setImageResource(R.drawable.community_invitations_selected)
            currentTab = "community_invitations"

        } else if (tab == "friend_requests") {
            setupRVForFriendRequests()
            binding.friendRequestIconIV.setImageResource(R.drawable.friend_requests_selected)
            currentTab = "friend_requests"
        }
    }

    private fun setupNotifications(){
        binding.notificationsRefreshLayout.isRefreshing = true
        binding.notificationsRefreshLayout.isRefreshing = false
        //TODO
    }

    private fun setupRVForCommunityInvitations(){
        binding.notificationsRefreshLayout.isRefreshing = true
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            val hosts:ArrayList<String> = ArrayList()
            for (key in user.communityInvitations.keys){
                hosts.add(key)
            }
            binding.notificationRV.layoutManager = LinearLayoutManager(context)
            requestsAdapter = RequestsAdapter(communityInvitations = user.communityInvitations, hosts = hosts, listener = this)
            binding.notificationRV.adapter = requestsAdapter
            binding.notificationsRefreshLayout.isRefreshing = false
        }
    }

    private fun setupRVForFriendRequests() {
        binding.notificationsRefreshLayout.isRefreshing = true
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            binding.notificationRV.layoutManager = LinearLayoutManager(context)
            requestsAdapter = RequestsAdapter(user.friendRequests, listener = this)
            binding.notificationRV.adapter = requestsAdapter
            binding.notificationsRefreshLayout.isRefreshing = false
        }
    }


    private fun bindButtons(){
        mainBinding.searchBtn.setOnClickListener {
            Toast.makeText(activity, "To be implemented", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::requestsAdapter.isInitialized){
            requestsAdapter.notifyDataSetChanged()
        }
    }

    override fun onChangeRequests(type: String) {
        //Refreshes adapter
        if (type == "community_invitations"){
            setupRVForCommunityInvitations()
        } else if (type == "notifications"){
            //TODO to add other types of notifications
            setupRVForFriendRequests()
        }
    }

    override fun onRefresh() {
        binding.notificationsRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            navigate(currentTab)
        },1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}