package com.example.synthronize.adapters

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.databinding.ItemRequestBinding
import com.example.synthronize.interfaces.NotificationOnDataChange
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import com.google.firestore.v1.MapValue

//FOR NOTIFICATIONS
class RequestsAdapter(private var friendRequests:List<String> = listOf(),
                      private val communityInvitations:Map<String, String> = HashMap(), private val hosts:ArrayList<String> = ArrayList(),
                      private val listener:NotificationOnDataChange): RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RequestsAdapter.RequestViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRequestBinding.inflate(inflater, parent, false)
        return RequestViewHolder(binding, inflater)
    }

    override fun onBindViewHolder(holder: RequestsAdapter.RequestViewHolder, position: Int) {
        if (friendRequests.isNotEmpty()){
            holder.bindFriendRequest(friendRequests[0])
        }else {
            //community invitations
            holder.bindCommunityInvitations(hosts[position], communityInvitations)
        }

    }

    override fun getItemCount(): Int {
        if (friendRequests.isNotEmpty()){
            return friendRequests.size
        }else {
            return communityInvitations.size
        }
    }

    inner class RequestViewHolder(private val itemRequestBinding:ItemRequestBinding, private val inflater: LayoutInflater):RecyclerView.ViewHolder(itemRequestBinding.root){
        fun bindFriendRequest(uid: String){
            FirebaseUtil().targetUserDetails(uid).get().addOnSuccessListener {
                val user = it.toObject(UserModel::class.java)!!
                itemRequestBinding.requestTV.text = "${user.fullName} sent you a friend request"
                AppUtil().setUserProfilePic(itemRequestBinding.root.context, uid, itemRequestBinding.profileCIV)
                itemRequestBinding.acceptBtn.visibility = View.VISIBLE
                itemRequestBinding.acceptBtn.setOnClickListener {
                    FirebaseUtil().currentUserDetails().update("friendsList", FieldValue.arrayUnion(uid))
                    FirebaseUtil().targetUserDetails(uid).update("friendsList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                    Toast.makeText(itemRequestBinding.root.context, "Friend Request Accepted", Toast.LENGTH_SHORT).show()
                    removeFriendRequest(uid)
                }
                itemRequestBinding.rejectBtn.visibility = View.VISIBLE
                itemRequestBinding.rejectBtn.setOnClickListener {
                    FirebaseUtil().currentUserDetails().update("friendRequests", FieldValue.arrayRemove(uid))
                    Toast.makeText(itemRequestBinding.root.context, "Friend Request Declined", Toast.LENGTH_SHORT).show()
                    removeFriendRequest(uid)
                }
                itemRequestBinding.requestTV.setOnClickListener {
                    val intent = Intent(itemRequestBinding.root.context, OtherUserProfile::class.java)
                    intent.putExtra("userID", uid)
                    itemRequestBinding.root.context.startActivity(intent)
                }
            }
        }
        fun bindCommunityInvitations(key:String, communityInvitations: Map<String, String>){
            FirebaseUtil().targetUserDetails(key).get().addOnSuccessListener {
                val host = it.toObject(UserModel::class.java)!!
                val communityId = communityInvitations.getValue(key)
                FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {result ->
                    val community = result.toObject(CommunityModel::class.java)!!
                    itemRequestBinding.requestTV.text = "${host.fullName} has invited you to join ${community.communityName} community"
                    AppUtil().setCommunityProfilePic(itemRequestBinding.root.context, community.communityId , itemRequestBinding.profileCIV)
                    itemRequestBinding.acceptBtn.visibility = View.VISIBLE
                    itemRequestBinding.acceptBtn.setOnClickListener {
                        FirebaseUtil().retrieveCommunityDocument(communityId)
                            .update("communityMembers", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                                FirebaseUtil().addUserToAllCommunityChannels(communityId, FirebaseUtil().currentUserUid()){
                                    removeCommunityInvitation(key)
                                    Toast.makeText(itemRequestBinding.root.context, "Invitation Accepted", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    itemRequestBinding.rejectBtn.visibility = View.VISIBLE
                    itemRequestBinding.rejectBtn.setOnClickListener {
                        removeCommunityInvitation(key)
                        Toast.makeText(itemRequestBinding.root.context, "Invitation Decline", Toast.LENGTH_SHORT).show()

                    }
                    itemRequestBinding.requestTV.setOnClickListener {
                        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {result ->
                            val communityModel = result.toObject(CommunityModel::class.java)!!
                            DialogUtil().openCommunityPreviewDialog(itemRequestBinding.root.context, inflater, communityModel)
                        }

                    }
                }
            }.addOnFailureListener {
                //
            }
        }
        private fun removeCommunityInvitation(key: String){
            val updates = hashMapOf<String, Any>(
                "communityInvitations.${key}" to FieldValue.delete()
            )
            FirebaseUtil().currentUserDetails().update(updates).addOnSuccessListener {
                Log.d(TAG, "Key '$key' removed successfully.")
                listener.onChangeRequests("community_invitations")
            }.addOnFailureListener {
                Log.d(TAG, "error removing Key '$key'")
            }
        }

        private fun removeFriendRequest(uid: String){
            FirebaseUtil().currentUserDetails().update("friendRequests", FieldValue.arrayRemove(uid)).addOnSuccessListener {
                Log.d(TAG, "request removed")
                listener.onChangeRequests("notifications")
            }.addOnFailureListener {
                Log.d(TAG, "error in removing friend request")
            }
        }
    }

}