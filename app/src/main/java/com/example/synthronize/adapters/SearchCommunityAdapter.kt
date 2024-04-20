package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.databinding.ItemCommunityBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchCommunityAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>):
    FirestoreRecyclerAdapter<CommunityModel, SearchCommunityAdapter.CommunityViewHolder>(options) {
    private var totalItems = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommunityBinding.inflate(inflater, parent, false)
        return CommunityViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        totalItems += 1
        holder.bind(model)
    }
    fun getTotalItems():Int{
        return totalItems
    }

    class CommunityViewHolder(private val binding: ItemCommunityBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        fun bind(model: CommunityModel){
            binding.groupNameTextView.text = model.communityName
            binding.itemGroupLayout.setOnClickListener {
                Toast.makeText(context, "You selected ${model.communityName}", Toast.LENGTH_SHORT).show()
            }
        }

    }
}