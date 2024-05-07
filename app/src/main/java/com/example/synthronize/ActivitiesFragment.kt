package com.example.synthronize

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.synthronize.databinding.FragmentActivitiesBinding
import com.example.synthronize.databinding.FragmentCommunityBinding

class ActivitiesFragment(private val mainBinding: FragmentCommunityBinding, communityId:String) : Fragment() {

    private lateinit var binding: FragmentActivitiesBinding
    private lateinit var context: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //Retrieve Group Model
            context = requireContext()
        }

    }
}