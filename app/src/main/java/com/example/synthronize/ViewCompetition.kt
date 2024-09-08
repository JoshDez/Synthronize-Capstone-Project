package com.example.synthronize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CompetitionFilesAdapter
import com.example.synthronize.adapters.InstructionsAdapter
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityViewCompetitionBinding
import com.example.synthronize.databinding.DialogSelectUserBinding
import com.example.synthronize.interfaces.OnInstructionModified
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.FileModel
import com.example.synthronize.model.InstructionModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class ViewCompetition : AppCompatActivity(), OnRefreshListener, OnNetworkRetryListener, OnInstructionModified, OnItemClickListener {
    private lateinit var binding:ActivityViewCompetitionBinding
    private lateinit var instructionsAdapter:InstructionsAdapter
    private lateinit var submissionsAdapter:CompetitionFilesAdapter
    private lateinit var resultsAdapter:SearchUserAdapter
    private lateinit var dialogPlusBinding:DialogSelectUserBinding
    private lateinit var searchUserAdapter:SearchUserAdapter
    private lateinit var selectedUsersAdapter:SearchUserAdapter
    private lateinit var competitionModel:CompetitionModel

    private var communityId = ""
    private var competitionId = ""
    private var currentTab = ""
    private var searchUserQuery = ""
    private var resultType = ""
    private var isUserAdmin = false
    private var isCompetitionDue = false
    private var selectedUserList:ArrayList<String> = arrayListOf()
    private var submittedContestants:ArrayList<String> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewCompetitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        competitionId = intent.getStringExtra("competitionId").toString()
        isUserAdmin = intent.getBooleanExtra("isUserAdmin", false)
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
                competitionModel = competition.toObject(CompetitionModel::class.java)!!
                binding.createdTimestampTV.text = DateAndTimeUtil().getTimeAgo(competitionModel.createdTimestamp, false)
                binding.competitionNameTV.text = competitionModel.competitionName
                binding.descTV.text = competitionModel.description
                binding.rewardsTV.text = competitionModel.rewards
                binding.actionBtn.text = "Join Competition (${competitionModel.contestants.keys.size})"
                binding.actionBtn.visibility = View.GONE
                resultType = competitionModel.results.keys.toList()[0]
                selectedUserList = ArrayList(competitionModel.results.getValue(resultType))

                FirebaseUtil().targetUserDetails(competitionModel.ownerId).get().addOnSuccessListener {
                    binding.hostNameTV.text = "created by ${it.getString("fullName")}"
                }

                DateAndTimeUtil().isTimestampDue(competitionModel.deadline){ isDue,daysLeft ->
                    isCompetitionDue = isDue
                    if (isCompetitionDue){
                        binding.remainingTimeTV.text = "The competition has ended"
                    } else {
                        binding.remainingTimeTV.text = "$daysLeft days before it closes"
                    }
                }

                navigate(currentTab)

                //buttons
                binding.instructionsBtn.setOnClickListener {
                    navigate("instructions")
                }
                binding.submissionsBtn.setOnClickListener {
                    navigate("submissions")
                }
                binding.resultBtn.setOnClickListener {
                    navigate("results")
                }

                getSubmittedContestants()

                //Bind action button
                if (isUserAdmin){
                    binding.actionBtn.visibility = View.VISIBLE
                    binding.actionBtn.text = "Select Winner"
                    binding.actionBtn.setOnClickListener {
                        openSelectUserDialog()
                    }
                } else if (!AppUtil().isIdOnList(competitionModel.contestants.keys, FirebaseUtil().currentUserUid())){
                    if (!isCompetitionDue){
                        binding.actionBtn.visibility = View.VISIBLE
                        binding.actionBtn.setOnClickListener {
                            val updates = hashMapOf<String, Any>(
                                "contestants.${FirebaseUtil().currentUserUid()}" to ""
                            )
                            FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId).document(competitionId).update(updates).addOnSuccessListener {
                                onRefresh()
                            }
                        }
                    }
                } else {
                    if (!isCompetitionDue){
                        binding.actionBtn.visibility = View.VISIBLE
                        if (competitionModel.contestants.getValue(FirebaseUtil().currentUserUid()).isEmpty()){
                            binding.actionBtn.text = "Submit File"
                            binding.actionBtn.setOnClickListener {
                                val intent = Intent(this, CreateUploadFile::class.java)
                                intent.putExtra("communityId", communityId)
                                intent.putExtra("competitionId", competitionId)
                                intent.putExtra("forCompetition", true)
                                startActivity(intent)
                            }
                        } else {
                            binding.actionBtn.visibility = View.GONE
                        }
                    }
                }


                binding.kebabMenuBtn.setOnClickListener {
                    DialogUtil().openMenuDialog(this, layoutInflater, "Competition", competitionModel.competitionId,
                        competitionModel.ownerId, competitionModel.communityId){closeCurrentActivity ->
                        if (closeCurrentActivity){
                            Handler().postDelayed({
                                onBackPressed()
                            }, 2000)
                        }
                    }
                }

                binding.viewCompetitionRefresh.isRefreshing = false
            }
        }
    }


    private fun navigate(tab:String){
        val unselectedColor = ContextCompat.getColor(this, R.color.less_saturated_light_teal)
        val selectedColor = ContextCompat.getColor(this, R.color.light_teal)
        binding.resultsTypeTV.visibility = View.GONE
        binding.instructionsRV.visibility = View.GONE
        binding.submissionsRV.visibility = View.GONE
        binding.resultsRV.visibility = View.GONE
        binding.instructionsBtn.setTextColor(unselectedColor)
        binding.submissionsBtn.setTextColor(unselectedColor)
        binding.resultBtn.setTextColor(unselectedColor)

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
                setupSubmissions(competitionModel.contestants)
        } else if (tab == "results") {
            binding.resultsTypeTV.visibility = View.VISIBLE
            binding.resultsRV.visibility = View.VISIBLE
            binding.resultBtn.setTextColor(selectedColor)
            currentTab = "results"
            if (!::submissionsAdapter.isInitialized)
                setupResults()
        }
    }

    private fun setupResults() {
        binding.viewCompetitionRefresh.isRefreshing = true

        //displays result type
        if (resultType == "All"){
            binding.resultsTypeTV.text = "Winners"
        } else {
            binding.resultsTypeTV.text = "Top ${resultType.split('/').last()} winners"
        }
        //setup rv
        if (selectedUserList.isNotEmpty()){
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", selectedUserList)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            // Add a listener to handle success or failure of the query
            myQuery.addSnapshotListener { _, e ->
                if (e != null) {
                    // Handle the error here (e.g., log the error or show a message to the user)
                    Log.e("Firestore Error", "Error while fetching data", e)
                    return@addSnapshotListener
                } else {
                    binding.viewCompetitionRefresh.isRefreshing = false
                }
            }

            binding.resultsRV.layoutManager = LinearLayoutManager(this)
            resultsAdapter = SearchUserAdapter(this, options, this, "Top")
            binding.resultsRV.adapter = resultsAdapter
            resultsAdapter.startListening()
        } else {
            binding.viewCompetitionRefresh.isRefreshing = false
        }
    }

    private fun setupSubmissions(map:HashMap<String, String>) {
        binding.viewCompetitionRefresh.isRefreshing = true
        val fileUrls = map.values.toList()

        if (fileUrls.isNotEmpty()){
            val myQuery: Query = FirebaseUtil().retrieveCommunityFilesCollection(communityId)
                .whereIn("fileUrl", fileUrls)
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)

            // Add a listener to handle success or failure of the query
            myQuery.addSnapshotListener { _, e ->
                if (e != null) {
                    // Handle the error here (e.g., log the error or show a message to the user)
                    Log.e("Firestore Error", "Error while fetching data", e)
                    return@addSnapshotListener
                } else {
                    binding.viewCompetitionRefresh.isRefreshing = false
                }
            }

            //set options for firebase ui
            val options: FirestoreRecyclerOptions<FileModel> =
                FirestoreRecyclerOptions.Builder<FileModel>().setQuery(myQuery, FileModel::class.java).build()

            binding.submissionsRV.layoutManager = LinearLayoutManager(this)
            submissionsAdapter = CompetitionFilesAdapter(this, options, competitionId)
            binding.submissionsRV.adapter = submissionsAdapter
            submissionsAdapter.startListening()
        } else {
            binding.viewCompetitionRefresh.isRefreshing = false
        }
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



    //For Select User Dialog
    private fun openSelectUserDialog() {
        dialogPlusBinding = DialogSelectUserBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(dialogPlusBinding.root))
            .setExpanded(false)
            .setOnDismissListener { onRefresh() }
            .create()

        searchUsers()

        dialogPlusBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchUserQuery = dialogPlusBinding.searchEdtTxt.text.toString()
                searchUsers()
            }
        })

        setupSelectedUsersRV()

        dialogPlusBinding.backBtn.setOnClickListener {
            dialogPlus.dismiss()
        }

        dialogPlusBinding.assignBtn.setOnClickListener {
            FirebaseUtil().retrieveCommunityCompetitionsCollection(communityId)
                .document(competitionId)
                .update(FieldPath.of("results", resultType), selectedUserList)
                .addOnSuccessListener {
                    Toast.makeText(this, "Successfully saved results", Toast.LENGTH_SHORT).show()
                    dialogPlus.dismiss()
                }
        }

        Handler().postDelayed({
            dialogPlus.show()
        }, 500)
    }


    //For Select User Dialog
    private fun searchUsers(){
        if (searchUserQuery.isNotEmpty() && submittedContestants.isNotEmpty()){
            if (searchUserQuery[0] == '@'){
                //search for username
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", submittedContestants)
                    .whereGreaterThanOrEqualTo("username", searchUserQuery.removePrefix("@"))

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUserList)
                dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            } else {
                //search for fullName
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", submittedContestants)
                    .whereGreaterThanOrEqualTo("fullName", searchUserQuery)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
                searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUserList)
                dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        } else if (submittedContestants.isNotEmpty()) {
            //query all users
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", submittedContestants)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(this)
            searchUserAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser", selectedUserList)
            dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
            searchUserAdapter.startListening()
        }
    }

    private fun getSubmittedContestants(){
        for (contestant in competitionModel.contestants){
            //if the user submitted its file
            if (contestant.value.isNotEmpty()){
                //adds to the list of submittedContestants
                submittedContestants.add(contestant.key)
            }
        }
    }


    //For Select User Dialog
    private fun setupSelectedUsersRV(){
        if (selectedUserList.isNotEmpty()){

            dialogPlusBinding.selectedUsersLayout.visibility = View.VISIBLE
            dialogPlusBinding.selectedUsersTV.text = "Selected Users (${selectedUserList.size})"

            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", selectedUserList)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            if (resultType == "Top"){
                //get the total rank
                val limit = resultType.split('/')[1].toInt()
                //limits the selectedUserList
                selectedUserList = ArrayList(selectedUserList.take(limit))
            }

            //set up searched users recycler view
            dialogPlusBinding.selectedUsersRV.layoutManager = LinearLayoutManager(this)
            selectedUsersAdapter = SearchUserAdapter(context = this, options, listener = this, purpose = "SelectUser/${resultType.split('/')[0]}", selectedUserList)
            dialogPlusBinding.selectedUsersRV.adapter = selectedUsersAdapter
            selectedUsersAdapter.startListening()

        } else {
            dialogPlusBinding.selectedUsersTV.text = "Selected Users (0)"
            dialogPlusBinding.selectedUsersLayout.visibility = View.GONE
        }
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

    override fun onItemClick(id: String, isChecked: Boolean) {
        //Interface for select user adapter
        if (isChecked) {
            //add user to selected user list
            selectedUserList.add(id)
            setupSelectedUsersRV()
            searchUsers()
        } else {
            //remove user to selected user list
            selectedUserList.remove(id)
            setupSelectedUsersRV()
            searchUsers()
        }
    }

    //Unnecessary methods (needed for the instruction adapter to work)
    override fun deleteInstruction(key: String) {
    }
    override fun modifiedInstruction(key: String, instructionModel: InstructionModel) {
    }
    override fun openImageLauncher(key: String, instruction: String) {
    }
}