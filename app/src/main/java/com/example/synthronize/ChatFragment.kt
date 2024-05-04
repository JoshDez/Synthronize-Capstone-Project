package com.example.synthronize

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.example.synthronize.adapters.ChatroomAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentChatBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.utils.FirebaseUtil

class ChatFragment(private val mainBinding: ActivityMainBinding) : Fragment() {
    private lateinit var binding: FragmentChatBinding
    private lateinit var chatroomAdapter: ChatroomAdapter
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
        binding = FragmentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "INBOX"

        //checks if fragment is added to the main activity
        if (isAdded){
            context = requireContext()
            if (::context.isInitialized){

                binding.inboxRV.layoutManager = LinearLayoutManager(activity)
                binding.communityChatsRV.layoutManager = LinearLayoutManager(activity)

                setupChatroomListForInbox()

                binding.inboxBtn.setOnClickListener {
                    navigate("direct_message")
                }

                binding.communityChatsBtn.setOnClickListener {
                    navigate("community_chat")
                }

            }
        }
    }

    private fun navigate(tab:String){
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.less_saturated_light_purple)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.light_purple)
        binding.inboxBtn.setTextColor(unselectedColor)
        binding.communityChatsBtn.setTextColor(unselectedColor)
        binding.inboxRV.visibility = View.GONE
        binding.communityChatsRV.visibility = View.GONE


        if (tab == "community_chat"){
            binding.communityChatsRV.visibility = View.VISIBLE
            setupChatroomListForCommunity()
            binding.communityChatsBtn.setTextColor(selectedColor)
        }else if (tab == "direct_message"){
            binding.inboxRV.visibility = View.VISIBLE
            setupChatroomListForInbox()
            binding.inboxBtn.setTextColor(selectedColor)
        }
    }

    //Set Recycle View
    private fun setupChatroomListForInbox(){
        //already indexed in firebase
        val query:Query = FirebaseUtil().retrieveAllChatRoomReferences()
            .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
            .whereEqualTo("chatroomType", "direct_message")
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        val options: FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(query, ChatroomModel::class.java).build()

        chatroomAdapter = ChatroomAdapter(context, options)
        binding.inboxRV.adapter = chatroomAdapter
        chatroomAdapter.startListening()
    }

    private fun setupChatroomListForCommunity(){
        //already indexed in firebase
        val query:Query = FirebaseUtil().retrieveAllChatRoomReferences()
            .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
            .whereEqualTo("chatroomType", "community_chat")
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        val options: FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(query, ChatroomModel::class.java).build()

        chatroomAdapter = ChatroomAdapter(context, options)
        binding.communityChatsRV.adapter = chatroomAdapter
        chatroomAdapter.startListening()
    }



    override fun onStart() {
        super.onStart()
        if (::chatroomAdapter.isInitialized){
            chatroomAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::chatroomAdapter.isInitialized){
            chatroomAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::chatroomAdapter.isInitialized){
            chatroomAdapter.stopListening()
        }
    }
}