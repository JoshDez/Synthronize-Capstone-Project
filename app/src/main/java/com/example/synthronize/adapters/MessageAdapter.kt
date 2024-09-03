package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.ViewPost
import com.example.synthronize.ViewProduct
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemMessageBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.MessageModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.FirebaseUtil

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
                bindProductMessage(model, true)
            } else {
                //If user is the receiver
                binding.senderLayout.visibility = View.GONE
                binding.recieverLayout.visibility = View.VISIBLE
                binding.recieverMsgTV.visibility = View.VISIBLE
                binding.recieverMsgTV.text = model.message
                bindPostMessage(model, false)
                bindProductMessage(model, false)
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
                    if (community.exists()){
                        //community exists

                        val communityModel = community.toObject(CommunityModel::class.java)!!

                        FirebaseUtil().retrieveCommunityFeedsCollection(model.communityIdOfPost).document(model.postID).get().addOnSuccessListener {post ->
                            if (post.exists()){
                                //If post exists

                                val postModel = post.toObject(PostModel::class.java)!!

                                FirebaseUtil().targetUserDetails(postModel.ownerId).get().addOnSuccessListener {owner ->
                                    if (owner.exists()){
                                        //owner exists

                                        val userModel = owner.toObject(UserModel::class.java)!!

                                        if (communityModel.communityType == "Private" && !AppUtil().isIdOnList(communityModel.communityMembers.keys, FirebaseUtil().currentUserUid())){
                                            //if post is not available to current user
                                            displayContentNotAvailable(isSender)
                                        } else {
                                            //if post is available to current user
                                            //BIND POST
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
                                                    ContentUtil().setImageContent(context, postModel.contentList[0], binding.postThumbnailIV2)
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
                                                    ContentUtil().setImageContent(context, postModel.contentList[0], binding.postThumbnail)
                                                }
                                            }

                                        }

                                    } else {
                                        //owner no longer exists
                                        displayContentNotAvailable(isSender)
                                    }
                                }
                            } else {
                                //if post no longer exists
                                displayContentNotAvailable(isSender)
                            }
                        }.addOnFailureListener {
                            //if theres an error retrieving post
                            displayContentNotAvailable(isSender)
                        }
                    } else {
                        //community no longer exists
                        displayContentNotAvailable(isSender)
                    }
                }.addOnFailureListener {
                    //if theres an error retrieving community
                    displayContentNotAvailable(isSender)
                }

            }
        }

        private fun bindProductMessage(model: MessageModel, isSender:Boolean){
            if (model.productID.isNotEmpty() && model.productID != "null" &&
                model.communityIdOfPost.isNotEmpty() && model.communityIdOfPost != "null" ){

                FirebaseUtil().retrieveCommunityDocument(model.communityIdOfPost).get().addOnSuccessListener {community ->
                    if (community.exists()){
                        //community exists

                        val communityModel = community.toObject(CommunityModel::class.java)!!

                        FirebaseUtil().retrieveCommunityMarketCollection(model.communityIdOfPost).document(model.productID).get().addOnSuccessListener {product ->
                            if (product.exists()){
                                //If post exists

                                val productModel = product.toObject(ProductModel::class.java)!!

                                FirebaseUtil().targetUserDetails(productModel.ownerId).get().addOnSuccessListener {owner ->
                                    if (owner.exists()){
                                        //owner exists

                                        val userModel = owner.toObject(UserModel::class.java)!!

                                        if (communityModel.communityType == "Private" && !AppUtil().isIdOnList(communityModel.communityMembers.keys, FirebaseUtil().currentUserUid())){
                                            //if post is not available to current user
                                            displayContentNotAvailable(isSender)
                                        } else {
                                            //if post is available to current user
                                            //BIND PRODUCT
                                            if (isSender){
                                                //Sender (YOU)
                                                binding.postLayout2.visibility = View.VISIBLE
                                                binding.postLayout2.setOnClickListener {
                                                    headToProduct(productModel.productId, productModel.communityId)
                                                }
                                                AppUtil().setUserProfilePic(context, productModel.ownerId, binding.postOwnerProfileCIV2)
                                                binding.postCaptionTV2.text = productModel.productName
                                                binding.postOwnerUsernameTV2.text = userModel.username

                                                if (productModel.imageList.isNotEmpty()){
                                                    binding.postThumbnailIV2.visibility = View.VISIBLE
                                                    ContentUtil().setImageContent(context, productModel.imageList[0], binding.postThumbnailIV2)
                                                }
                                            } else {
                                                //Receiver
                                                binding.postLayout.visibility = View.VISIBLE

                                                binding.postLayout.setOnClickListener {
                                                    headToProduct(productModel.productId, productModel.communityId)
                                                }

                                                AppUtil().setUserProfilePic(context, productModel.ownerId, binding.postOwnerProfileCIV)
                                                binding.postCaptionTV.text = productModel.productName
                                                binding.postOwnerUsernameTV.text = userModel.username

                                                if (productModel.imageList.isNotEmpty()){
                                                    binding.postThumbnail.visibility = View.VISIBLE
                                                    ContentUtil().setImageContent(context, productModel.imageList[0], binding.postThumbnail)
                                                }
                                            }

                                        }

                                    } else {
                                        //owner no longer exists
                                        displayContentNotAvailable(isSender)
                                    }
                                }
                            } else {
                                //if post no longer exists
                                displayContentNotAvailable(isSender)
                            }
                        }.addOnFailureListener {
                            //if theres an error retrieving post
                            displayContentNotAvailable(isSender)
                        }
                    } else {
                        //community no longer exists
                        displayContentNotAvailable(isSender)
                    }
                }.addOnFailureListener {
                    //if theres an error retrieving community
                    displayContentNotAvailable(isSender)
                }

            }
        }
        
        private fun displayContentNotAvailable(isSender:Boolean){
            if (isSender){
                binding.postOwnerProfileLayout2.visibility = View.GONE
                binding.postThumbnailIV2.visibility = View.GONE
                binding.postLayout2.visibility = View.VISIBLE
                binding.postCaptionTV2.text = "Content not available"
            } else {
                binding.postOwnerProfileLayout.visibility = View.GONE
                binding.postThumbnail.visibility = View.GONE
                binding.postLayout.visibility = View.VISIBLE
                binding.postCaptionTV.text = "Content not available"
            }
        }

        private fun headToPost(postId:String, communityId:String){
            val intent = Intent(context, ViewPost::class.java)
            intent.putExtra("communityId", communityId)
            intent.putExtra("postId", postId)
            context.startActivity(intent)
        }
        private fun headToProduct(productId:String, communityId:String){
            val intent = Intent(context, ViewProduct::class.java)
            intent.putExtra("communityId", communityId)
            intent.putExtra("productId", productId)
            context.startActivity(intent)
        }
    }

}