package com.example.synthronize

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.synthronize.databinding.ActivityCreateCommunityBinding
import com.example.synthronize.databinding.ActivityEditProfileBinding
import com.example.synthronize.utils.FirebaseUtil

class CreateCommunity : AppCompatActivity() {
    private lateinit var binding:ActivityCreateCommunityBinding
    private var communityName: String = ""
    private var communityType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        changeLayout(0)
        bindFirstSectionLayout()
    }

    private fun changeLayout(layoutNo:Int){
        binding.firstSectionLayout.visibility = View.GONE

        when(layoutNo){
            0 -> binding.firstSectionLayout.visibility = View.VISIBLE
            1 -> binding.firstSectionLayout.visibility = View.GONE
        }
    }

    private fun bindFirstSectionLayout(){

        //replaces previousBtn with cancelBtn
        binding.cancelBtn.visibility = View.VISIBLE
        binding.previousBtn.visibility = View.GONE

        binding.communityNameEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = binding.communityNameEdtTxt.text.toString()
                isCommunityNameAvailable(name){isAvailable ->
                    if (isAvailable){
                        communityName = name
                    } else {
                        binding.communityNameEdtTxt.error = "Community name is not available"
                        communityName = ""
                    }
                }
            }

        })

        binding.publicRB.setOnClickListener {
            binding.privateRB.isChecked = false
            communityType = "Public"
        }

        binding.privateRB.setOnClickListener {
            binding.publicRB.isChecked = false
            communityType = "Private"

        }

        binding.nextBtn.setOnClickListener {
            if (communityName.isEmpty()){
                binding.communityNameEdtTxt.error = "Enter community name"
                Toast.makeText(this, "Enter community name", Toast.LENGTH_SHORT).show()
            } else if (communityType.isEmpty()){
                Toast.makeText(this, "Select community type", Toast.LENGTH_SHORT).show()
            } else {
                changeLayout(1)
            }
        }
        binding.cancelBtn.setOnClickListener {
            //TODO: add dialog
            this.finish()
        }
    }

    private fun isCommunityNameAvailable(name: String, callback: (Boolean) -> Unit) {
        FirebaseUtil().retrieveAllCommunityCollection().whereEqualTo("communityName", name).get().addOnSuccessListener{
            if (!it.isEmpty){
                //community name already exists
                callback(false)
            } else {
                callback(true)
            }
        }.addOnFailureListener {
                callback(false)
        }
    }

}