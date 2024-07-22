package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.Chatroom
import com.example.synthronize.R
import com.example.synthronize.ViewPost
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemMessageBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.MessageModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.GlideApp
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat

class MessageAdapter(private val context: Context, options: FirestoreRecyclerOptions<MessageModel>):
    FirestoreRecyclerAdapter<MessageModel, MessageAdapter.MessageViewHolder>(options) {

    private var itemCount:Int = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMessageBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: MessageModel) {
        holder.bind(model)
        itemCount += 1
    }

    fun getMessageCount(): Int{
        return itemCount
    }

    class MessageViewHolder(private val binding: ItemMessageBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){
        fun bind(model: MessageModel){

            //bind user's message
            if (model.senderID == FirebaseUtil().currentUserUid()){
                //If user is the sender (YOU)
                binding.recieverLayout.visibility = View.GONE
                binding.senderLayout.visibility = View.VISIBLE
                binding.senderMsgTV.visibility = View.VISIBLE
                binding.senderMsgTV.text = model.message
                bindPostMessage(model, true)
            } else {
                //If user is the receiver
                binding.senderLayout.visibility = View.GONE
                binding.recieverLayout.visibility = View.VISIBLE
                binding.recieverMsgTV.visibility = View.VISIBLE
                binding.recieverMsgTV.text = model.message
                bindPostMessage(model, false)
                //retrieve sender user data
                FirebaseUtil().targetUserDetails(model.senderID).get().addOnCompleteListener {
                    if (it.isSuccessful && it.result.exists()){
                        val userModel = it.result.toObject(UserModel::class.java)!!
                        binding.userNameTV.text = userModel.fullName
                        AppUtil().setUserProfilePic(context, userModel.userID, binding.userProfileCIV)
                    }
                }
            }

        }
        private fun bindPostMessage(model: MessageModel, isSender:Boolean){
            if (model.postID.isNotEmpty() && model.postID != "null" &&
                model.communityIdOfPost.isNotEmpty() && model.communityIdOfPost != "null" ){

                FirebaseUtil().retrieveCommunityDocument(model.communityIdOfPost).get().addOnSuccessListener {community ->
                    val communityModel = community.toObject(CommunityModel::class.java)!!

                    FirebaseUtil().retrieveCommunityFeedsCollection(model.communityIdOfPost).document(model.postID).get().addOnSuccessListener {
                        val postModel = it.toObject(PostModel::class.java)!!

                        FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener {owner ->
                            val userModel = owner.toObject(UserModel::class.java)!!

                            if (communityModel.communityType == "Private" && !AppUtil().isIdOnList(communityModel.communityMembers.keys, FirebaseUtil().currentUserUid())){
                                //if post is not available to current user
                                if (isSender){
                                    binding.postLayout2.visibility = View.VISIBLE
                                    binding.postCaptionTV2.text = "Post is Unavailable"
                                } else {
                                    binding.postLayout.visibility = View.VISIBLE
                                    binding.postCaptionTV.text = "Post is Unavailable"
                                }
                            } else {
                                //if post is available to current user
                                if (isSender){
                                    //Sender (YOU)
                                    binding.postLayout2.visibility = View.VISIBLE

                                    binding.postLayout2.setOnClickListener {
                                        headToPost(postModel.postId, postModel.communityId)
                                    }

                                    AppUtil().setUserProfilePic(context, postModel.ownerId, binding.postOwnerProfileCIV2)
                                    binding.postCaptionTV2.text = postModel.caption
                                    binding.postOwnerUsernameTV2.text = userModel.username

                                    if (postModel.contentList.isNotEmpty()){
                                        binding.postThumbnailIV2.visibility = View.VISIBLE
                                        GlideApp.with(context)
                                            .load(FirebaseUtil().retrieveCommunityContentImageRef(postModel.contentList[0]))
                                            .error(R.drawable.baseline_image_24)
                                            .into(binding.postThumbnailIV2)
                                    }
                                } else {
                                    //Receiver
                                    binding.postLayout.visibility = View.VISIBLE

                                    binding.postLayout.setOnClickListener {
                                        headToPost(postModel.postId, postModel.communityId)
                                    }

                                    AppUtil().setUserProfilePic(context, postModel.ownerId, binding.postOwnerProfileCIV)
                                    binding.postCaptionTV.text = postModel.caption
                                    binding.postOwnerUsernameTV.text = userModel.username

                                    if (postModel.contentList.isNotEmpty()){
                                        binding.postThumbnail.visibility = View.VISIBLE
                                        GlideApp.with(context)
                                            .load(FirebaseUtil().retrieveCommunityContentImageRef(postModel.contentList[0]))
                                            .error(R.drawable.baseline_image_24)
                                            .into(binding.postThumbnail)
                                    }
                                }

                            }

                        }

                    }.addOnFailureListener {
                        //if post itself is not available
                        if (isSender){
                            binding.postLayout2.visibility = View.VISIBLE
                            binding.postCaptionTV2.text = "Post is Unavailable"
                        } else {
                            binding.postLayout.visibility = View.VISIBLE
                            binding.postCaptionTV.text = "Post is Unavailable"
                        }
                    }
                }.addOnFailureListener {
                    //if community is not available
                    if (isSender){
                        binding.postLayout2.visibility = View.VISIBLE
                        binding.postCaptionTV2.text = "Post is Unavailable"
                    } else {
                        binding.postLayout.visibility = View.VISIBLE
                        binding.postCaptionTV.text = "Post is Unavailable"
                    }
                }

            }
        }

        private fun headToPost(postId:String, communityId:String){
            val intent = Intent(context, ViewPost::class.java)
            intent.putExtra("communityId", communityId)
            intent.putExtra("postId", postId)
            context.startActivity(intent)
        }
    }

}