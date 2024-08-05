package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemCommunityChatsBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

//CHATROOMS
class CommunityChatroomsAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>):
    FirestoreRecyclerAdapter<CommunityModel, CommunityChatroomsAdapter.CommunityChatroomViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityChatroomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommunityChatsBinding.inflate(inflater, parent, false)
        return CommunityChatroomViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: CommunityChatroomViewHolder, position: Int, model: CommunityModel) {
        holder.bind(model)
    }


    class CommunityChatroomViewHolder(private val binding: ItemCommunityChatsBinding, private val context: Context,
                                      private val inflater: LayoutInflater): RecyclerView.ViewHolder(binding.root){

        private lateinit var communityModel: CommunityModel
        private lateinit var chatroomAdapter: ChatroomAdapter
        private var isOpen = false

        fun bind(model: CommunityModel){
            communityModel = model

            binding.communityNameTV.text = communityModel.communityName
            AppUtil().setCommunityProfilePic(context, communityModel.communityId, binding.communityProfileCIV)
            binding.communityChatsRV.layoutManager = LinearLayoutManager(context)
            setupCommunityChatroom()

            //toggles recycle view
            binding.communityLayout.setOnClickListener {
                if (isOpen){
                    binding.communityChatsRV.visibility = View.GONE
                    isOpen = false
                } else {
                    binding.communityChatsRV.visibility = View.VISIBLE
                    isOpen = true
                }
            }

            //opens community preview dialog
            binding.communityProfileCIV.setOnClickListener {
                DialogUtil().openCommunityPreviewDialog(context, inflater, communityModel)
            }
        }

        private fun setupCommunityChatroom(){
            binding.communityChatsRV.visibility = View.VISIBLE
            isOpen = true

            //already indexed in firebase
            val query: Query = FirebaseUtil().retrieveAllChatRoomReferences()
                .whereEqualTo("communityId", communityModel.communityId)
                .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
                .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

            val options: FirestoreRecyclerOptions<ChatroomModel> =
                FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(query, ChatroomModel::class.java).build()

            chatroomAdapter = ChatroomAdapter(context, options)
            binding.communityChatsRV.adapter = chatroomAdapter
            chatroomAdapter.startListening()

        }

    }

}