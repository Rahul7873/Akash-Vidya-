package com.example.akashvidya

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import java.util.Locale

class Profile_Create : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_create)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val firstName = findViewById<EditText>(R.id.F_name)
        val lastName = findViewById<EditText>(R.id.L_name)
        val submitBtn = findViewById<Button>(R.id.submit_btn)

        submitBtn.setOnClickListener {
            val fName = firstName.text.toString().trim()
            val lName = lastName.text.toString().trim()
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""

            if (fName.isEmpty() || lName.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                saveUserProfile(fName, lName, phoneNumber)
            }
        }
    }

    private fun saveUserProfile(fName: String, lName: String, phoneNumber: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Find the first available sequential User ID starting from 01 (to fill gaps if any are deleted)
        database.getReference("users").get().addOnSuccessListener { snapshot ->
            val existingIds = mutableSetOf<Int>()
            for (userSnap in snapshot.children) {
                val idStr = userSnap.key ?: ""
                idStr.toIntOrNull()?.let { existingIds.add(it) }
            }

            var nextId = 1
            while (existingIds.contains(nextId)) {
                nextId++
            }

            val userIdNumber = String.format(Locale.getDefault(), "%02d", nextId)

            val userMap = mapOf(
                "uid" to userId,
                "firstName" to fName,
                "lastName" to lName,
                "phoneNumber" to phoneNumber,
                "userIdNumber" to userIdNumber
            )

            database.getReference("users").child(userIdNumber).setValue(userMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Save profile created state in SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("profileCreated", true).apply()

                        Toast.makeText(this@Profile_Create, "Profile Created Successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Profile_Create, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@Profile_Create, "Failed to save profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Error fetching users: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
