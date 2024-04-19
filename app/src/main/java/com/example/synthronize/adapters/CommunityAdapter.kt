package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemGroupBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil

class CommunityAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>):
    FirestoreRecyclerAdapter<CommunityModel, CommunityAdapter.CommunityViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGroupBinding.inflate(inflater, parent, false)
        return CommunityViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        holder.bind(model)
    }

    class CommunityViewHolder(private val binding: ItemGroupBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){
        fun bind(model: CommunityModel){
            binding.groupNameTextView.text = model.communityName
            //set on click listener
            binding.itemGroupLayout.setOnClickListener {
                //Head to MainActivity with community fragment
                AppUtil().headToMainActivity(context, "community", 0, model.communityId)
            }
        }
        
    }

}