package com.example.synthronize

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.example.synthronize.adapters.CommunityAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogAddCommunityBinding
import com.example.synthronize.databinding.DialogCommunityCodeBinding
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.FragmentCommunitySelectionBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DialogUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FieldValue
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class CommunitySelectionFragment(private val mainBinding: ActivityMainBinding, private val mainActivityListener: OnItemClickListener) : Fragment(), OnItemClickListener,
    OnRefreshListener, OnNetworkRetryListener {
    private lateinit var binding: FragmentCommunitySelectionBinding
    private lateinit var dialogBinding: DialogAddCommunityBinding
    private lateinit var communityAdapter: CommunityAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var context:Context
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCommunitySelectionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "COMMUNITIES"

        //If the fragment is added
        if (isAdded){
            context = requireContext()

            //checks if context is initialized
            if (::context.isInitialized){

                // Initialize RecyclerView and adapter
                recyclerView = binding.groupSelectionRV
                recyclerView.layoutManager = LinearLayoutManager(context)

                //reset main toolbar
                AppUtil().resetMainToolbar(mainBinding)

                //initialize refresh layout
                binding.selectionRefreshLayout.setOnRefreshListener(this)
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)


                //bind search community
                mainBinding.searchBtn.visibility = View.VISIBLE
                mainBinding.searchBtn.setOnClickListener {
                    binding.searchContainerLL.visibility = View.VISIBLE
                    mainBinding.searchBtn.visibility = View.GONE
                }

                //bind cancel button beside the search bar
                binding.cancelBtn.setOnClickListener {
                    mainBinding.searchBtn.visibility = View.VISIBLE
                    binding.searchContainerLL.visibility = View.GONE
                    searchQuery = ""
                    binding.searchEdtTxt.setText("")
                    setupRecyclerView()
                }

                //bind search bar for search groups
                binding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        searchQuery = binding.searchEdtTxt.text.toString()
                        setupRecyclerView()
                    }
                })

                // Set up Add Group FAB
                binding.addGroupFab.setOnClickListener {
                    openAddCommunityDialog()
                }

                // Fetch groups from Firestore
                setupRecyclerView()


            }

        }
    }

    private fun openAddCommunityDialog(){
        dialogBinding = DialogAddCommunityBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogBinding.root))
            .setGravity(Gravity.CENTER)
            .setBackgroundColorResId(R.color.transparent)
            .setCancelable(true)
            .setExpanded(false)
            .create()

        //set dialog on click listeners
        dialogBinding.createNewCommunityBtn.setOnClickListener {
            val intent = Intent(context, CreateCommunity::class.java)
            startActivity(intent)
            dialogPlus.dismiss()
        }

        dialogBinding.joinCommunityViaCodeBtn.setOnClickListener {
            //CREATES NEW DIALOG FOR COMMUNITY CODE
            val codeDialogBinding = DialogCommunityCodeBinding.inflate(layoutInflater)
            val codeDialogPlus = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(codeDialogBinding.root))
                .setGravity(Gravity.CENTER)
                .setBackgroundColorResId(R.color.transparent)
                .setCancelable(true)
                .setExpanded(false)
                .create()

            //confirm button
            codeDialogBinding.confirmBtn.setOnClickListener {
                val code = codeDialogBinding.codeEdtTxt.text.toString()
                if (code.isNotEmpty()){
                    FirebaseUtil().retrieveAllCommunityCollection().whereEqualTo("communityCode", code).get().addOnSuccessListener {documents ->
                        for (document in documents){
                            val communityModel = document.toObject(CommunityModel::class.java)
                            DialogUtil().openCommunityPreviewDialog(context, layoutInflater, communityModel)
                            codeDialogPlus.dismiss()
                        }
                    }
                } else {
                    codeDialogPlus.dismiss()
                }
            }
            dialogPlus.dismiss()
            android.os.Handler().postDelayed({
                codeDialogPlus.show()
            }, 500)
        }

        dialogBinding.searchNewCommunityBtn.setOnClickListener {
            val intent = Intent(context, Search::class.java)
            intent.putExtra("searchInCategory", "communities")
            startActivity(intent)
            dialogPlus.dismiss()
        }

        dialogPlus.show()
    }

    private fun setupRecyclerView() {
        binding.selectionRefreshLayout.isRefreshing = true

        //roles
        val roles = listOf("Admin", "Moderator", "Member")

        var communityQuery = FirebaseUtil().retrieveAllCommunityCollection()
            .whereIn("communityMembers.${FirebaseUtil().currentUserUid()}", roles)

        if (searchQuery.isNotEmpty()){
            //query firestore
            communityQuery = FirebaseUtil().retrieveAllCommunityCollection()
                .whereIn("communityMembers.${FirebaseUtil().currentUserUid()}", roles)
                .whereGreaterThanOrEqualTo("communityName", searchQuery)
        }


        // Add a listener to handle success or failure of the query
        communityQuery.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.selectionRefreshLayout.isRefreshing = false
            }
        }

        //set options for firebase ui
        val options: FirestoreRecyclerOptions<CommunityModel> =
             FirestoreRecyclerOptions.Builder<CommunityModel>().setQuery(communityQuery, CommunityModel::class.java).build()

        communityAdapter = CommunityAdapter(mainBinding, context, options, this)
        recyclerView.adapter = communityAdapter
        communityAdapter.startListening()
    }

    override fun onStart() {
        super.onStart()
        if (::communityAdapter.isInitialized)
            communityAdapter.startListening()
    }
    override fun onResume() {
        super.onResume()
        if (::communityAdapter.isInitialized)
            communityAdapter.notifyDataSetChanged()
    }


    override fun onStop() {
        super.onStop()
        if (::communityAdapter.isInitialized)
            communityAdapter.stopListening()
    }

    override fun onRefresh() {
        binding.selectionRefreshLayout.isRefreshing = true
        Handler().postDelayed({
            setupRecyclerView()
        }, 1000)
    }

    override fun retryNetwork() {
        onRefresh()
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        mainActivityListener.onItemClick(id)
    }


}