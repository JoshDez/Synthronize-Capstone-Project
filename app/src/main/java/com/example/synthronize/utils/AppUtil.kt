package com.example.synthronize.utils
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.bumptech.glide.request.RequestOptions
import com.example.synthronize.MainActivity
import com.example.synthronize.R
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import de.hdodenhof.circleimageview.CircleImageView


class AppUtil {
    //For Images
    fun setUserProfilePic(context:Context, uid: String, civ:CircleImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveUserProfilePicRef(uid))
            .error(R.drawable.profile_not_selected)
            .apply(RequestOptions.circleCropTransform())
            //image view
            .into(civ)
    }
    fun setUserCoverPic(context: Context, uid: String, cover:ImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveUserCoverPicRef(uid))
            //image view
            .into(cover)
    }

    fun setCommunityProfilePic(context:Context, uid: String, civ:CircleImageView){
        GlideApp.with(context)
            //storage reference
            .load(FirebaseUtil().retrieveCommunityProfilePicRef(uid))
            .error(R.drawable.community_not_selected)
            .apply(RequestOptions.circleCropTransform())
            //image view
            .into(civ)
    }

    //For Heading Back to Main Activity
    fun headToMainActivity(context: Context, fragment:String = "", delay:Long = 0, communityId: String = "") {
        Handler().postDelayed({
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("fragment", fragment)
            intent.putExtra("communityId", communityId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }, delay)
    }

    //For Slicing Strings
    fun sliceMessage(string:String, intCap:Int):String{
        if (string.length > intCap){
            return "${string.slice(0..intCap)}..."
        } else {
            return string
        }
    }

    //checks if user is on list
    fun isUserOnList(list: List<String>, userId:String):Boolean{
        for (id in list){
            if (id == userId){
                return true
            }
        }
        return false
    }

    //OPENS COMMUNITY PREVIEW DIALOG
    fun openCommunityPreviewDialog(context:Context, layoutInflater: LayoutInflater, communityModel: CommunityModel){
        val dialogPlusBinding = DialogCommunityPreviewBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogPlusBinding.root))
            .setGravity(Gravity.CENTER)
            .create()

        //Community details
        dialogPlusBinding.communityNameTV.text = communityModel.communityName
        dialogPlusBinding.communityDescriptionTV.text = communityModel.communityDescription
        dialogPlusBinding.totalMembersCountTV.text = "${communityModel.communityMembers.size}"
        //TODO: dialogBinding.friendsJoinedCountTV.text = "${getFriendsJoinedCount()}"
        //TODO: dialogBinding.createdDateTV.text = communityModel.communityCreatedTimestamp
        setCommunityProfilePic(context, communityModel.communityId, dialogPlusBinding.communityProfileCIV)

        //ON SET LISTENER BUTTONS
        //Join Community Button
        dialogPlusBinding.joinCommunityBtn.setOnClickListener {
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
        dialogPlusBinding.cancelRequestBtn.setOnClickListener {
            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                .update("joinRequestList", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                .addOnSuccessListener {
                    appearButton(dialogPlusBinding.requestToJoinBtn, dialogPlusBinding)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                }
        }
        //Request To Join Button
        dialogPlusBinding.requestToJoinBtn.setOnClickListener {
            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                .update("joinRequestList", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                .addOnSuccessListener {
                    appearButton(dialogPlusBinding.cancelRequestBtn, dialogPlusBinding)
                    Toast.makeText(context, "Please wait for the admin to take you in", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error occur please try again", Toast.LENGTH_SHORT).show()
                }
        }
        //Enter Button
        dialogPlusBinding.enterBtn.setOnClickListener {
            AppUtil().headToMainActivity(context, "community", 0, communityModel.communityId)
            Toast.makeText(context, "Clicked enter", Toast.LENGTH_SHORT).show()
        }

        //SET COMMUNITY TYPE
        if (communityModel.communityType == "Private"){
            //FOR PRIVATE COMMUNITY TYPE
            if (AppUtil().isUserOnList(communityModel.joinRequestList, FirebaseUtil().currentUserUid())){
                //checks if user already requested to join
                appearButton(dialogPlusBinding.cancelRequestBtn, dialogPlusBinding)
            } else if (AppUtil().isUserOnList(communityModel.communityMembers, FirebaseUtil().currentUserUid())){
                //checks if user is already a member
                appearButton(dialogPlusBinding.enterBtn, dialogPlusBinding)
            } else {
                //user has not yet joined the group
                appearButton(dialogPlusBinding.requestToJoinBtn, dialogPlusBinding)
            }
        } else {
            //FOR PUBLIC COMMUNITY TYPE
            if (AppUtil().isUserOnList(communityModel.communityMembers, FirebaseUtil().currentUserUid())){
                //checks if user is already a member
                appearButton(dialogPlusBinding.enterBtn, dialogPlusBinding)
            } else {
                //user has not yet joined the group
                appearButton(dialogPlusBinding.joinCommunityBtn, dialogPlusBinding)
            }
        }

        Handler().postDelayed({
            dialogPlus.show()
        }, 500)
    }
    private fun getFriendsJoinedCount(membersList: List<String>, callback: (Int) -> Unit) {
        var count = 0
        FirebaseUtil().currentUserDetails().get().addOnSuccessListener {
            val userModel = it.toObject(UserModel::class.java)
            //TODO: friends joined
        }

    }

    private fun appearButton(button: MaterialButton, dialogPlusBinding: DialogCommunityPreviewBinding){
        dialogPlusBinding.joinCommunityBtn.visibility = View.GONE
        dialogPlusBinding.enterBtn.visibility = View.GONE
        dialogPlusBinding.cancelRequestBtn.visibility = View.GONE
        dialogPlusBinding.requestToJoinBtn.visibility = View.GONE
        when(button){
            dialogPlusBinding.joinCommunityBtn -> dialogPlusBinding.joinCommunityBtn.visibility = View.VISIBLE
            dialogPlusBinding.enterBtn -> dialogPlusBinding.enterBtn.visibility = View.VISIBLE
            dialogPlusBinding.cancelRequestBtn -> dialogPlusBinding.cancelRequestBtn.visibility = View.VISIBLE
            dialogPlusBinding.requestToJoinBtn -> dialogPlusBinding.requestToJoinBtn.visibility = View.VISIBLE
        }
    }
}