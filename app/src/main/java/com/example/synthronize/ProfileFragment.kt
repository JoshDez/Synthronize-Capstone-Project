package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.Timestamp
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentProfileBinding
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.ModelHandler

class ProfileFragment(private var mainBinding: ActivityMainBinding) : Fragment() {
    private lateinit var binding: FragmentProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "PROFILE"

        //checks if the fragment is already added to the activity
        if (isAdded){
            ModelHandler().retrieveUserModel(FirebaseUtil().currentUserUid()) {userModel ->
                bindUserDetails(userModel)
            }
        }
    }

    private fun bindUserDetails(userModel: UserModel) {
        binding.userNameTV.text = userModel.username
        binding.userDisplayNameTV.text = userModel.fullName
        binding.userDescriptionTV.text = userModel.description

        //binds userProfilePic
        AppUtil().setUserProfilePic(requireContext(), userModel.userID, binding.userProfileCIV)
        AppUtil().setUserCoverPic(requireContext(), userModel.userID, binding.userCoverIV)

        binding.editProfileBtn.setOnClickListener {
            val intent = Intent(requireContext(), EditProfile::class.java)
            intent.putExtra("userID", userModel.userID)
            startActivity(intent)
        }
    }
}