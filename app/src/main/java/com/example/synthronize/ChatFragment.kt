package com.example.synthronize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.example.synthronize.adapters.ChatroomAdapter
import com.example.synthronize.adapters.CommunityChatroomsAdapter
import com.example.synthronize.adapters.FriendsAdapter
import com.example.synthronize.adapters.SearchUserAdapter
import com.example.synthronize.databinding.ActivityMainBinding
import com.example.synthronize.databinding.DialogCommunityPreviewBinding
import com.example.synthronize.databinding.DialogCreateGroupchatBinding
import com.example.synthronize.databinding.DialogMenuBinding
import com.example.synthronize.databinding.FragmentChatBinding
import com.example.synthronize.interfaces.OnItemClickListener
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.example.synthronize.model.ChatroomModel
import com.example.synthronize.model.CommunityModel
import com.example.synthronize.model.UserModel
import com.example.synthronize.utils.AppUtil
import com.example.synthronize.utils.DateAndTimeUtil
import com.example.synthronize.utils.FirebaseUtil
import com.example.synthronize.utils.NetworkUtil
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import android.net.Uri
import android.widget.Toast
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp

class ChatFragment(private val mainBinding: ActivityMainBinding) : Fragment(), OnRefreshListener, OnNetworkRetryListener, OnItemClickListener {
    private lateinit var binding: FragmentChatBinding
    private lateinit var chatroomAdapter: ChatroomAdapter
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var communityChatroomsAdapter: CommunityChatroomsAdapter
    private lateinit var context: Context
    private var currentTab = ""

    //for creating new group chat
    private var searchUserQuery = ""
    private var selectedUserList:ArrayList<String> = arrayListOf()
    private lateinit var selectedUsersAdapter:SearchUserAdapter
    private lateinit var searchUserAdapter:SearchUserAdapter
    private lateinit var dialogPlusBinding: DialogCreateGroupchatBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedGCImage: Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding.toolbarTitleTV.text = "CHATS"

