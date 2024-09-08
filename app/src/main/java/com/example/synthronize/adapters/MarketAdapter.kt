package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.synthronize.Chatroom
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.ViewProduct
import com.example.synthronize.databinding.ItemProductBinding
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.NumberFormat
import java.util.Locale

class MarketAdapter(private val context: Context, options: FirestoreRecyclerOptions<ProductModel>):
    FirestoreRecyclerAdapter<ProductModel, MarketAdapter.ProductViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProductBinding.inflate(inflater, parent, false)
        return ProductViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, model: ProductModel) {
        holder.checkAvailabilityBeforeBind(model)
    }

    class ProductViewHolder(private val binding: ItemProductBinding, private val context: Context,
                                private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var productModel: ProductModel
        private lateinit var viewPageAdapter: ViewPageAdapter
        private val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        fun checkAvailabilityBeforeBind(model: ProductModel){
            ContentUtil().verifyCommunityContentAvailability(model.ownerId, model.communityId){ isAvailable ->
                if(isAvailable){
                    bindProduct(model)
                } else {
                    bindContentNotAvailable()
                }
            }
        }
        private fun bindContentNotAvailable(){
            binding.descriptionTV.text = "Content Not Available"
            binding.messageUserBtn.visibility = View.GONE
            binding.viewPager2.visibility = View.GONE
        }


        private fun bindProduct(model: ProductModel){
            productModel = model

            FirebaseUtil().targetUserDetails(productModel.ownerId).get().addOnSuccessListener {
                val model = it.toObject(UserModel::class.java)!!
                AppUtil().setUserProfilePic(context, model.userID, binding.profileCIV)
                binding.usernameTV.text = model.username

                if (model.userID == FirebaseUtil().currentUserUid()){
                    binding.messageUserBtn.visibility = View.GONE
                } else {
                    binding.messageUserBtn.setOnClickListener {
                        val intent = Intent(context, Chatroom::class.java)
                        intent.putExtra("chatroomName", model.fullName)
                        intent.putExtra("userID", model.userID)
                        intent.putExtra("chatroomType", "direct_message")
                        intent.putExtra("productId", productModel.productId)
                        intent.putExtra("communityIdOfPost", productModel.communityId)
                        context.startActivity(intent)
                    }
                }

                binding.descriptionTV.text = productModel.productDesc
                binding.nameTV.text = productModel.productName
                binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(productModel.createdTimestamp)
                binding.priceTV.text = format.format(productModel.price)

                binding.mainLayout.setOnClickListener {
                    val intent = Intent(context, ViewProduct::class.java)
                    intent.putExtra("communityId", productModel.communityId)
                    intent.putExtra("productId", productModel.productId)
                    context.startActivity(intent)
                }

                binding.profileCIV.setOnClickListener {
                    headToUserProfile()
                }

                binding.usernameTV.setOnClickListener {
                    headToUserProfile()
                }

                binding.menuBtn.setOnClickListener {
                    DialogUtil().openMenuDialog(context, inflater, "Product", productModel.productId,
                        productModel.ownerId, productModel.communityId){}
                }

                bindContent()
            }
        }


        private fun headToUserProfile() {
            if (productModel.ownerId != FirebaseUtil().currentUserUid()){
                val intent = Intent(context, OtherUserProfile::class.java)
                intent.putExtra("userID", productModel.ownerId)
                context.startActivity(intent)
            }
        }
        private fun bindContent() {
            if (productModel.imageList.isNotEmpty()){
                //displays content with view pager 2
                binding.viewPager2.visibility = View.VISIBLE
                viewPageAdapter = ViewPageAdapter(binding.root.context, productModel.imageList)
                binding.viewPager2.adapter = viewPageAdapter
                binding.viewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                //shows the indicator if the content is more than one
                if (productModel.imageList.size > 1){
                    binding.circleIndicator3.visibility = View.VISIBLE
                    binding.circleIndicator3.setViewPager(binding.viewPager2)
                    binding.circleIndicator3
                }
            } else {
                //default
                binding.viewPager2.visibility = View.GONE
                binding.circleIndicator3.visibility = View.GONE
            }
        }
    }
}