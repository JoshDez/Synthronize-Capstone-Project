package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentExploreBinding
class ExploreFragment(private val mainBinding:ActivityMainBinding) : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var binding:FragmentExploreBinding
    private lateinit var context:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentExploreBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "EXPLORE"
        if (isAdded){
            context = requireContext()
            if (::context.isInitialized){
                bindButtons()
            }
        }

    }

    private fun bindButtons(){
        mainBinding.searchBtn.setOnClickListener {
            val intent = Intent(context, Search::class.java)
            startActivity(intent)
        }
    }
}