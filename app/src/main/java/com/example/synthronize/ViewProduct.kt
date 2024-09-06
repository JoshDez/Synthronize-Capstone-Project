package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.databinding.ActivityViewProductBinding
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.ProductModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.ContentUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import java.text.NumberFormat
import java.util.Locale

class ViewProduct : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener {
    private lateinit var binding:ActivityViewProductBinding
    private lateinit var communityId:String
    private lateinit var productId:String
    private lateinit var productModel: ProductModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewProductRefreshLayout.setOnRefreshListener(this)

        //check for internet
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        communityId = intent.getStringExtra("communityId").toString()
        productId = intent.getStringExtra("productId").toString()

        bindProduct()


        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun bindProduct() {
        FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(productId).get().addOnSuccessListener {
            productModel = it.toObject(ProductModel::class.java)!!
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

            ContentUtil().verifyCommunityContentAvailability(productModel.ownerId, productModel.communityId) { isAvailable ->
                if (isAvailable){
                    binding.feedTimestampTV.text = DateAndTimeUtil().getTimeAgo(productModel.createdTimestamp)
                    binding.productNameTV.text = productModel.productName
                    binding.descTV.text = productModel.productDesc
                    binding.priceTV.text = currencyFormat.format(productModel.price)

                    FirebaseUtil().targetUserDetails(productModel.ownerId).get().addOnSuccessListener { result ->
                        val user = result.toObject(UserModel::class.java)!!
                        binding.ownerUsernameTV.text = user.username
                        AppUtil().setUserProfilePic(this, user.userID, binding.profileCIV)

                        if (!productModel.available){
                            //product is no longer available
                            val lessLightTeal = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
                            binding.productAvailableTV.text = "This product is no longer available"
                            binding.productAvailableTV.setTextColor(lessLightTeal)
                        } else {
                            //product is no longer available
                            val green = ContextCompat.getColor(this, R.color.green)
                            binding.productAvailableTV.text = "Available"
                            binding.productAvailableTV.setTextColor(green)
                        }


                        //Bottom layout buttons
                        if (productModel.ownerId != FirebaseUtil().currentUserUid()){
                            //For Customer
                            if (productModel.available){
                                binding.productAvailableBtn.visibility = View.GONE
                                binding.messageUserBtn.visibility = View.VISIBLE
                                binding.messageUserBtn.setOnClickListener {
                                    val intent = Intent(this, Chatroom::class.java)
                                    intent.putExtra("chatroomName", user.fullName)
                                    intent.putExtra("userID", user.userID)
                                    intent.putExtra("chatroomType", "direct_message")
                                    intent.putExtra("productId", productModel.productId)
                                    intent.putExtra("communityIdOfPost", productModel.communityId)
                                    startActivity(intent)
                                }
                            } else {
                                binding.productAvailableBtn.visibility = View.GONE
                                binding.messageUserBtn.visibility = View.GONE
                            }
                        } else {
                            //For Seller
                            binding.productAvailableBtn.visibility = View.VISIBLE
                            binding.messageUserBtn.visibility = View.GONE

                            if (productModel.available){
                                //set the product unavailable button
                                binding.productAvailableBtn.text = "Set the product unavailable"
                                binding.productAvailableBtn.setOnClickListener {
                                    FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(productId).update("available", false).addOnSuccessListener {
                                        onRefresh()
                                    }
                                }
                            } else {
                                //set the product available button
                                binding.productAvailableBtn.text = "Set the product available"
                                binding.productAvailableBtn.setOnClickListener {
                                    FirebaseUtil().retrieveCommunityMarketCollection(communityId).document(productId).update("available", true).addOnSuccessListener {
                                        onRefresh()
                                    }
                                }
                            }
                        }

                        binding.kebabMenuBtn.setOnClickListener {
                            DialogUtil().openMenuDialog(this, layoutInflater, "Product", productModel.productId,
                                productModel.ownerId, productModel.communityId){closeCurrentActivity ->
                                if (closeCurrentActivity){
                                    Handler().postDelayed({
                                        onBackPressed()
                                    }, 2000)
                                }
                            }
                        }

                    }

                    if (productModel.imageList.isNotEmpty())
                        bindContent(productModel.imageList)

                } else {
                    hideContent()
                }

            }
        }
    }

    private fun bindContent(contentList: List<String>){
        for (content in contentList){
            //Identifies the content
            binding.contentLayout.addView(ContentUtil().getImageView(this, content))
            binding.contentLayout.addView(ContentUtil().createSpaceView(this))
        }
    }


    private fun hideContent(){
        binding.scrollViewLayout.visibility = View.GONE
        binding.bottomToolbar.visibility = View.INVISIBLE
        binding.divider2.visibility = View.INVISIBLE
        binding.contentNotAvailableLayout.visibility = View.VISIBLE
    }

    override fun onRefresh() {
        Handler().postDelayed({
            bindProduct()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }

}