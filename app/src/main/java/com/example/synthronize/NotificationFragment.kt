package com.example.synthronize

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.RequestsAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentNotificationBinding
import com.example.synthronize.interfaces.NotificationOnDataChange
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.Query

class NotificationFragment(private val mainBinding: ActivityMainBinding): Fragment(), NotificationOnDataChange {
    // TODO: Rename and change types of parameters
    private lateinit var binding: FragmentNotificationBinding
    private lateinit var requestsAdapter: RequestsAdapter
    private lateinit var context: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "NOTIFICATIONS"

        if (isAdded){
            context = requireContext()
            if (::context.isInitialized){
                bindButtons()
                setupRVForFriendRequests()

                binding.notificationBtn.setOnClickListener {
                    navigate("notifications")
                }
                binding.communityInvitationsBtn.setOnClickListener {
                    navigate("community_invitations")
                }
            }
        }
    }

    private fun navigate(tab:String){
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.less_saturated_light_purple)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.light_purple)
        binding.notificationBtn.setTextColor(unselectedColor)
        binding.communityInvitationsBtn.setTextColor(unselectedColor)


        if (tab == "notifications"){
            setupRVForFriendRequests()
            binding.notificationBtn.setTextColor(selectedColor)
        }else if (tab == "community_invitations") {
            setupRVForCommunityInvitations()
            binding.communityInvitationsBtn.setTextColor(selectedColor)
        }
    }
    private fun setupRVForCommunityInvitations(){
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            val hosts:ArrayList<String> = ArrayList()
            for (key in user.communityInvitations.keys){
                hosts.add(key)
            }
            binding.notificationRV.layoutManager = LinearLayoutManager(context)
            requestsAdapter = RequestsAdapter(communityInvitations = user.communityInvitations, hosts = hosts, listener = this)
            binding.notificationRV.adapter = requestsAdapter
        }
    }

    private fun setupRVForFriendRequests() {
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(UserModel::class.java)!!
            binding.notificationRV.layoutManager = LinearLayoutManager(context)
            requestsAdapter = RequestsAdapter(user.friendRequests, listener = this)
            binding.notificationRV.adapter = requestsAdapter
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
}