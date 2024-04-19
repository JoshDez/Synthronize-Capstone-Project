package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.CommunityFragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.synthronize.databinding.ItemGroupBinding
import com.example.synthronize.model.CommunityModel

class CommunityAdapter(private val mainBinding: ActivityMainBinding, private val fragmentManager: FragmentManager,
                       private val context: Context, options: FirestoreRecyclerOptions<CommunityModel>):
    FirestoreRecyclerAdapter<CommunityModel, CommunityAdapter.CommunityViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val communityBinding = ItemGroupBinding.inflate(inflater, parent, false)
        return CommunityViewHolder(mainBinding, fragmentManager, communityBinding, context)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        holder.bind(model)
    }

    class CommunityViewHolder(private val mainBinding: ActivityMainBinding, private val fragmentManager: FragmentManager,
                              private val communityBinding: ItemGroupBinding, private val context: Context): RecyclerView.ViewHolder(communityBinding.root){
        fun bind(model: CommunityModel){
            communityBinding.groupNameTextView.text = model.communityName
            //set on click listener
            communityBinding.itemGroupLayout.setOnClickListener {
                //set the header of main activity
                mainBinding.toolbarTitleTV.text = model.communityName
                addCommunityFragmentToMain(model.communityId)

            }
        }
        private fun addCommunityFragmentToMain(communityId:String){

            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(mainBinding.mainFrameLayout.id, CommunityFragment(mainBinding, communityId))
            fragmentTransaction.commit()
        }
        
    }

}