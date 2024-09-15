package com.example.synthronize

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.synthronize.adapters.InstructionsAdapter
import com.example.synthronize.adapters.RulesAdapter
import com.example.synthronize.databinding.ActivityCommunityRulesBinding
import com.example.synthronize.databinding.DialogLoadingBinding
import com.example.synthronize.interfaces.OnCommunityRuleModified
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.InstructionModel
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.toObject
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CommunityRules : AppCompatActivity(), OnCommunityRuleModified {
    private lateinit var binding:ActivityCommunityRulesBinding
    private lateinit var rulesAdapter: RulesAdapter
    private var ruleMap:HashMap<String, String> = HashMap()
    private var communityId = ""
    private var ctr = 0
    private var toEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        toEdit = intent.getBooleanExtra("toEdit", false)

        bindRules()
    }

    private fun bindRules(){
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnCompleteListener {
            if (it.isSuccessful){
                val communityModel = it.result.toObject(CommunityModel::class.java)!!

                for (rule in communityModel.communityRules){
                    ruleMap[ctr.toString()] = rule
                    ctr += 1
                }

                setupRulesRV()
                bindButtons()
            }
        }
    }

    private fun bindButtons(){
        if (toEdit){
            binding.bottomNavigation.visibility = View.VISIBLE
            binding.addRuleBtn.visibility = View.VISIBLE

            binding.saveBtn.setOnClickListener {
                if (checkIfCanAddRule()){
                    //adds new instruction
                    saveCommunityRules()
                } else {
                    Toast.makeText(this, "Please save the rule first", Toast.LENGTH_SHORT).show()
                }
            }

            binding.addRuleBtn.setOnClickListener {
                if (checkIfCanAddRule()){
                    //adds new instruction
                    ruleMap[ctr.toString()] = ""
                    setupRulesRV()
                    ctr += 1
                } else {
                    Toast.makeText(this, "Please save the rule first", Toast.LENGTH_SHORT).show()
                }
            }

        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun saveCommunityRules() {
        val dialogLoadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogLoadingBinding.root))
            .setCancelable(false)
            .setBackgroundColorResId(R.color.transparent)
            .setGravity(Gravity.CENTER)
            .create()

        dialogLoadingBinding.messageTV.text = "Saving..."

        loadingDialog.show()

        val keys = ruleMap.keys.toList().sorted()
        val communityRules:ArrayList<String> = arrayListOf()
        for (key in keys){
            communityRules.add(ruleMap[key]!!)
        }
        FirebaseUtil().retrieveCommunityDocument(communityId).update("communityRules", communityRules).addOnCompleteListener {
            if (it.isSuccessful){
                loadingDialog.dismiss()
                Toast.makeText(this, "Successfully updated community rules", Toast.LENGTH_SHORT).show()
                onBackPressed()
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, "Failed to update community rules", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRulesRV(){
        binding.rulesRV.layoutManager = LinearLayoutManager(this)
        rulesAdapter = RulesAdapter(this, ruleMap, this, toEdit)
        binding.rulesRV.adapter = rulesAdapter
        binding.rulesRV.smoothScrollToPosition(ruleMap.size)
    }


    private fun checkIfCanAddRule(): Boolean {
        for (rule in ruleMap){
            val ruleValue = rule.value
            if (ruleValue.isEmpty()){
                return false
            }
        }
        return true
    }

    override fun deleteRule(key: String) {
        ruleMap.remove(key)
        setupRulesRV()
    }

    override fun modifiedRule(key: String, rule: String) {
        ruleMap[key] = rule
        setupRulesRV()
    }
}