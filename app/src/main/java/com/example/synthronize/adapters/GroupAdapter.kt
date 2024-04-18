package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemGroupBinding
import com.example.synthronize.model.GroupModel

class GroupAdapter(private val context: Context) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var groups: List<GroupModel> = emptyList()

    fun setData(groups: List<GroupModel>) {
        this.groups = groups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGroupBinding.inflate(inflater, parent, false)
        return GroupViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount(): Int {
        return groups.size
    }

    class GroupViewHolder(private val binding:ItemGroupBinding, private val context: Context) : RecyclerView.ViewHolder(binding.root){

        fun bind(group: GroupModel) {
            binding.groupNameTextView.text = group.groupName
            binding.itemGroupLayout.setOnClickListener {
                Toast.makeText(context, "Clicked on group: ${group.groupName}  \n Group Code: ${group.groupCode}", Toast.LENGTH_SHORT).show()

            }
        }
    }

}
