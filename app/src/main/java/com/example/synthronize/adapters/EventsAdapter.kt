package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemEventBinding
import com.example.synthronize.model.EventModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.concurrent.TimeUnit

class EventsAdapter(private val context: Context, options: FirestoreRecyclerOptions<EventModel>):
    FirestoreRecyclerAdapter<EventModel, EventsAdapter.EventViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEventBinding.inflate(inflater, parent, false)
        return EventViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int, model: EventModel) {
        holder.bind(model)
    }

    class EventViewHolder(private val binding: ItemEventBinding, private val context: Context,
                            private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var eventModel: EventModel

        fun bind(model: EventModel){
            eventModel = model

            bindParticipateBtn()

            binding.eventNameTV.text = eventModel.eventName

            binding.dateTV.text = getTimeBefore(eventModel.eventDate)

        }

        private fun bindParticipateBtn(){
            FirebaseUtil().retrieveCommunityEventsCollection(eventModel.communityId).document(eventModel.eventId).get().addOnSuccessListener {
                val tempModel = it.toObject(EventModel::class.java)!!
                if (AppUtil().isIdOnList(tempModel.eventParticipants, FirebaseUtil().currentUserUid())){
                    //user is already one of the participants
                    binding.participateBtn.text = "Cancel"
                    binding.participateBtn.setOnClickListener {
                        FirebaseUtil().retrieveCommunityEventsCollection(tempModel.communityId).document(tempModel.eventId)
                            .update("eventParticipants", FieldValue.arrayRemove(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                                bindParticipateBtn()
                            }
                    }
                } else {
                    //user is not yet participated
                    binding.participateBtn.text = "Participate"
                    binding.participateBtn.setOnClickListener {
                        FirebaseUtil().retrieveCommunityEventsCollection(tempModel.communityId).document(tempModel.eventId)
                            .update("eventParticipants", FieldValue.arrayUnion(FirebaseUtil().currentUserUid())).addOnSuccessListener {
                                bindParticipateBtn()
                            }
                    }
                }
            }
        }

        // Time before the given timestamp in days
        fun getTimeBefore(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val past = timestamp.toDate().time

            val diff = past - now // Calculate the difference

            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return "$days days before"
        }

    }
}