        //checks if fragment is added to the main activity
        if (isAdded){
            context = requireContext()
            if (::context.isInitialized){

                //check for internet
                NetworkUtil(context).checkNetworkAndShowSnackbar(mainBinding.root, this)

                //reset main toolbar
                AppUtil().resetMainToolbar(mainBinding)

                binding.inboxRV.layoutManager = LinearLayoutManager(activity)
                binding.communityChatsRV.layoutManager = LinearLayoutManager(activity)
                binding.friendsListRV.layoutManager = LinearLayoutManager(activity)
                binding.chatRefreshLayout.setOnRefreshListener(this)


                mainBinding.kebabMenuBtn.visibility = View.VISIBLE
                mainBinding.kebabMenuBtn.setOnClickListener {
                    openMenuDialog()
                }

                //set default tab
                navigate("direct_message")

                binding.inboxBtn.setOnClickListener {
                    navigate("direct_message")
                }
                binding.communityChatsBtn.setOnClickListener {
                    navigate("community_chat")
                }
                binding.friendsBtn.setOnClickListener {
                    navigate("friends_list")
                }

                //For Creating Group Chat
                //Launcher for user profile pic and user cover pic
                imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
                    //Image is selected
                    if (result.resultCode == Activity.RESULT_OK){
                        val data = result.data
                        if (data != null && data.data != null){
                            selectedGCImage = data.data!!
                            Glide.with(this)
                                .load(selectedGCImage)
                                .apply(RequestOptions.circleCropTransform())
                                .into(dialogPlusBinding.userProfileCIV)
                        }
                    }
                }

            }
        }
    }
    private fun navigate(tab:String){
        binding.inboxRV.visibility = View.GONE
        binding.communityChatsRV.visibility = View.GONE
        binding.friendsListRV.visibility = View.GONE
        //binding.communityChatsBtn.setTextColor(unselectedColor)
        //binding.communityChatsRV.visibility = View.GONE


        if (tab == "community_chat"){
            binding.communityChatsRV.visibility = View.VISIBLE
            setupChatroomListForCommunity()
            currentTab = "community_chat"
            //binding.communityChatsBtn.setTextColor(selectedColor)
        }else if (tab == "direct_message"){
            binding.inboxRV.visibility = View.VISIBLE
            setupChatroomListForInbox()
            currentTab = "direct_message"
            //binding.inboxBtn.setTextColor(selectedColor)
        }else if (tab == "friends_list"){
            binding.friendsListRV.visibility = View.VISIBLE
            setupFriendsList()
            currentTab = "friends_list"
        }
    }

    //Set Recycle View
    private fun setupChatroomListForInbox(){
        //already indexed in firebase
        binding.chatRefreshLayout.isRefreshing =  true

        val forInbox = listOf("group_chat", "direct_message")

        val query:Query = FirebaseUtil().retrieveAllChatRoomReferences()
            .whereArrayContains("userIdList", FirebaseUtil().currentUserUid())
            .whereIn("chatroomType", forInbox)
            .whereNotEqualTo("lastMessage", "")
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        // Add a listener to handle success or failure of the query
        query.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.chatRefreshLayout.isRefreshing =  false
            }
        }

        val options: FirestoreRecyclerOptions<ChatroomModel> =
            FirestoreRecyclerOptions.Builder<ChatroomModel>().setQuery(query, ChatroomModel::class.java).build()

        chatroomAdapter = ChatroomAdapter(context, options)
        binding.inboxRV.adapter = chatroomAdapter
        chatroomAdapter.startListening()
    }

    private fun setupChatroomListForCommunity(){
        val roles = listOf("Admin", "Member", "Moderator")

        val query:Query = FirebaseUtil().retrieveAllCommunityCollection()
            .whereIn("communityMembers.${FirebaseUtil().currentUserUid()}", roles)


        // Add a listener to handle success or failure of the query
        query.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.chatRefreshLayout.isRefreshing =  false
            }
        }

        val options: FirestoreRecyclerOptions<CommunityModel> =
            FirestoreRecyclerOptions.Builder<CommunityModel>().setQuery(query, CommunityModel::class.java).build()

        communityChatroomsAdapter = CommunityChatroomsAdapter(context, options)
        binding.communityChatsRV.adapter = communityChatroomsAdapter
        communityChatroomsAdapter.startListening()
    }

    private fun setupFriendsList(){
        val query:Query = FirebaseUtil().allUsersCollectionReference()
            .whereArrayContains("friendsList", FirebaseUtil().currentUserUid())

        // Add a listener to handle success or failure of the query
        query.addSnapshotListener { _, e ->
            if (e != null) {
                // Handle the error here (e.g., log the error or show a message to the user)
                Log.e("Firestore Error", "Error while fetching data", e)
                return@addSnapshotListener
            } else {
                binding.chatRefreshLayout.isRefreshing =  false
            }
        }

        val options: FirestoreRecyclerOptions<UserModel> =
            FirestoreRecyclerOptions.Builder<UserModel>().setQuery(query, UserModel::class.java).build()

        friendsAdapter = FriendsAdapter(context, options)
        binding.friendsListRV.adapter = friendsAdapter
        friendsAdapter.startListening()
    }


    //MENU
    private fun openMenuDialog() {
        val menuBinding = DialogMenuBinding.inflate(layoutInflater)
        val menuDialog = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(menuBinding.root))
            .setMargin(400, 0, 0, 0)
            .setGravity(Gravity.TOP)
            .setCancelable(true)
            .create()

        //Option 1
        menuBinding.option1.visibility = View.VISIBLE
        menuBinding.optiontitle1.text = "Settings"
        menuBinding.optionIcon1.setImageResource(R.drawable.gear_icon)
        menuBinding.optiontitle1.setOnClickListener {
            val intent = Intent(context, AppSettings::class.java)
            startActivity(intent)
        }



        //Option 2
        menuBinding.option2.visibility = View.VISIBLE
        menuBinding.optiontitle2.text = "Create group chat"
        menuBinding.optionIcon2.setImageResource(R.drawable.baseline_edit_24)
        menuBinding.optiontitle2.setOnClickListener {
            menuDialog.dismiss()
            openCreateGCDialog()
        }

        //Option 3
        menuBinding.option3.visibility = View.VISIBLE
        menuBinding.optiontitle3.text = "Search Users"
        menuBinding.optionIcon3.setImageResource(R.drawable.baseline_logout_24)
        menuBinding.optiontitle3.setOnClickListener {
            menuDialog.dismiss()
            val intent = Intent(context, Search::class.java)
            intent.putExtra("searchInCategory", "users")
            startActivity(intent)
        }
        menuDialog.show()
    }





    //FOR CREATING GROUP CHAT
    private fun openCreateGCDialog() {
        dialogPlusBinding = DialogCreateGroupchatBinding.inflate(layoutInflater)
        val dialogPlus = DialogPlus.newDialog(context)
            .setContentHolder(ViewHolder(dialogPlusBinding.root))
            .setExpanded(false)
            .create()

        searchUsers()

        dialogPlusBinding.searchEdtTxt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchUserQuery = dialogPlusBinding.searchEdtTxt.text.toString()
                searchUsers()
            }
        })

        setupSelectedUsersRV()

        dialogPlusBinding.userProfileCIV.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(512)
                .maxResultSize(512, 512)
                .createIntent {
                    imagePickerLauncher.launch(it)
                }
        }

        dialogPlusBinding.saveBtn.setOnClickListener {
            saveGroupChat(dialogPlus)
        }

        dialogPlusBinding.backBtn.setOnClickListener {
            dialogPlus.dismiss()
        }

        Handler().postDelayed({
            dialogPlus.show()
        }, 500)
    }

    private fun saveGroupChat(dialogPlus: DialogPlus) {
        var chatroomModel = ChatroomModel()

        val groupChatName = dialogPlusBinding.groupChatNameEdtTxt.text.toString()
        if (groupChatName.isEmpty() || groupChatName.length < 3){
            Toast.makeText(context, "Group chat name should at least have 3 or more characters", Toast.LENGTH_SHORT).show()
        } else if (selectedUserList.size < 2) {
            Toast.makeText(context, "Members should be more than 1", Toast.LENGTH_SHORT).show()
        } else {

            FirebaseUtil().retrieveAllChatRoomReferences().add(chatroomModel).addOnSuccessListener {
                var imageUrl = ""

                if (selectedGCImage != null){
                    imageUrl = "${it.id}-${Timestamp.now()}"
                    FirebaseUtil().retrieveGroupChatProfileRef(imageUrl).putFile(selectedGCImage)
                }

                selectedUserList.add(FirebaseUtil().currentUserUid())

                chatroomModel = ChatroomModel(
                    chatroomId = it.id,
                    chatroomType = "group_chat",
                    userIdList = selectedUserList,
                    lastMsgTimestamp = Timestamp.now(),
                    lastMessage = "Created a group",
                    lastMessageUserId = FirebaseUtil().currentUserUid(),
                    chatroomName = groupChatName,
                    chatroomProfileUrl = imageUrl
                )

                FirebaseUtil().retrieveAllChatRoomReferences().document(it.id).set(chatroomModel).addOnSuccessListener {
                    Toast.makeText(context, "Group chat is successfully created", Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({
                        dialogPlus.dismiss()
                        val intent = Intent(context, Chatroom::class.java)
                        intent.putExtra("chatroomId", chatroomModel.chatroomId)
                        intent.putExtra("chatroomType", chatroomModel.chatroomType)
                        startActivity(intent)
                    }, 2000)
                }.addOnFailureListener {
                    Toast.makeText(context, "An error has occurred, please try again", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun setupSelectedUsersRV(){
        if (selectedUserList.isNotEmpty()){

            dialogPlusBinding.selectedUsersLayout.visibility = View.VISIBLE
            dialogPlusBinding.selectedUsersTV.text = "Selected Users (${selectedUserList.size})"

            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                .whereIn("userID", selectedUserList)

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            dialogPlusBinding.selectedUsersRV.layoutManager = LinearLayoutManager(context)
            selectedUsersAdapter = SearchUserAdapter(context = context, options, listener = this, purpose = "SelectUser", selectedUserList)
            dialogPlusBinding.selectedUsersRV.adapter = selectedUsersAdapter
            selectedUsersAdapter.startListening()

        } else {
            dialogPlusBinding.selectedUsersTV.text = "Selected Users (0)"
            dialogPlusBinding.selectedUsersLayout.visibility = View.GONE
        }
    }


    private fun searchUsers(){
        if (searchUserQuery.isNotEmpty()){
            if (searchUserQuery[0] == '@'){
                //search for username
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("username", searchUserQuery.removePrefix("@"))

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(context)
                searchUserAdapter = SearchUserAdapter(context = context, options, listener = this, purpose = "SelectUser", selectedUserList)
                dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()

            } else {
                //search for fullName
                val myQuery: Query = FirebaseUtil().allUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("fullName", searchUserQuery)

                val options: FirestoreRecyclerOptions<UserModel> =
                    FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

                //set up searched users recycler view
                dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(context)
                searchUserAdapter = SearchUserAdapter(context = context, options, listener = this, purpose = "SelectUser", selectedUserList)
                dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
                searchUserAdapter.startListening()
            }
        } else {
            //query all users
            val myQuery: Query = FirebaseUtil().allUsersCollectionReference()

            val options: FirestoreRecyclerOptions<UserModel> =
                FirestoreRecyclerOptions.Builder<UserModel>().setQuery(myQuery, UserModel::class.java).build()

            //set up searched users recycler view
            dialogPlusBinding.searchedUsersRV.layoutManager = LinearLayoutManager(context)
            searchUserAdapter = SearchUserAdapter(context = context, options, listener = this, purpose = "SelectUser", selectedUserList)
            dialogPlusBinding.searchedUsersRV.adapter = searchUserAdapter
            searchUserAdapter.startListening()
        }
    }


    override fun onStart() {
        super.onStart()
        if (::chatroomAdapter.isInitialized){
            chatroomAdapter.startListening()
        }
        if (::friendsAdapter.isInitialized){
            friendsAdapter.startListening()
        }
        if (::communityChatroomsAdapter.isInitialized){
            communityChatroomsAdapter.startListening()
        }

    }

    override fun onResume() {
        super.onResume()
        if (::chatroomAdapter.isInitialized){
            chatroomAdapter.notifyDataSetChanged()
        }
        if (::friendsAdapter.isInitialized){
            friendsAdapter.notifyDataSetChanged()
        }
        if (::communityChatroomsAdapter.isInitialized){
            communityChatroomsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::chatroomAdapter.isInitialized){
            chatroomAdapter.stopListening()
        }
        if (::friendsAdapter.isInitialized){
            friendsAdapter.stopListening()
        }
        if (::communityChatroomsAdapter.isInitialized){
            communityChatroomsAdapter.stopListening()
        }
    }

    override fun onRefresh() {
        binding.chatRefreshLayout.isRefreshing =  true
        Handler().postDelayed({
            navigate(currentTab)
        }, 1000)

    }

    override fun retryNetwork() {
        binding.chatRefreshLayout.isRefreshing =  true
        Handler().postDelayed({
            navigate(currentTab)
        }, 1000)
    }

    override fun onItemClick(id: String, isChecked: Boolean) {
        //Interface for select user adapter
        if (isChecked) {
            //add user to selected user list
            selectedUserList.add(id)
            setupSelectedUsersRV()
            searchUsers()
        } else {
            //remove user to selected user list
            selectedUserList.remove(id)
            setupSelectedUsersRV()
            searchUsers()
        }
    }
}