package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.R
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.ItemCommunityBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class SearchCommunityAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>,
                             private var listener:OnItemClickListener, private var forProfileUtil:Boolean = false,
                             private var dialogPlus: DialogPlus = DialogPlus.newDialog(context).setContentHolder(ViewHolder(R.layout.dialog_list)).create(),
                             private var removeDivider:Boolean = false):
    FirestoreRecyclerAdapter<CommunityModel, SearchCommunityAdapter.CommunityViewHolder>(options) {
    private var totalItems = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommunityBinding.inflate(inflater, parent, false)
        return CommunityViewHolder(binding, context, inflater, parent)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        totalItems += 1
        holder.bind(model)
    }
    fun getTotalItems():Int{
        return totalItems
    }

    inner class CommunityViewHolder(private val binding: ItemCommunityBinding, private val context: Context,
                              private val inflater: LayoutInflater, private val parent:ViewGroup): RecyclerView.ViewHolder(binding.root){
        private lateinit var dialogBinding: DialogCommunityPreviewBinding
        private lateinit var communityModel: CommunityModel
        fun bind(model: CommunityModel){
            //removes divider
            if (removeDivider)
                binding.divider.visibility = View.INVISIBLE

            //Checks if user is not on community block list
            if (!AppUtil().isIdOnList(model.bannedUsers, FirebaseUtil().currentUserUid())){
                communityModel = model
                binding.groupNameTextView.text = model.communityName
                AppUtil().setCommunityProfilePic(context, communityModel.communityId, binding.profileImage)

                binding.itemGroupLayout.setOnClickListener {
                    if (forProfileUtil){
                        //For Profile Util only
                        //closes dialog plus
                        dialogPlus.dismiss()
                        listener.onItemClick(communityModel.communityId)
                    } else {
                        DialogUtil().openCommunityPreviewDialog(context, inflater, communityModel)
                    }
                }
            } else {
                //hide the community if the user is on the community block list
                binding.itemGroupLayout.visibility == View.GONE
            }


        }
    }
}