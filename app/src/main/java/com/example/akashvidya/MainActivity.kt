package com.example.akashvidya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var itemAdapter: ItemAdapter
    private val itemList = mutableListOf<ItemModel>()
    
    private lateinit var learningAdapter: ItemAdapter
    private lateinit var purchasedAdapter: PurchasedAdapter
    private val purchasedList = mutableListOf<ItemModel>()
    
    // UI Sections
    private lateinit var homeSection: NestedScrollView
    private lateinit var learningSection: NestedScrollView
    private lateinit var accountSection: NestedScrollView

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        homeSection = findViewById(R.id.home_section)
        learningSection = findViewById(R.id.learning_section)
        accountSection = findViewById(R.id.account_section)

        val userNameTv = findViewById<TextView>(R.id.user_name)
        val accFullNameTv = findViewById<TextView>(R.id.acc_full_name)
        val accPhoneTv = findViewById<TextView>(R.id.acc_phone)
        
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

    private fun performLogout() {
        auth.signOut()
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("isLoggedIn", false)
            .putBoolean("profileCreated", false)
            .remove("cachedFullName")
            .remove("cachedPhone")
            .apply()

        val intent = Intent(this, Splash::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupFooterNavigation() {
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navLearning = findViewById<LinearLayout>(R.id.nav_learning)
        val navAccount = findViewById<LinearLayout>(R.id.nav_account)

        val ivHome = findViewById<ImageView>(R.id.iv_home)
        val tvHome = findViewById<TextView>(R.id.tv_home)
        val ivLearning = findViewById<ImageView>(R.id.iv_learning)
        val tvLearning = findViewById<TextView>(R.id.tv_learning)
        val ivAccount = findViewById<ImageView>(R.id.iv_account)
        val tvAccount = findViewById<TextView>(R.id.tv_account)

        val activeColor = ContextCompat.getColor(this, R.color.primary_blue)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray_text)

        navHome.setOnClickListener {
            showSection(1)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivAccount, tvAccount, activeColor, inactiveColor, 1)
        }

        navLearning.setOnClickListener {
            showSection(2)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivAccount, tvAccount, activeColor, inactiveColor, 2)
        }

        navAccount.setOnClickListener {
            showSection(3)
            updateNavUI(ivHome, tvHome, ivLearning, tvLearning, ivAccount, tvAccount, activeColor, inactiveColor, 3)
        }
    }
    
    private fun showSection(selection: Int) {
        homeSection.visibility = if (selection == 1) View.VISIBLE else View.GONE
        learningSection.visibility = if (selection == 2) View.VISIBLE else View.GONE
        accountSection.visibility = if (selection == 3) View.VISIBLE else View.GONE
    }

    private fun updateNavUI(
        ivH: ImageView, tvH: TextView,
        ivL: ImageView, tvL: TextView,
        ivA: ImageView, tvA: TextView,
        active: Int, inactive: Int, selection: Int
    ) {
        ivH.setColorFilter(inactive); tvH.setTextColor(inactive)
        ivL.setColorFilter(inactive); tvL.setTextColor(inactive)
        ivA.setColorFilter(inactive); tvA.setTextColor(inactive)

        when (selection) {
            1 -> { ivH.setColorFilter(active); tvH.setTextColor(active) }
            2 -> { ivL.setColorFilter(active); tvL.setTextColor(active) }
            3 -> { ivA.setColorFilter(active); tvA.setTextColor(active) }
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
                        for (userSnap in userSnapshot.children) {
                            val purchasedIds = mutableListOf<String>()
                            val pSnap = userSnap.child("purchased_playlists")
                            for (idSnap in pSnap.children) {
                                idSnap.key?.let { purchasedIds.add(it) }
                            }
                            
                            if (purchasedIds.isNotEmpty()) {
                                loadPurchasedDetails(purchasedIds)
                            } else {
                                purchasedList.clear()
                                learningAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadPurchasedDetails(ids: List<String>) {
        database.getReference("playlists").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                purchasedList.clear()
                for (itemSnap in snapshot.children) {
                    val item = itemSnap.getValue(ItemModel::class.java)
                    if (item != null && ids.contains(item.playlistId)) {
                        purchasedList.add(item)
                    }
                }
                learningAdapter.notifyDataSetChanged()
                purchasedAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
