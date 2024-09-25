package com.example.synthronize.adapters

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.OtherUserProfile
import com.example.synthronize.ViewCompetition
import com.example.synthronize.ViewFile
import com.example.synthronize.ViewPost
import com.example.synthronize.databinding.ItemRequestBinding
import com.example.synthronize.interfaces.NotificationOnDataChange
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject

class NotificationsAdapter(private var context:Context,
                           private var sortedKeys:ArrayList<String> = arrayListOf(),
                           private val notifications:Map<String, List<String>> = HashMap(),
                           private val listener: NotificationOnDataChange
): RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationsAdapter.NotificationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRequestBinding.inflate(inflater, parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationsAdapter.NotificationViewHolder, position: Int) {
        holder.bind(sortedKeys[position])
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    inner class NotificationViewHolder(private val binding: ItemRequestBinding):
        RecyclerView.ViewHolder(binding.root){

        private lateinit var value:List<String>
        private lateinit var contentId:String
        private var isUserAdmin = false
        fun bind(key:String){
            //key: contentId  value: List(userId, action, repeatedAction, contentType, communityId, timestamp)
            value = notifications.getValue(key)
            contentId = key

            FirebaseUtil().targetUserDetails(value[0]).get().addOnCompleteListener {
                if (it.isSuccessful){
                    val user = it.result.toObject(UserModel::class.java)!!
                    AppUtil().setUserProfilePic(context, user.userID, binding.profileCIV)

                    //adds the name of the user to the notification message
                    binding.requestTV.text = "${user.fullName} "

                    try {
                        //converts timestamp to time ago
                        val convertedTimestamp = DateAndTimeUtil().stringToTimestamp(value[5])
                        binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(convertedTimestamp)

                        val repeatedAction = value[2].toInt()-1
                        if (repeatedAction > 0){
                            //adds repeated action (number of people who did the same action to the content) to the notification message
                            binding.requestTV.text = binding.requestTV.text.toString() + "+ $repeatedAction others "
                        }
                    } catch (e:Exception){
                        Log.d("error", e.message.toString())
                    }


                    when(value[1]){
                        "Love" -> {
                            //adds action to the notification message
                            binding.requestTV.text = binding.requestTV.text.toString() + "loved "
                        }
                        "Comment" -> {
                            //adds action to the notification message
                            binding.requestTV.text = binding.requestTV.text.toString() + "commented on "
                        }
                        "Join" -> {
                            //adds action to the notification message
                            binding.requestTV.text = binding.requestTV.text.toString() + "joined "
                        }
                        "Share" -> {
                            //adds action to the notification message
                            binding.requestTV.text = binding.requestTV.text.toString() + "shared "
                        }
                    }

                    
                    val communityContentTypes = listOf("Post", "Competition", "File")
                    if (communityContentTypes.contains(value[3])){
                        bindCommunity()
                    }

                }
            }
        }

        private fun bindCommunity() {
            FirebaseUtil().currentUserDetails().get().addOnCompleteListener {user ->
                if (user.isSuccessful){
                    val myModel = user.result.toObject(UserModel::class.java)!!

                    FirebaseUtil().retrieveCommunityDocument(value[4]).get().addOnCompleteListener {community ->
                        if (community.isSuccessful){
                            val communityModel = community.result.toObject(CommunityModel::class.java)!!

                            //assign current user role
                            for (user in communityModel.communityMembers){
                                if (myModel.userType == "AppAdmin"){
                                    //User is AppAdmin
                                    isUserAdmin = true
                                } else if (user.value == "Admin" && myModel.userID == user.key){
                                    //User is Admin
                                    isUserAdmin = true
                                }
                            }

                            //Content type
                            when(value[3]){
                                "Post" -> {
                                    //adds content type to the notification message
                                    binding.requestTV.text = binding.requestTV.text.toString() + "your post in ${communityModel.communityName} "
                                    binding.requestContainerLayout.setOnClickListener {
                                        viewPost()
                                    }
                                }
                                "File" -> {
                                    //adds content type to the notification message
                                    binding.requestTV.text = binding.requestTV.text.toString() + "your file in ${communityModel.communityName} "
                                    binding.requestContainerLayout.setOnClickListener {
                                        headToViewFile()
                                    }
                                }
                                "Competition" -> {
                                    //adds content type to the notification message
                                    binding.requestTV.text = binding.requestTV.text.toString() + "your competition in ${communityModel.communityName} "
                                    binding.requestContainerLayout.setOnClickListener {
                                        headToCompetition()
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        private fun viewPost(){
            val intent = Intent(context, ViewPost::class.java)
            intent.putExtra("communityId", value[4])
            intent.putExtra("postId", contentId)
            context.startActivity(intent)
        }


        private fun headToViewFile(){
            val intent = Intent(context, ViewFile::class.java)
            intent.putExtra("communityId", value[4])
            intent.putExtra("fileId", contentId)
            intent.putExtra("contentType", "File")
            context.startActivity(intent)
        }

        private fun headToCompetition(){
            val intent = Intent(context, ViewCompetition::class.java)
            intent.putExtra("communityId", value[4])
            intent.putExtra("competitionId", contentId)
            intent.putExtra("isUserAdmin", isUserAdmin)
            context.startActivity(intent)
        }

    }

}