package com.example.synthronize.adapters

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.synthronize.R
import com.example.synthronize.ViewEvent
import com.example.synthronize.databinding.DialogListBinding
import com.example.synthronize.databinding.ItemEventBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.model.EventModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NotificationUtil
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
        holder.checkIfAvailable(model)
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding,
        private val context: Context,
        private val inflater: LayoutInflater
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var eventModel: EventModel

        fun checkIfAvailable(model: EventModel){
            if (model.eventId.isNotEmpty())
                bind(model)
            else
                binding.eventDescriptionTV.text = "Content Not Available"
        }

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
                DialogUtil().openInteractionUsersList(context, inflater, eventModel.eventParticipants)
            }
            binding.mainLayout.setOnClickListener {
                val intent = Intent(context, ViewEvent::class.java)
                intent.putExtra("communityId", eventModel.communityId)
                intent.putExtra("eventId", eventModel.eventId)
                context.startActivity(intent)
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
                                        NotificationUtil().sendNotificationToUser(context, tempModel.eventId, tempModel.eventOwnerId, "Participant",
                                            "${tempModel.eventParticipants.size + 1}","Event", tempModel.communityId, DateAndTimeUtil().timestampToString(
                                                Timestamp.now()))
                                    }
                            }
                        }
                    }
            } else {
                binding.participateBtn.visibility = View.GONE
            }
        }
        private fun formatEventDate(timestamp: Timestamp): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(timestamp.toDate())
        }


    }
}
