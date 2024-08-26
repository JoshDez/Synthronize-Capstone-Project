package com.example.synthronize

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.InstructionsAdapter
import com.example.synthronize.databinding.ActivityViewCompetitionBinding
import com.example.synthronize.interfaces.OnInstructionModified
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.InstructionModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject

class ViewCompetition : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener, OnInstructionModified {
    private lateinit var binding:ActivityViewCompetitionBinding
    private lateinit var instructionsAdapter:InstructionsAdapter
    private lateinit var submissionsAdapter:InstructionsAdapter

    private var communityId = ""
    private var competitionId = ""
    private var currentTab = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewCompetitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        competitionId = intent.getStringExtra("competitionId").toString()
        currentTab = "instructions"
        bindCompetition()

        binding.viewCompetitionRefresh.setOnRefreshListener(this)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun bindCompetition(){
        binding.viewCompetitionRefresh.isRefreshing = true
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnSuccessListener {community->
            val communityModel = community.toObject(CommunityModel::class.java)!!
            AppUtil().setCommunityProfilePic(this, communityModel.communityId, binding.communityProfileCIV)
            binding.communityNameTV.text = communityModel.communityName

            FirebaseUtil().retrieveCommunityCompetitionsCollection(communityModel.communityId).document(competitionId).get().addOnSuccessListener { competition ->
                val competitionModel = competition.toObject(CompetitionModel::class.java)!!
                binding.createdTimestampTV.text = DateAndTimeUtil().getTimeAgo(competitionModel.createdTimestamp, false)
                binding.competitionNameTV.text = competitionModel.competitionName
                binding.descTV.text = competitionModel.description
                binding.rewardsTV.text = competitionModel.rewards
                binding.joinCompetitionBtn.text = "Join Competition (${competitionModel.contestants.keys.size})"
                FirebaseUtil().targetUserDetails(competitionModel.ownerId).get().addOnSuccessListener {
                    binding.hostNameTV.text = "created by ${it.getString("fullName")}"
                }

                navigate(currentTab, competitionModel)

                binding.viewCompetitionRefresh.isRefreshing = false

                //buttons
                binding.instructionsBtn.setOnClickListener {
                    navigate("instructions", competitionModel)
                }
                binding.submissionsBtn.setOnClickListener {
                    navigate("submissions", competitionModel)
                }
                if (!AppUtil().isIdOnList(competitionModel.contestants.keys, FirebaseUtil().currentUserUid())){
                    binding.joinCompetitionBtn.setOnClickListener {
                        val updates = hashMapOf<String, Any>(
                            "contestants.${FirebaseUtil().currentUserUid()}" to ""
                        )
                        FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(competitionId).update(updates).addOnSuccessListener {
                            onRefresh()
                        }
                    }
                } else {
                    //TODO
                    binding.joinCompetitionBtn.visibility = View.GONE
                }
            }
        }
    }


    private fun navigate(tab:String, competitionModel:CompetitionModel){
        val unselectedColor = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(this, R.color.light_teal)
        binding.instructionsRV.visibility = View.GONE
        binding.submissionsRV.visibility = View.GONE
        binding.instructionsBtn.setTextColor(unselectedColor)
        binding.submissionsBtn.setTextColor(unselectedColor)

        if (tab == "instructions"){
            binding.instructionsRV.visibility = View.VISIBLE
            binding.instructionsBtn.setTextColor(selectedColor)
            currentTab = "instructions"
            if (!::instructionsAdapter.isInitialized)
                setupInstructions(competitionModel.instruction)


        } else if (tab == "submissions") {
            binding.submissionsRV.visibility = View.VISIBLE
            binding.submissionsBtn.setTextColor(selectedColor)
            currentTab = "submissions"
            if (!::submissionsAdapter.isInitialized)
                setupSubmissions()
        }
    }

    private fun setupSubmissions() {
        //TODO
    }

    private fun setupInstructions(map:HashMap<String, List<String>>) {
        val instructionMap:HashMap<String, InstructionModel> = hashMapOf()
        val orderedKeys = map.keys.toList().sorted()

        for (key in orderedKeys){
            val content = map.getValue(key)
            val instructionModel = InstructionModel(
                instruction = content[0],
                imageName = content[1],
                saved = true
            )
            instructionMap[key] = instructionModel
        }
        binding.instructionsRV.layoutManager = LinearLayoutManager(this)
        instructionsAdapter = InstructionsAdapter(this, instructionMap, this)
        binding.instructionsRV.adapter = instructionsAdapter
        binding.instructionsRV.smoothScrollToPosition(instructionMap.size)
    }

    override fun onRefresh() {
        binding.viewCompetitionRefresh.isRefreshing = true
        Handler().postDelayed({
            bindCompetition()
        },1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }


    //Unnecessary methods (needed for the instruction adapter to work)
    override fun deleteInstruction(key: String) {
    }
    override fun modifiedInstruction(key: String, instructionModel: InstructionModel) {
    }
    override fun openImageLauncher(key: String, instruction: String) {
    }
}