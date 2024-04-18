package com.example.synthronize

import android.content.Context
import android.os.Bundle
import android.view.*
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatroomAdapter: ChatroomAdapter

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
            val context = requireContext()
            recyclerView = binding.chatroomsRV
            recyclerView.layoutManager = LinearLayoutManager(activity)
            setupChatroomListRV(context)
        }
    }

    //Set Recycle View
    private fun setupChatroomListRV(context: Context){
        val query:Query = FirebaseUtil().retrieveAllChatRoomReferences().whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
                        .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        val options: FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(query, ChatroomModel::class.java).build()
        chatroomAdapter = ChatroomAdapter(context, options)
        recyclerView.adapter = chatroomAdapter
        chatroomAdapter.startListening()
    }
}