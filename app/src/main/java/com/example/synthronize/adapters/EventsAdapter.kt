package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.synthronize.R
import com.example.synthronize.databinding.ItemEventBinding
import com.example.synthronize.model.EventModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Locale

class EventsAdapter(
    private val context: Context,
    options: FirestoreRecyclerOptions<EventModel>
) : FirestoreRecyclerAdapter<EventModel, EventsAdapter.EventViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEventBinding.inflate(inflater, parent, false)
        return EventViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int, model: EventModel) {
        holder.bind(model)
    }

    class EventViewHolder(
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

            val inflater = LayoutInflater.from(context)
            binding.menu.setOnClickListener {
                DialogUtil().openMenuDialog(context, inflater, "Events", eventModel.eventId, eventModel.eventOwnerId, eventModel.communityId){}

            }

            // Load event image if available
            if (eventModel.eventImageList.isNotEmpty()) {
                Glide.with(context)
                    .load(eventModel.eventImageList[0])
                    .into(binding.eventImageIV)
            } else {
                binding.eventImageIV.visibility = View.GONE
            }

            // Handle participate button logic
            bindParticipateButton()
        }

        private fun bindParticipateButton() {
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
        }

        private fun formatEventDate(timestamp: Timestamp): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(timestamp.toDate())
        }


    }
}
