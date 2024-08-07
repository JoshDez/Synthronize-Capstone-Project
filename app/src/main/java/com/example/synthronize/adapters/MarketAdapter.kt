package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.Chatroom
import com.example.synthronize.databinding.ItemProductBinding
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
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
        holder.bind(model)
    }

    class ProductViewHolder(private val binding: ItemProductBinding, private val context: Context,
                                private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var productModel: ProductModel
        private val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        fun bind(model: ProductModel){
            productModel = model

            FirebaseUtil().targetUserDetails(productModel.sellerId).get().addOnSuccessListener {
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
                        context.startActivity(intent)
                    }
                }

                binding.descriptionTV.text = productModel.productDesc
                binding.nameTV.text = productModel.productName
                binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(productModel.createdTimestamp)
                binding.priceTV.text = format.format(productModel.productPrice)
            }

        }
    }
}