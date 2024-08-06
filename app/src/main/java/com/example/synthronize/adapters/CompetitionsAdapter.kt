package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemCompetitionBinding
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CompetitionModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

class CompetitionsAdapter(private val context: Context, options: FirestoreRecyclerOptions<CompetitionModel>):
    FirestoreRecyclerAdapter<CompetitionModel, CompetitionsAdapter.CompetitionViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompetitionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCompetitionBinding.inflate(inflater, parent, false)
        return CompetitionViewHolder(binding, context, inflater)
    }

    override fun onBindViewHolder(holder: CompetitionViewHolder, position: Int, model: CompetitionModel) {
        holder.bind(model)
    }

    class CompetitionViewHolder(private val binding: ItemCompetitionBinding, private val context: Context,
                                      private val inflater: LayoutInflater
    ): RecyclerView.ViewHolder(binding.root){

        private lateinit var competitionModel: CompetitionModel

        fun bind(model: CompetitionModel){
            competitionModel = model
            binding.competitionNameTV.text = competitionModel.competitionName
            binding.descriptionTV.text = competitionModel.instruction
            binding.statusTV.text = DateAndTimeUtil().formatTimestampToDate(competitionModel.deadline)
            binding.timestampTV.text = DateAndTimeUtil().getTimeAgo(competitionModel.createdTimestamp)
            binding.rewardsTV.text = competitionModel.rewards

        }
    }
}