package com.example.synthronize

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentNotificationBinding
class NotificationFragment(private val mainBinding: ActivityMainBinding) : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var binding: FragmentNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (isAdded){
            super.onViewCreated(view, savedInstanceState)
            mainBinding.toolbarTitleTV.text = "NOTIFICATIONS"
        }
    }
}