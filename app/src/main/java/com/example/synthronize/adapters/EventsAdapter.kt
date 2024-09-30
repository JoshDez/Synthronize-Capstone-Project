package com.example.synthronize.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.synthronize.R
import com.example.synthronize.databinding.DialogListBinding
import com.example.synthronize.databinding.ItemEventBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Locale

class EventsAdapter(
    private val context: Context,
    options: FirestoreRecyclerOptions<EventModel>,
    private val listener: OnItemClickListener
) : FirestoreRecyclerAdapter<EventModel, EventsAdapter.EventViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEventBinding.inflate(inflater, parent, false)
        return EventViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int, model: EventModel) {
        holder.bind(model)
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding,
        private val context: Context,
        private val inflater: LayoutInflater
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var eventModel: EventModel

        fun bind(model: EventModel) {
            eventModel = model

            val dateAndTime = formatEventDate(eventModel.eventDate)

            // Bind event details to the UI elements
            binding.eventNameTV.text = eventModel.eventName
            binding.eventDescriptionTV.text = eventModel.eventDesc
            binding.eventLocationTV.text = eventModel.eventLocation
            binding.eventDateTV.text = dateAndTime.split(", ")[0]
            binding.eventTimeTV.text = dateAndTime.split(", ")[1]
            binding.participantsCountTV.text = eventModel.eventParticipants.size.toString()

            binding.menu.setOnClickListener {
                DialogUtil().openMenuDialog(context, inflater, "Event", eventModel.eventId, eventModel.eventOwnerId, eventModel.communityId){}

            }
            binding.participantsCountTV.setOnClickListener {
                openParticipantsListDialog(inflater)
            }

            // Load event image if available
            if (eventModel.eventImageName.isNotEmpty()) {
                Glide.with(context)
                    .load(FirebaseUtil().retrieveCommunityContentImageRef(eventModel.eventImageName))
                    .into(binding.eventImageIV)
            } else {
                binding.eventImageIV.visibility = View.GONE
            }

            // Handle participate button logic
            bindParticipateButton()
        }

        private fun bindParticipateButton() {
            if (eventModel.eventDate > Timestamp.now()){
                FirebaseUtil().retrieveCommunityEventsCollection(eventModel.communityId)
                    .document(eventModel.eventId)
                    .get()
                    .addOnSuccessListener {
                        val tempModel = it.toObject(EventModel::class.java)!!
                        if (AppUtil().isIdOnList(tempModel.eventParticipants, FirebaseUtil().currentUserUid())) {
                            binding.participateBtn.text = "Cancel"
                            binding.participateBtn.setOnClickListener {
                                FirebaseUtil().retrieveCommunityEventsCollection(tempModel.communityId)
                                    .document(tempModel.eventId)
                                    .update("eventParticipants", FieldValue.arrayRemove(FirebaseUtil().currentUserUid()))
                                    .addOnSuccessListener {
                                        bindParticipateButton()
                                    }
                            }
                        } else {
                            binding.participateBtn.text = "Participate"
                            binding.participateBtn.setOnClickListener {
                                FirebaseUtil().retrieveCommunityEventsCollection(tempModel.communityId)
                                    .document(tempModel.eventId)
                                    .update("eventParticipants", FieldValue.arrayUnion(FirebaseUtil().currentUserUid()))
                                    .addOnSuccessListener {
                                        bindParticipateButton()
                                    }
                            }
                        }
                    }
            } else {
                binding.participateBtn.visibility = View.GONE
            }

        }


        private fun openParticipantsListDialog(layoutInflater: LayoutInflater){
            val participantsBinding = DialogListBinding.inflate(layoutInflater)
            val friendsDialog = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(participantsBinding.root))
                .create()

            var searchQuery = ""

            if (eventModel.eventParticipants.isNotEmpty())
                setupParticipantsRV(context, searchQuery, participantsBinding.listRV)

            participantsBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    searchQuery = participantsBinding.searchEdtTxt.text.toString()
                    if (eventModel.eventParticipants.isNotEmpty())
                        setupParticipantsRV(context, searchQuery, participantsBinding.listRV)
                }

            })

            participantsBinding.toolbarTitleTV.text = "Participants"

            participantsBinding.backBtn.setOnClickListener {
                friendsDialog.dismiss()
            }

            friendsDialog.show()
        }

        private fun setupParticipantsRV(context: Context, searchQuery:String, participantsRV:RecyclerView) {
            if (searchQuery.isNotEmpty()){
                if (searchQuery[0] == '@'){
                    //search for username
                    val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                        .whereIn("userID", eventModel.eventParticipants)
                        .whereGreaterThanOrEqualTo("username", searchQuery.removePrefix("@"))

                    val options: FirestoreRecyclerOptions<UserModel> =
                        FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                    //set up searched users recycler view
                    participantsRV.layoutManager = LinearLayoutManager(context)
                    val searchUserAdapter = SearchUserAdapter(context = context, options, listener = listener)
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
                    participantsRV.layoutManager = LinearLayoutManager(context)
                    val searchUserAdapter = SearchUserAdapter(context = context, options, listener = listener)
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
                participantsRV.layoutManager = LinearLayoutManager(context)
                val searchUserAdapter = SearchUserAdapter(context = context, options, listener = listener)
                participantsRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        }

        private fun formatEventDate(timestamp: Timestamp): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(timestamp.toDate())
        }


    }
}
