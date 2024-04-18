package com.example.synthronize

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentExploreBinding
import com.example.synthronize.databinding.FragmentGroupBinding
import com.example.synthronize.databinding.FragmentGroupSelectionBinding
class GroupSelectionFragment(private val mainBinding: ActivityMainBinding) : Fragment() {
    private lateinit var binding: FragmentGroupSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGroupSelectionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "GROUPS"
    }
}