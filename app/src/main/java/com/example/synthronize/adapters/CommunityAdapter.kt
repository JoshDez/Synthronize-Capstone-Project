package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.CommunityFragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemCommunityBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil

class CommunityAdapter(private val mainBinding: ActivityMainBinding,
                       private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>, private val listener:OnItemClickListener):
    FirestoreRecyclerAdapter<CommunityModel, CommunityAdapter.CommunityViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val communityBinding = ItemCommunityBinding.inflate(inflater, parent, false)
        return CommunityViewHolder(mainBinding, communityBinding, context)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        holder.bind(model)
    }

    inner class CommunityViewHolder(private val mainBinding: ActivityMainBinding, private val communityBinding: ItemCommunityBinding,
                                    private val context: Context): RecyclerView.ViewHolder(communityBinding.root){
        fun bind(model: CommunityModel){
            communityBinding.groupNameTextView.text = model.communityName
            //set on click listener
            communityBinding.itemGroupLayout.setOnClickListener {
                //set the header of main activity
                mainBinding.toolbarTitleTV.text = model.communityName
                listener.onItemClick(model.communityId)
            }
            AppUtil().setCommunityProfilePic(context, model.communityId, communityBinding.profileImage)
        }
    }

}