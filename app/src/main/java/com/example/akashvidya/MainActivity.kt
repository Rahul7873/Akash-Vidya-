package com.example.akashvidya

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val userNameTv = findViewById<TextView>(R.id.user_name)

        // Check cache first
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val cachedName = sharedPreferences.getString("cachedFullName", null)

        if (cachedName != null) {
            userNameTv.text = cachedName
        } else {
            fetchUserData(userNameTv)
        }
    }

    private fun fetchUserData(userNameTv: TextView) {
        val userId = auth.currentUser?.uid ?: return

        // We use orderByChild("uid") because the node key is the sequential ID (01, 02, etc.)
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // snapshot.children will contain the user node
                        for (userSnap in snapshot.children) {
                            val firstName = userSnap.child("firstName").getValue(String::class.java) ?: ""
                            val lastName = userSnap.child("lastName").getValue(String::class.java) ?: ""
                            
                            val fullName = "$firstName $lastName".trim()
                            val displayName = if (fullName.isNotEmpty()) fullName else "User"
                            
                            userNameTv.text = displayName
                            
                            // Save to cache
                            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            sharedPreferences.edit().putString("cachedFullName", displayName).apply()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
