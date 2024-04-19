// GroupFragment.kt

package com.example.synthronize

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.FragmentCommunityBinding

class CommunityFragment(private val mainBinding: ActivityMainBinding, private val communityId:String) : Fragment() {

    private lateinit var binding: FragmentCommunityBinding
    private lateinit var context: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //IF THE FRAGMENT IS ADDED (avoids fragment related crash)
        if (isAdded){
            //retrieve context
            context = requireContext()
            if (::context.isInitialized){

                //Set Feeds fragment as default fragment
                selectNavigation("feeds")
                replaceFragment(FeedsFragment(binding, communityId))

                //bind setOnClickListeners
                bindButtons()
            }
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.communityFrameLayout.id, fragment)
        fragmentTransaction.commit()
    }

    private fun bindButtons(){
        binding.feedsTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(FeedsFragment(binding, communityId))
            selectNavigation("feeds")
        }
        binding.eventsTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(EventsFragment(binding, communityId))
            selectNavigation("events")
        }
        binding.forumsTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(ForumsFragment(binding, communityId))
            selectNavigation("forums")
        }
        binding.marketTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(MarketFragment(binding, communityId))
            selectNavigation("market")
        }
        binding.filesTextView.setOnClickListener {
            //TODO: changes to buttons
            replaceFragment(FilesFragment(binding, communityId))
            selectNavigation("files")
        }
    }
    private fun selectNavigation(fragment:String) {
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.less_saturated_light_purple)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.light_purple)
        binding.feedsTextView.setTextColor(unselectedColor)
        binding.eventsTextView.setTextColor(unselectedColor)
        binding.forumsTextView.setTextColor(unselectedColor)
        binding.marketTextView.setTextColor(unselectedColor)
        binding.filesTextView.setTextColor(unselectedColor)
        when (fragment) {
            "feeds" -> {
                binding.feedsTextView.setTextColor(selectedColor)
            }
            "events" -> {
                binding.eventsTextView.setTextColor(selectedColor)
            }
            "forums" -> {
                binding.forumsTextView.setTextColor(selectedColor)
            }
            "market" -> {
                binding.marketTextView.setTextColor(selectedColor)
            }
            "files" -> {
                binding.filesTextView.setTextColor(selectedColor)

            }
        }
    }
}
