package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemSuggestionBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil


//Friend suggestion used for in Explore fragment

class CommunitySuggestionAdapter(private val context: Context, private val communityIdList:ArrayList<String>)
    : RecyclerView.Adapter<CommunitySuggestionAdapter.CommunitySuggestionViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunitySuggestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSuggestionBinding.inflate(inflater, parent, false)
        return CommunitySuggestionViewHolder(binding, context, inflater)
    }

    override fun getItemCount(): Int {
        return communityIdList.size
    }

    override fun onBindViewHolder(holder: CommunitySuggestionViewHolder, position: Int) {
        FirebaseUtil().retrieveCommunityDocument(communityIdList[position]).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            holder.bind(communityModel)
        }
    }

    class CommunitySuggestionViewHolder(private val binding: ItemSuggestionBinding, private val context: Context, private val inflater: LayoutInflater): RecyclerView.ViewHolder(binding.root){
        private lateinit var communityModel: CommunityModel

        fun bind(model: CommunityModel){
            communityModel = model
            AppUtil().setCommunityProfilePic(context, model.communityId, binding.suggestionCIV)
            binding.suggestionNameTV.text = AppUtil().sliceMessage(model.communityName, 30)

            if (!communityModel.bannedUsers.contains(FirebaseUtil().currentUserUid())){

                AppUtil().changeCommunityButtonStates(context, binding.actionBtn, communityModel.communityId)

                binding.suggestionCIV.setOnClickListener {
                    DialogUtil().openCommunityPreviewDialog(context, inflater, model)
                }

                binding.suggestionMainLayout.setOnClickListener {
                    DialogUtil().openCommunityPreviewDialog(context, inflater, model)
                }

                binding.suggestionNameTV.setOnClickListener {
                    DialogUtil().openCommunityPreviewDialog(context, inflater, model)
                }
            }
        }
    }
}