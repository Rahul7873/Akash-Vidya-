package com.akashascent.akashvidya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var itemAdapter: ItemAdapter
    private val itemList = mutableListOf<ItemModel>()
    
    private lateinit var learningAdapter: ItemAdapter
    private lateinit var purchasedAdapter: PurchasedAdapter
    private val purchasedList = mutableListOf<ItemModel>()

    private lateinit var downloadAdapter: DownloadAdapter
    private val downloadList = mutableListOf<VideoModel>()
    private lateinit var downloadManager: DownloadManager
    
    // UI Sections
    private lateinit var homeSection: NestedScrollView
    private lateinit var learningSection: NestedScrollView
    private lateinit var downloadsSection: NestedScrollView
    private lateinit var accountSection: NestedScrollView

    private var doubleBackToExitPressedOnce = false
    private var deviceIdListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        requestNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        downloadManager = DownloadManager(this)

        setupSingleDeviceLoginListener()

        homeSection = findViewById(R.id.home_section)
        learningSection = findViewById(R.id.learning_section)
        downloadsSection = findViewById(R.id.downloads_section)
        accountSection = findViewById(R.id.account_section)

        val userNameTv = findViewById<TextView>(R.id.user_name)
        val accFullNameTv = findViewById<TextView>(R.id.acc_full_name)
        val accPhoneTv = findViewById<TextView>(R.id.acc_phone)
        
        // Setup Downloads RecyclerView
        val rvDownloads = findViewById<RecyclerView>(R.id.rv_downloads)
        rvDownloads.layoutManager = LinearLayoutManager(this)
        downloadAdapter = DownloadAdapter(downloadList) { video ->
            video.videoUrl?.let { url ->
                downloadManager.removeDownload(url)
                loadDownloads()
            }
        }
        rvDownloads.adapter = downloadAdapter

        // Setup Home RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rv_items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemAdapter = ItemAdapter(itemList)
        recyclerView.adapter = itemAdapter
        
        // Setup My Learning RecyclerView
        val rvLearning = findViewById<RecyclerView>(R.id.rv_learning_items)
        rvLearning.layoutManager = LinearLayoutManager(this)
        learningAdapter = ItemAdapter(purchasedList)
        rvLearning.adapter = learningAdapter
        
        // Setup Account My Purchases RecyclerView
        val rvPurchases = findViewById<RecyclerView>(R.id.rv_purchases)
        rvPurchases.layoutManager = LinearLayoutManager(this)
        purchasedAdapter = PurchasedAdapter(purchasedList)
        rvPurchases.adapter = purchasedAdapter

        // Check cache first
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val cachedName = sharedPreferences.getString("cachedFullName", null)
        val cachedPhone = sharedPreferences.getString("cachedPhone", null)
        val cachedGoal = sharedPreferences.getString("userGoal", null)

        if (cachedName != null) {
            userNameTv.text = cachedName
            accFullNameTv.text = cachedName
        }
        if (cachedPhone != null) {
            accPhoneTv.text = cachedPhone
        }
        if (cachedGoal != null) {
            findViewById<TextView>(R.id.tv_current_goal).text = "Goal: $cachedGoal"
        }
        
        fetchUserData(userNameTv, accFullNameTv, accPhoneTv)

        setupFooterNavigation()
        fetchFirebaseItems()
        fetchPurchasedPlaylists()
        loadDownloads()
        
        findViewById<View>(R.id.btn_help_support).setOnClickListener {
            Toast.makeText(this, "Support Contact: support@akashascent.com", Toast.LENGTH_LONG).show()
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            performLogout()
        }

        findViewById<View>(R.id.btn_change_goal).setOnClickListener {
            val intent = Intent(this, PreferenceActivity::class.java)
            startActivity(intent)
        }

        // Double press back to exit
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (homeSection.visibility != View.VISIBLE) {
                    // If not on Home, go back to Home first
                    findViewById<LinearLayout>(R.id.nav_home).performClick()
                    return
                }

                // If already on Home, perform double press to exit
                if (doubleBackToExitPressedOnce) {
                    finishAffinity() // Exit the app completely
                    return
                }

                doubleBackToExitPressedOnce = true
                Toast.makeText(this@MainActivity, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadDownloads()
    }

    private fun setupSingleDeviceLoginListener() {
        val userId = auth.currentUser?.uid ?: return
        val currentDeviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)

        deviceIdListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnap = snapshot.children.first()
                    val savedDeviceId = userSnap.child("deviceId").getValue(String::class.java)
                    if (savedDeviceId != null && savedDeviceId != currentDeviceId) {
                        Toast.makeText(this@MainActivity, "Logged out: Logged in on another device", Toast.LENGTH_LONG).show()
                        performLogout()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addValueEventListener(deviceIdListener!!)
    }

    private fun performLogout() {
        deviceIdListener?.let { 
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.getReference("users").orderByChild("uid").equalTo(userId)
                    .removeEventListener(it)
            }
        }
        auth.signOut()
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("isLoggedIn", false)
            .putBoolean("profileCreated", false)
            .putBoolean("goalSet", false)
            .putBoolean("classSet", false)
            .remove("cachedFullName")
            .remove("cachedPhone")
            .remove("userGoal")
            .remove("userClass")
            .apply()

        val intent = Intent(this, Splash::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceIdListener?.let { 
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.getReference("users").orderByChild("uid").equalTo(userId)
                    .removeEventListener(it)
            }
        }
    }

    private fun setupFooterNavigation() {
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navLearning = findViewById<LinearLayout>(R.id.nav_learning)
        val navDownloads = findViewById<LinearLayout>(R.id.nav_downloads)
        val navAccount = findViewById<LinearLayout>(R.id.nav_account)

        val ivHome = findViewById<ImageView>(R.id.iv_home)
        val tvHome = findViewById<TextView>(R.id.tv_home)
        val ivLearning = findViewById<ImageView>(R.id.iv_learning)
        val tvLearning = findViewById<TextView>(R.id.tv_learning)
        val ivDownloads = findViewById<ImageView>(R.id.iv_downloads)
        val tvDownloads = findViewById<TextView>(R.id.tv_downloads)
        val ivAccount = findViewById<ImageView>(R.id.iv_account)
        val tvAccount = findViewById<TextView>(R.id.tv_account)

        val activeColor = ContextCompat.getColor(this, R.color.primary_blue)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray_text)

        navHome.setOnClickListener {
            showSection(1)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivDownloads, tvDownloads, ivAccount, tvAccount, activeColor, inactiveColor, 1)
        }

        navLearning.setOnClickListener {
            showSection(2)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivDownloads, tvDownloads, ivAccount, tvAccount, activeColor, inactiveColor, 2)
        }

        navDownloads.setOnClickListener {
            showSection(3)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivDownloads, tvDownloads, ivAccount, tvAccount, activeColor, inactiveColor, 3)
        }

        navAccount.setOnClickListener {
            showSection(4)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivDownloads, tvDownloads, ivAccount, tvAccount, activeColor, inactiveColor, 4)
        }
    }
    
    private fun showSection(selection: Int) {
        homeSection.visibility = if (selection == 1) View.VISIBLE else View.GONE
        learningSection.visibility = if (selection == 2) View.VISIBLE else View.GONE
        downloadsSection.visibility = if (selection == 3) View.VISIBLE else View.GONE
        accountSection.visibility = if (selection == 4) View.VISIBLE else View.GONE
    }

    private fun updateNavUI(
        ivH: ImageView, tvH: TextView,
        ivL: ImageView, tvL: TextView,
        ivD: ImageView, tvD: TextView,
        ivA: ImageView, tvA: TextView,
        active: Int, inactive: Int, selection: Int
    ) {
        ivH.setColorFilter(inactive); tvH.setTextColor(inactive)
        ivL.setColorFilter(inactive); tvL.setTextColor(inactive)
        ivD.setColorFilter(inactive); tvD.setTextColor(inactive)
        ivA.setColorFilter(inactive); tvA.setTextColor(inactive)

        when (selection) {
            1 -> { ivH.setColorFilter(active); tvH.setTextColor(active) }
            2 -> { ivL.setColorFilter(active); tvL.setTextColor(active) }
            3 -> { ivD.setColorFilter(active); tvD.setTextColor(active) }
            4 -> { ivA.setColorFilter(active); tvA.setTextColor(active) }
        }
    }

    private fun fetchUserData(nameHome: TextView, nameAcc: TextView, phoneAcc: TextView) {
        val userId = auth.currentUser?.uid ?: return
        val tvCurrentGoal = findViewById<TextView>(R.id.tv_current_goal)

        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    if (snapshot.exists()) {
                        for (userSnap in snapshot.children) {
                            val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
                            val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
                            val phone = userSnap.child("phoneNumber").getValue(String::class.java) ?: "N/A"
                            val goalName = userSnap.child("goalName").getValue(String::class.java) ?: "Not Set"
                            val className = userSnap.child("selectedClassName").getValue(String::class.java)
                            val classId = userSnap.child("selectedClassId").getValue(String::class.java)
                            
                            val fullName = "$firstName $lastName".trim()
                            val displayName = if (fullName.isNotEmpty()) fullName else "User"
                            
                            nameHome.text = displayName
                            nameAcc.text = displayName
                            phoneAcc.text = phone
                            tvCurrentGoal.text = "Goal: $goalName"
                            
                            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            sharedPreferences.edit()
                                .putString("cachedFullName", displayName)
                                .putString("cachedPhone", phone)
                                .putString("userGoal", goalName)
                                .putString("userClass", className)
                                .putString("userClassId", classId)
                                .apply()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchFirebaseItems() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userClass = sharedPreferences.getString("userClass", null)
        val userClassId = sharedPreferences.getString("userClassId", null)

        database.getReference("playlists").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                itemList.clear()
                for (itemSnap in snapshot.children) {
                    val item = itemSnap.getValue(ItemModel::class.java)
                    if (item != null) {
                        // Filter by user's class (check both ID and Name for reliability)
                        val matchesClass = userClass == null || 
                                           item.`class` == userClass || 
                                           item.`class` == userClassId ||
                                           item.`class`?.contains(userClass) == true
                        
                        if (matchesClass) {
                            val playlistId = item.playlistId ?: itemSnap.key
                            itemList.add(item.copy(playlistId = playlistId))
                        }
                    }
                }
                itemAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPurchasedPlaylists() {
        val userId = auth.currentUser?.uid ?: return
        
        // Listen for changes in the user's purchased_playlists node
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    if (userSnapshot.exists()) {
                        val userSnap = userSnapshot.children.first()
                        val purchaseMap = mutableMapOf<String, DataSnapshot>()
                        val pSnap = userSnap.child("purchased_playlists")
                        for (idSnap in pSnap.children) {
                            idSnap.key?.let { purchaseMap[it] = idSnap }
                        }
                        
                        if (purchaseMap.isNotEmpty()) {
                            loadPurchasedDetails(purchaseMap)
                        } else {
                            purchasedList.clear()
                            learningAdapter.notifyDataSetChanged()
                            purchasedAdapter.notifyDataSetChanged()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadPurchasedDetails(purchaseMap: Map<String, DataSnapshot>) {
        database.getReference("playlists").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                purchasedList.clear()
                val learningList = mutableListOf<ItemModel>()
                
                for (itemSnap in snapshot.children) {
                    val item = itemSnap.getValue(ItemModel::class.java)
                    val pId = item?.playlistId ?: itemSnap.key
                    if (item != null && purchaseMap.containsKey(pId)) {
                        val purchaseSnap = purchaseMap[pId]
                        val expiryStr = purchaseSnap?.child("expiryDate")?.getValue(String::class.java)
                        
                        var isExpired = false
                        if (expiryStr != null) {
                            try {
                                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                val expiryDate = sdf.parse(expiryStr)
                                if (expiryDate != null && expiryDate.before(Date())) {
                                    isExpired = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        val updatedItem = item.copy(
                            playlistId = pId,
                            paymentId = purchaseSnap?.child("paymentId")?.getValue(String::class.java),
                            purchaseDate = purchaseSnap?.child("purchaseDate")?.getValue(String::class.java),
                            expiryDate = expiryStr,
                            isExpired = isExpired
                        )
                        purchasedList.add(updatedItem)
                        if (!isExpired) {
                            learningList.add(updatedItem)
                        }
                    }
                }
                
                // Update Learning section (only non-expired)
                learningAdapter = ItemAdapter(learningList)
                findViewById<RecyclerView>(R.id.rv_learning_items).adapter = learningAdapter
                
                // Update Account section (shows all, including expired)
                purchasedAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadDownloads() {
        val videos = downloadManager.getDownloadedVideos()
        downloadList.clear()
        downloadList.addAll(videos)
        downloadAdapter.notifyDataSetChanged()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}