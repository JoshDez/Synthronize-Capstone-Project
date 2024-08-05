package com.example.synthronize

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.synthronize.databinding.ActivityCommunitySettingsBinding
import com.example.synthronize.databinding.DialogWarningMessageBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CommunitySettings : AppCompatActivity() {
    private lateinit var binding:ActivityCommunitySettingsBinding
    private lateinit var communityModel: CommunityModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val communityId = intent.getStringExtra("communityId").toString()
        val isUserAdmin = intent.getBooleanExtra("isUserAdmin", false)

        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {
            communityModel = it.toObject(CommunityModel::class.java)!!
            navigate("general")
        }

        if (isUserAdmin){
            binding.navigationLayout.visibility = View.VISIBLE
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.adminBtn.setOnClickListener {
            navigate("admin")
        }

        binding.generalBtn.setOnClickListener {
            navigate("general")
        }

    }

    private fun navigate(tab:String){
        val unselectedColor = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(this, R.color.light_teal)
        binding.generalBtn.setTextColor(unselectedColor)
        binding.adminBtn.setTextColor(unselectedColor)
        binding.generalLayout.visibility = View.GONE
        binding.adminLayout.visibility = View.GONE


        if (tab == "general"){
            setupGeneralLayout()
            binding.generalBtn.setTextColor(selectedColor)
        }else if (tab == "admin") {
            setupAdminLayout()
            binding.adminBtn.setTextColor(selectedColor)
        }
    }

    private fun setupAdminLayout() {
        binding.adminLayout.visibility = View.VISIBLE

        if (communityModel.communityType == "Private"){
            binding.viewJoinRequestsBtn.visibility = View.VISIBLE
            binding.viewJoinRequestsBtn.setOnClickListener {
                val intent = Intent(this, Requests::class.java)
                intent.putExtra("communityId", communityModel.communityId)
                startActivity(intent)
            }
        }

        binding.bannedUsersBtn.setOnClickListener {
            val intent = Intent(this, BanAndBlockList::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.viewCommunityReportsBtn.setOnClickListener {
            val intent = Intent(this, Reports::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.editCommunityDetailsBtn.setOnClickListener {
            val intent = Intent(this, EditCommunity::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }
        binding.editCommunityRulesBtn.setOnClickListener {
            Toast.makeText(this, "To be implemented", Toast.LENGTH_SHORT).show()
        }
        binding.deleteCommunityBtn.setOnClickListener {
            val warningBinding = DialogWarningMessageBinding.inflate(layoutInflater)
            val warningDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(warningBinding.root))
                .setMargin(50, 800, 50, 800)
                .setGravity(Gravity.CENTER)
                .create()
            warningBinding.titleTV.text = "Warning"
            warningBinding.messageTV.text = "Do you want to permanently delete this community?"
            warningBinding.yesBtn.setOnClickListener {
                warningDialog.dismiss()
                FirebaseUtil().retrieveCommunityDocument(communityModel.communityId).delete().addOnSuccessListener {
                    Toast.makeText(this, "The community is deleted", Toast.LENGTH_SHORT).show()
                    deleteAllCommunityChannels()
                    AppUtil().headToMainActivity(this)
                }
                //TODO delete all channels
            }
            warningBinding.NoBtn.setOnClickListener {
                warningDialog.dismiss()
            }
            warningDialog.show()
        }
    }

    private fun setupGeneralLayout() {
        binding.generalLayout.visibility = View.VISIBLE

        //Common Binds
        binding.communityNameTV.text = communityModel.communityName
        binding.communityCodeEdtTxt.setText(communityModel.communityCode)
        AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.userProfileCIV)
        AppUtil().setCommunityBannerPic(this, communityModel.communityId, binding.communityBannerIV)

        if (communityModel.communityDescription.isNotEmpty()){
            binding.communityDescriptionTV.visibility = View.VISIBLE
            binding.communityDescriptionTV.text = communityModel.communityDescription
        }

        binding.leaveCommunityBtn.setOnClickListener {
            //get the list of userIDs of Admins
            val admin = AppUtil().extractKeysFromMapByValue(communityModel.communityMembers, "Admin")

            if (admin.size == 1 && admin.contains(FirebaseUtil().currentUserUid())){
                //shows a toast message
                Toast.makeText(this, "The community should at least have 1 admin available", Toast.LENGTH_SHORT).show()
            } else {
                //show error message
                val dialogPlusBinding = DialogWarningMessageBinding.inflate(layoutInflater)
                val dialogPlus = DialogPlus.newDialog(this)
                    .setContentHolder(ViewHolder(dialogPlusBinding.root))
                    .setGravity(Gravity.CENTER)
                    .setMargin(50, 800, 50, 800)
                    .setCancelable(true)
                    .create()

                dialogPlusBinding.titleTV.text = "Warning!"
                dialogPlusBinding.messageTV.text = "Do you want to leave this community?"
                dialogPlusBinding.yesBtn.setOnClickListener {
                    //removes user from community channels before leaving the community
                    FirebaseUtil().removeUserFromAllCommunityChannels(communityModel.communityId, FirebaseUtil().currentUserUid()){isSuccessful ->
                        if (isSuccessful){
                            //Removes user from community members
                            val updatedMap = mapOf(
                                "communityMembers.${FirebaseUtil().currentUserUid()}" to FieldValue.delete()
                            )
                            FirebaseUtil().retrieveCommunityDocument(communityModel.communityId)
                                .update(updatedMap).addOnSuccessListener {
                                    AppUtil().headToMainActivity(this)
                                }
                        } else {
                            Toast.makeText(this, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialogPlusBinding.NoBtn.setOnClickListener {
                    dialogPlus.dismiss()
                }
                dialogPlus.show()
            }


        }

        binding.copyCodeBtn.setOnClickListener {
            val textToCopy = binding.communityCodeEdtTxt.text.toString()
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Copied Text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.viewMembersBtn.setOnClickListener {
            val intent = Intent(applicationContext, Members::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            startActivity(intent)
        }

        binding.reportsMadeBtn.setOnClickListener {
            val intent = Intent(applicationContext, Reports::class.java)
            intent.putExtra("communityId", communityModel.communityId)
            intent.putExtra("isPersonalReport", true)
            startActivity(intent)
        }

        binding.reportCommunityBtn.setOnClickListener {
            DialogUtil().openReportDialog(this, layoutInflater, "Community",communityModel.communityId)
        }
    }


    private fun deleteAllCommunityChannels(){
        FirebaseUtil().retrieveAllChatRoomReferences()
            .whereEqualTo("communityId", communityModel.communityId).get().addOnSuccessListener {channels ->
                for (channel in channels.documents){
                    val chatModel = channel.toObject(ChatroomModel::class.java)!!
                    FirebaseUtil().retrieveChatRoomReference(chatModel.chatroomId).delete()
                }
            }
    }
}