package com.example.synthronize.utils
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.CreatePost
import com.example.synthronize.adapters.ChatroomAdapter
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.DialogForwardContentBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.PostModel
import com.example.synthronize.model.UserModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class DialogUtil: OnItemClickListener {

    //OPENS DIALOG FOR SEND POST
    fun openForwardContentDialog(context:Context, inflater: LayoutInflater, postId:String, communityIdOfPost:String){
        //prepares dialog
        val dialogForwardContentBinding = DialogForwardContentBinding.inflate(inflater)
        val forwardContentDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogForwardContentBinding.root))
            .setMargin(20, 300, 20, 0)
            .setCancelable(true)
            .setExpanded(false)
            .setGravity(Gravity.BOTTOM)
            .create()

        //query
        val myQuery: Query = FirebaseUtil().retrieveAllChatRoomReferences()
            .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
        val options:FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(myQuery, ChatroomModel::class.java).build()
        val chatroomAdapter = ChatroomAdapter(context, options, postId, communityIdOfPost)

        dialogForwardContentBinding.resultsRV.layoutManager = LinearLayoutManager(context)
        dialogForwardContentBinding.resultsRV.adapter = chatroomAdapter
        chatroomAdapter.startListening()

        dialogForwardContentBinding.headerLayout.setOnClickListener {
            forwardContentDialog.dismiss()
        }
        dialogForwardContentBinding.downBtn.setOnClickListener {
            forwardContentDialog.dismiss()
        }

        forwardContentDialog.show()
    }

    //OPENS DIALOG FOR FEED MENU
    fun openMenuDialog(context:Context, inflater: LayoutInflater, postModel:PostModel){
        FirebaseUtil().retrieveCommunityDocument(postModel.communityId).get().addOnSuccessListener {
            val communityModel = it.toObject(CommunityModel::class.java)!!
            val menuDialogBinding = DialogMenuBinding.inflate(inflater)
            val menuDialog = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(menuDialogBinding.root))
                .setCancelable(true)
                .setExpanded(false)
                .setGravity(Gravity.CENTER)
                .create()


            if (postModel.ownerId == FirebaseUtil().currentUserUid() ||
                AppUtil().isIdOnList(AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin"), FirebaseUtil().currentUserUid())){
                //TODO for moderator
                //displays delete post option if the user is the owner or admin of the community
                menuDialogBinding.option1.visibility = View.VISIBLE
                menuDialogBinding.optiontitle1.text = "Delete Post"
                menuDialogBinding.optiontitle1.setOnClickListener {
                    menuDialog.dismiss()
                    Handler().postDelayed({
                        val warningDialogBinding = DialogWarningMessageBinding.inflate(inflater)
                        val warningDialog = DialogPlus.newDialog(context)
                            .setContentHolder(ViewHolder(warningDialogBinding.root))
                            .setCancelable(true)
                            .setMargin(100, 800, 100, 800)
                            .setGravity(Gravity.CENTER)
                            .create()

                        warningDialogBinding.titleTV.text = "Delete Post"
                        warningDialogBinding.messageTV.text = "Do you want to delete this post?"
                        warningDialogBinding.yesBtn.setOnClickListener {
                            //deletes post from firebase firestore database
                            FirebaseUtil().retrieveCommunityFeedsCollection(postModel.communityId).document(postModel.postId).delete()
                            //deletes content from firebase storage
                            for (content in postModel.contentList){
                                FirebaseUtil().retrieveCommunityContentImageRef(content).delete()
                            }
                            warningDialog.dismiss()
                        }
                        warningDialogBinding.NoBtn.setOnClickListener {
                            warningDialog.dismiss()
                        }

                        warningDialog.show()

                    }, 500)
                }

                //displays edit post option if the user is the owner or admin of the community
                menuDialogBinding.option2.visibility = View.VISIBLE
                menuDialogBinding.optiontitle2.text = "Edit Post"
                menuDialogBinding.optiontitle2.setOnClickListener {
                    menuDialog.dismiss()
                    Handler().postDelayed({
                        val intent = Intent(context, CreatePost::class.java)
                        intent.putExtra("postId", postModel.postId)
                        intent.putExtra("communityId", postModel.communityId)
                        context.startActivity(intent)
                    }, 500)
                }
            } else {
                menuDialogBinding.option1.visibility = View.VISIBLE
                menuDialogBinding.optiontitle1.text = "Report Post"
                menuDialogBinding.option1.setOnClickListener {
                    Toast.makeText(context, "To be implemented", Toast.LENGTH_SHORT).show()
                }
            }
            menuDialog.show()
        }
    }


    //OPENS COMMUNITY PREVIEW DIALOG
    fun openCommunityPreviewDialog(context:Context, layoutInflater: LayoutInflater, communityModel: CommunityModel){
        val dialogPlusBinding = DialogCommunityPreviewBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogPlusBinding.root))
            .setGravity(Gravity.CENTER)
            .setExpanded(false)
            .create()

        //Community details
        dialogPlusBinding.communityNameTV.text = communityModel.communityName
        dialogPlusBinding.communityDescriptionTV.text = communityModel.communityDescription
        dialogPlusBinding.totalMembersCountTV.text = "${communityModel.communityMembers.size}"
        getFriendsJoinedCount(communityModel.communityMembers.keys.toList()){count->
            dialogPlusBinding.friendsJoinedCountTV.text = count.toString()
        }
        dialogPlusBinding.createdDateTV.text =  DateAndTimeUtil().formatTimestampToDate(communityModel.communityCreatedTimestamp)
        AppUtil().setCommunityProfilePic(context, communityModel.communityId, dialogPlusBinding.communityProfileCIV)
        AppUtil().setCommunityBannerPic(context, communityModel.communityId, dialogPlusBinding.communityBannerIV)
        AppUtil().changeCommunityButtonStates(context, dialogPlusBinding.communityActionBtn, communityModel.communityId)

        Handler().postDelayed({
            dialogPlus.show()
        }, 500)
    }

    private fun getFriendsJoinedCount(membersList: List<String>, callback: (Int) -> Unit) {
        var count = 0
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)!!
            for (friend in userModel.friendsList){
                if (AppUtil().isIdOnList(membersList, friend)){
                    count += 1
                }
            }
            callback(count)
        }.addOnFailureListener {
            callback(count)
        }

    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        TODO("Not yet implemented")
    }

}