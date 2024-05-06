package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.adapters.ForumsAdapter
import com.example.synthronize.databinding.FragmentCommunityBinding
import com.example.synthronize.databinding.FragmentForumsBinding
import com.example.synthronize.model.ForumsModel
import com.example.synthronize.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

class ForumsFragment(private val mainBinding: FragmentCommunityBinding, private val communityId:String) : Fragment() {

    private lateinit var binding:FragmentForumsBinding
    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var forumsAdapter: ForumsAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
            if (context != null){
                bindButtons()
                setRecyclerView()
            }
        }
    }

    private fun setRecyclerView() {
        val myQuery:Query = FirebaseUtil().retrieveCommunityForumsCollection(communityId)
            .orderBy("createdTimestamp", Query.Direction.DESCENDING)

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<ForumsModel> =
            FirestoreRecyclerOptions.Builder<ForumsModel>().setQuery(myQuery, ForumsModel::class.java).build()

        recyclerView = binding.threadsRV
        recyclerView.layoutManager = LinearLayoutManager(context)
        forumsAdapter = ForumsAdapter(mainBinding, context, options)
        recyclerView.adapter = forumsAdapter
        forumsAdapter.startListening()
    }

    private fun bindButtons(){
        binding.addThreadFab.setOnClickListener{
            val intent = Intent(context, CreateThread::class.java)
            intent.putExtra("communityId", communityId)
            context.startActivity(intent)
        }
    }
    override fun onStart() {
        super.onStart()
        if (::forumsAdapter.isInitialized){
            forumsAdapter.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::forumsAdapter.isInitialized){
            forumsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::forumsAdapter.isInitialized){
            forumsAdapter.stopListening()
        }
    }
}
