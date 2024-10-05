package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityViewEventBinding
import com.example.synthronize.databinding.DialogListBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.example.synthronize.utils.NotificationUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.text.SimpleDateFormat
import java.util.Locale

class ViewEvent : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, OnItemClickListener,
    OnNetworkRetryListener {
    private lateinit var binding:ActivityViewEventBinding
    private lateinit var eventModel: EventModel

    private var communityId = ""
    private var eventId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId").toString()
        eventId = intent.getStringExtra("eventId").toString()

        binding.eventRefreshLayout.setOnRefreshListener(this)

        //check for internet
        NetworkUtil(this).checkNetworkAndShowSnackbar(binding.root, this)

        bindEvent()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun bindEvent(){
        binding.eventRefreshLayout.isRefreshing = true
        FirebaseUtil().retrieveCommunityDocument(communityId).get().addOnCompleteListener { community->
            if (community.result.exists()){
                FirebaseUtil().retrieveCommunityEventsCollection(communityId).document(eventId).get().addOnCompleteListener { event ->
                    if (event.result.exists()){

                        eventModel = event.result.toObject(EventModel::class.java)!!

                        val dateAndTime = formatEventDate(eventModel.eventDate)

                        // Bind event details to the UI elements
                        binding.eventNameTV.text = eventModel.eventName
                        binding.descTV.text = eventModel.eventDesc
                        binding.eventLocationTV.text = eventModel.eventLocation
                        binding.eventDateTV.text = dateAndTime.split(", ")[0]
                        binding.eventTimeTV.text = dateAndTime.split(", ")[1]
                        binding.participantsCountTV.text = eventModel.eventParticipants.size.toString()

                        Glide.with(this)
                            .load(FirebaseUtil().retrieveCommunityContentImageRef(eventModel.eventImageName))
                            .into(binding.eventsCoverIV)

                        FirebaseUtil().targetUserDetails(eventModel.eventOwnerId).get().addOnCompleteListener {user ->
                            if (user.result.exists()){
                                binding.hostNameTV.text = "created by ${user.result.getString("fullName")}"
                            }
                        }

                        binding.kebabMenuBtn.setOnClickListener {
                            DialogUtil().openMenuDialog(this, layoutInflater, "Event", eventModel.eventId, eventModel.eventOwnerId, eventModel.communityId){closeCurrentActivity ->
                                if (closeCurrentActivity){
                                    Handler().postDelayed({
                                        onBackPressed()
                                    }, 2000)
                                }
                            }
                        }

                        binding.participantsCountTV.setOnClickListener {
                            openParticipantsListDialog()
                        }

                        bindParticipateButton()
                        binding.eventRefreshLayout.isRefreshing = false
                    }
                }
            } else {
                hideContent()
            }

        }
    }
    private fun bindParticipateButton() {
        if (eventModel.eventDate > Timestamp.now()){
            FirebaseUtil().retrieveCommunityEventsCollection(eventModel.communityId)
                .document(eventModel.eventId)
                .get()
                .addOnSuccessListener {
                    val tempModel = it.toObject(EventModel::class.java)!!
                    if (AppUtil().isIdOnList(tempModel.eventParticipants, FirebaseUtil().currentUserUid())) {
                        binding.actionBtn.text = "Cancel"
                        binding.actionBtn.setOnClickListener {
                            FirebaseUtil().retrieveCommunityEventsCollection(tempModel.communityId)
                                .document(tempModel.eventId)
                                .update("eventParticipants", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                                .addOnSuccessListener {
                                    bindParticipateButton()
                                }
                        }
                    } else {
                        binding.actionBtn.text = "Participate"
                        binding.actionBtn.setOnClickListener {
                            FirebaseUtil().retrieveCommunityEventsCollection(tempModel.communityId)
                                .document(tempModel.eventId)
                                .update("eventParticipants", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                                .addOnSuccessListener {
                                    bindParticipateButton()
                                    NotificationUtil().sendNotificationToUser(this, tempModel.eventId, tempModel.eventOwnerId, "Participant",
                                        "${tempModel.eventParticipants.size + 1}","Event", tempModel.communityId, DateAndTimeUtil().timestampToString(
                                            Timestamp.now()))
                                }
                        }
                    }
                }
        } else {
            binding.actionBtn.visibility = View.GONE
        }

    }

    private fun openParticipantsListDialog(){
        val participantsBinding = DialogListBinding.inflate(layoutInflater)
        val friendsDialog = DialogPlus.newDialog(this)
            .setContentHolder(ViewHolder(participantsBinding.root))
            .create()

        var searchQuery = ""

        if (eventModel.eventParticipants.isNotEmpty())
            setupParticipantsRV(searchQuery, participantsBinding.listRV)

        participantsBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = participantsBinding.searchEdtTxt.text.toString()
                if (eventModel.eventParticipants.isNotEmpty())
                    setupParticipantsRV(searchQuery, participantsBinding.listRV)
            }

        })

        participantsBinding.toolbarTitleTV.text = "Participants"

        participantsBinding.backBtn.setOnClickListener {
            friendsDialog.dismiss()
        }

        friendsDialog.show()
    }

    private fun setupParticipantsRV(searchQuery:String, participantsRV: RecyclerView) {
        if (searchQuery.isNotEmpty()){
            if (searchQuery[0] == '@'){
                //search for username
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", eventModel.eventParticipants)
                    .whereGreaterThanOrEqualTo("username", searchQuery.removePrefix("@"))

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                participantsRV.layoutManager = LinearLayoutManager(this)
                val searchUserAdapter = SearchUserAdapter(context = this, options, listener = this)
                participantsRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            } else {
                //search for fullName
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereIn("userID", eventModel.eventParticipants)
                    .whereGreaterThanOrEqualTo("fullName", searchQuery)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                participantsRV.layoutManager = LinearLayoutManager(this)
                val searchUserAdapter = SearchUserAdapter(context = this, options, listener = this)
                participantsRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        } else {
            //query all users
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", eventModel.eventParticipants)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            participantsRV.layoutManager = LinearLayoutManager(this)
            val searchUserAdapter = SearchUserAdapter(context = this, options, listener = this)
            participantsRV.adapter = searchUserAdapter
            searchUserAdapter.startListening()
        }
    }
    private fun hideContent(){
        binding.scrollViewLayout.visibility = View.GONE
        binding.bottomToolbar.visibility = View.INVISIBLE
        binding.divider2.visibility = View.INVISIBLE
        binding.contentNotAvailableLayout.visibility = View.VISIBLE
    }

    private fun formatEventDate(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    override fun onRefresh() {
        Handler().postDelayed({
            bindEvent()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        val intent = Intent(this, OtherUserProfile::class.java)
        intent.putExtra("userID", id)
        startActivity(intent)
    }
}