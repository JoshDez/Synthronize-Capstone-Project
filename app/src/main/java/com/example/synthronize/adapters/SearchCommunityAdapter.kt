package com.example.synthronize.adapters

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.ItemCommunityBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import kotlinx.coroutines.NonDisposableHandle.parent

class SearchCommunityAdapter(private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>):
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

    class CommunityViewHolder(private val binding: ItemCommunityBinding, private val context: Context,
                              private val inflater: LayoutInflater, private val parent:ViewGroup): RecyclerView.ViewHolder(binding.root){
        private lateinit var dialogBinding: DialogCommunityPreviewBinding
        private lateinit var communityModel: CommunityModel
        fun bind(model: CommunityModel){
            //Checks if user is not on community block list
            if (!AppUtil().isUserOnList(model.blockList, FirebaseUtil().currentUserUid())){
                communityModel = model
                binding.groupNameTextView.text = model.communityName
                //TODO: Bind community image
                binding.itemGroupLayout.setOnClickListener {
                    dialogBinding = DialogCommunityPreviewBinding.inflate(inflater)
                    val dialogPlus = DialogPlus.newDialog(context)
                        .setContentHolder(ViewHolder(dialogBinding.root))
                        .setGravity(Gravity.CENTER)
                        .create()
                    dialogPlus.show()
                    setCommunityPreviewBindings()
                }
            } else {
                //hide the community if the user is on the community block list
                binding.itemGroupLayout.visibility == View.GONE
            }

        }
        private fun setCommunityPreviewBindings(){
            //Community details
            dialogBinding.communityNameTV.text = communityModel.communityName
            dialogBinding.communityDescriptionTV.text = communityModel.communityDescription
            dialogBinding.totalMembersCountTV.text = "${communityModel.communityMembers.size}"
            //dialogBinding.friendsJoinedCountTV.text = "${getFriendsJoinedCount()}"
            //dialogBinding.createdDateTV.text = communityModel.communityCreatedTimestamp

            //ON SET LISTENER BUTTONS
            //Join Community Button
            dialogBinding.joinCommunityBtn.setOnClickListener {
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                    .update("communityMembers", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        AppUtil().headToMainActivity(context, "community", 0, communityModel.communityId)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                    }
            }
            //Cancel Request Button
            dialogBinding.cancelRequestBtn.setOnClickListener {
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                    .update("joinRequestList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        appearButton(dialogBinding.requestToJoinBtn)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                    }
            }
            //Request To Join Button
            dialogBinding.requestToJoinBtn.setOnClickListener {
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                    .update("joinRequestList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                    .addOnSuccessListener {
                        appearButton(dialogBinding.cancelRequestBtn)
                        Toast.makeText(context, "Please wait for the admin to take you in", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                    }
            }
            //Enter Button
            dialogBinding.enterBtn.setOnClickListener {
                AppUtil().headToMainActivity(context, "community", 0, communityModel.communityId)
                Toast.makeText(context, "Clicked enter", Toast.LENGTH_SHORT).show()
            }

            //SET COMMUNITY TYPE
            if (communityModel.communityType == "Private"){
                //FOR PRIVATE COMMUNITY TYPE
                if (AppUtil().isUserOnList(communityModel.joinRequestList, FirebaseUtil().currentUserUid())){
                    //checks if user already requested to join
                    appearButton(dialogBinding.cancelRequestBtn)
                } else if (AppUtil().isUserOnList(communityModel.communityMembers, FirebaseUtil().currentUserUid())){
                    //checks if user is already a member
                    appearButton(dialogBinding.enterBtn)
                } else {
                    //user has not yet joined the group
                    appearButton(dialogBinding.requestToJoinBtn)
                }
            } else {
                //FOR PUBLIC COMMUNITY TYPE
                if (AppUtil().isUserOnList(communityModel.communityMembers, FirebaseUtil().currentUserUid())){
                    //checks if user is already a member
                    appearButton(dialogBinding.enterBtn)
                } else {
                    //user has not yet joined the group
                    appearButton(dialogBinding.joinCommunityBtn)
                }
            }
        }

        private fun getFriendsJoinedCount(membersList: List<String>, callback: (Int) -> Unit) {
            var count = 0
            FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
                val userModel = it.toObject(UserModel::class.java)
                //TODO: friends joined
            }

        }

        private fun appearButton(button:MaterialButton){
            dialogBinding.joinCommunityBtn.visibility = View.GONE
            dialogBinding.enterBtn.visibility = View.GONE
            dialogBinding.cancelRequestBtn.visibility = View.GONE
            dialogBinding.requestToJoinBtn.visibility = View.GONE
            when(button){
                dialogBinding.joinCommunityBtn -> dialogBinding.joinCommunityBtn.visibility = View.VISIBLE
                dialogBinding.enterBtn -> dialogBinding.enterBtn.visibility = View.VISIBLE
                dialogBinding.cancelRequestBtn -> dialogBinding.cancelRequestBtn.visibility = View.VISIBLE
                dialogBinding.requestToJoinBtn -> dialogBinding.requestToJoinBtn.visibility = View.VISIBLE
            }
        }
        

    }
}