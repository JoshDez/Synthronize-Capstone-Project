package com.example.synthronize

import android.content.Context
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
import com.example.synthronize.adapters.NotificationsAdapter
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
    private lateinit var invitationsAdapter: RequestsAdapter
    private lateinit var notificationsAdapter: NotificationsAdapter
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

    private fun navigate(tab:String, toRefresh:Boolean = false){
        binding.notificationsIconIV.setImageResource(R.drawable.notifications_not_selected)
        binding.communityInviteIconIV.setImageResource(R.drawable.community_invitations_not_selected)
        binding.friendRequestIconIV.setImageResource(R.drawable.friend_requests_not_selected)
        binding.notificationRV.visibility = View.GONE
        binding.invitationsRV.visibility = View.GONE
        binding.requestsRV.visibility = View.GONE


        if (tab == "notifications"){
            binding.notificationsIconIV.setImageResource(R.drawable.notifications_selected)
            binding.notificationRV.visibility = View.VISIBLE
            currentTab = "notifications"
            if (toRefresh || !::notificationsAdapter.isInitialized)
                setupNotifications()


        } else if (tab == "community_invitations") {
            binding.communityInviteIconIV.setImageResource(R.drawable.community_invitations_selected)
            binding.invitationsRV.visibility = View.VISIBLE
            currentTab = "community_invitations"
            if (toRefresh || !::invitationsAdapter.isInitialized)
                setupRVForCommunityInvitations()

        } else if (tab == "friend_requests") {
            binding.friendRequestIconIV.setImageResource(R.drawable.friend_requests_selected)
            binding.requestsRV.visibility = View.VISIBLE
            currentTab = "friend_requests"
            if (toRefresh || !::requestsAdapter.isInitialized)
                setupRVForFriendRequests()
        }
    }

    private fun setupNotifications(){
        binding.notificationsRefreshLayout.isRefreshing = true
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            val toSortKeys:ArrayList<String> = ArrayList()
            val sortedKeys:ArrayList<String> = ArrayList()

            try {
                //sort the keys by the timestamp of its value
                for (key in user.notifications.keys){
                    toSortKeys.add("${user.notifications.getValue(key)[5]}/$key")
                }
                toSortKeys.sortDescending()

                //extract keys
                for (key in toSortKeys){
                    sortedKeys.add(key.split("/")[1])
                }

                binding.notificationRV.layoutManager = LinearLayoutManager(context)
                notificationsAdapter = NotificationsAdapter(context, sortedKeys = sortedKeys, notifications = user.notifications, listener = this)
                binding.notificationRV.adapter = notificationsAdapter
                binding.notificationsRefreshLayout.isRefreshing = false
            } catch (e:Exception){
                Log.d("error in setup notifications", e.message.toString())
            }
        }
    }

    private fun setupRVForCommunityInvitations(){
        binding.notificationsRefreshLayout.isRefreshing = true
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            val hosts:ArrayList<String> = ArrayList()
            for (key in user.communityInvitations.keys){
                hosts.add(key)
            }
            binding.invitationsRV.layoutManager = LinearLayoutManager(context)
            invitationsAdapter = RequestsAdapter(communityInvitations = user.communityInvitations, hosts = hosts, listener = this)
            binding.invitationsRV.adapter = invitationsAdapter
            binding.notificationsRefreshLayout.isRefreshing = false
        }
    }

    private fun setupRVForFriendRequests() {
        binding.notificationsRefreshLayout.isRefreshing = true
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            binding.requestsRV.layoutManager = LinearLayoutManager(context)
            requestsAdapter = RequestsAdapter(user.friendRequests, listener = this)
            binding.requestsRV.adapter = requestsAdapter
            binding.notificationsRefreshLayout.isRefreshing = false
        }
    }


    private fun bindButtons(){
        mainBinding.searchBtn.setOnClickListener {
            Toast.makeText(activity, "To be implemented", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onChangeRequests(type: String) {
        //Refreshes adapter
        if (type == "community_invitations"){
            setupRVForCommunityInvitations()
        } else if (type == "friend_request"){
            setupRVForFriendRequests()
        } else if (type == "notifications"){
            setupNotifications()
        }
    }

    override fun onRefresh() {
        binding.notificationsRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            navigate(currentTab, true)
        },1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }
}