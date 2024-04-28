package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.databinding.ItemProfileBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil

class SearchUserAdapter(private val context: Context, options: FirestoreRecyclerOptions<UserModel>,  private val listener: OnItemClickListener,
                        //for user selecting
                        private val toCheckUser:Boolean = false,
                        private val selectedUserList:ArrayList<String> = ArrayList()):
    FirestoreRecyclerAdapter<UserModel, SearchUserAdapter.UserViewHolder>(options) {

    private var totalItems = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProfileBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: UserModel) {
        totalItems += 1
        holder.bind(model)
    }

    fun getTotalItems():Int{
        return totalItems
    }

    inner class UserViewHolder(private val binding: ItemProfileBinding, private val context: Context): RecyclerView.ViewHolder(binding.root){

        fun bind(model: UserModel){

            AppUtil().setUserProfilePic(context,model.userID, binding.userCircleImageView)
            if (model.username.isNotEmpty())
                binding.usernameTV.text = "@${model.username}"

            if (model.userID == FirebaseUtil().currentUserUid()){
                binding.userFullNameTV.text = "${model.fullName} (You)"
            }else {
                binding.userFullNameTV.text = model.fullName
                if (toCheckUser){
                    //display check box if user is not the current user
                    binding.selectUserCB.visibility = View.VISIBLE
                    for (userId in selectedUserList){
                        if (model.userID == userId){
                            binding.selectUserCB.isChecked = true
                        }
                    }
                    binding.selectUserCB.setOnClickListener {
                        if (binding.selectUserCB.isChecked){
                            listener.onUserClick(model.userID, true)
                        } else {
                            listener.onUserClick(model.userID, false)
                        }
                    }
                }else {
                    //display check box if user is not the current user
                    binding.userContainerRL.setOnClickListener{
                        val intent = Intent(context, OtherUserProfile::class.java)
                        intent.putExtra("userID", model.userID)
                        context.startActivity(intent)
                    }
                }


            }
        }
    }

}