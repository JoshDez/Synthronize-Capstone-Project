package com.example.synthronize

import UserLastSeenUpdater
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.example.synthronize.databinding.ActivitySplashBinding
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.toObject

class Splash : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var communityId = ""
    private var contentId = ""
    private var contentType = ""
    private var isUserAdmin = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(FirebaseUtil().isLoggedIn() && intent.extras != null){
            //from notification
            communityId = intent.getStringExtra("communityId").toString()
            contentId = intent.getStringExtra("contentId").toString()
            contentType = intent.getStringExtra("contentType").toString()

            //shows the loading screen
            Handler().postDelayed({
                //open the main activity first
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                startActivity(intent)

                val communityContentTypes = listOf("Post", "Competition", "File")
                if (communityContentTypes.contains(contentType)){

                    //NOTIFICATIONS INSIDE COMMUNITY
                    FirebaseUtil().currentUserDetails().get().addOnCompleteListener {user ->
                        if (user.result.exists()){
                            val currentUserModel = user.result.toObject(UserModel::class.java)!!
                            FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnCompleteListener {community ->
                                if (community.result.exists()){
                                    val communityModel = community.result.toObject(CommunityModel::class.java)!!
                                    //assign current user role
                                    for (member in communityModel.communityMembers){
                                        if (currentUserModel.userType == "AppAdmin"){
                                            //User is AppAdmin
                                            isUserAdmin = true
                                        } else if (member.value == "Admin" && currentUserModel.userID == member.key){
                                            //User is Admin
                                            isUserAdmin = true
                                        }
                                    }
                                    //then head to the content
                                    when(contentType){
                                        "Post" -> {
                                            viewPost()
                                        }
                                        "File" -> {
                                            viewFile()
                                        }
                                        "Competition" -> {
                                            viewCompetition()
                                        }
                                    }

                                }
                            }

                        }
                    }
                } else {
                    //NOTIFICATIONS OUTSIDE COMMUNITY
                    //TODO
                }
            }, 1000)


        } else {
            Handler().postDelayed({
                intent = if(FirebaseUtil().isLoggedIn()){
                    //starts updating user last seen
                    UserLastSeenUpdater().startUpdating()
                    //head to main activity
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, Login::class.java)
                }

                startActivity(intent)
                this.finish()
            }, 1000)
        }
    }

    private fun viewPost(){
        val intent = Intent(this, ViewPost::class.java)
        intent.putExtra("communityId", communityId)
        intent.putExtra("postId", contentId)
        startActivity(intent)
    }


    private fun viewFile(){
        val intent = Intent(this, ViewFile::class.java)
        intent.putExtra("communityId", communityId)
        intent.putExtra("fileId", contentId)
        intent.putExtra("contentType", "File")
        startActivity(intent)
    }

    private fun viewCompetition(){
        val intent = Intent(this, ViewCompetition::class.java)
        intent.putExtra("communityId", communityId)
        intent.putExtra("competitionId", contentId)
        intent.putExtra("isUserAdmin", isUserAdmin)
        startActivity(intent)
    }
}