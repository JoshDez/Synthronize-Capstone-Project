package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemFeedPageBinding
import com.example.synthronize.utils.ContentUtil

//FOR FEED CONTENT
class ViewPageAdapter(private val context:Context, private val contentList: List<String>):RecyclerView.Adapter<ViewPageAdapter.PageViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPageAdapter.PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFeedPageBinding.inflate(inflater, parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewPageAdapter.PageViewHolder, position: Int) {
        holder.bind(contentList[position])
    }

    override fun getItemCount(): Int {
        return contentList.size
    }


    inner class PageViewHolder(private val feedBinding: ItemFeedPageBinding): RecyclerView.ViewHolder(feedBinding.root){

        fun bind(content:String){
            //gets the content type
            val temp = content.split('-')
            if (temp[1] == "Image"){
                //gets image view and binds it in the linear layout
                feedBinding.contentContainerLayout.addView(ContentUtil().getImageView(context, content))

            } else if (temp[1] == "Video"){
                //TODO to be implemented
            }
        }


    }
}