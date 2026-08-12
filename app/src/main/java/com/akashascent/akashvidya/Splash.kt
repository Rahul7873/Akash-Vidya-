package com.akashascent.akashvidya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val currentUid = auth.currentUser?.uid
        val currentDeviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)

        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

            if (isLoggedIn && currentUid != null) {
                // Check device ID mismatch
                database.getReference("users").orderByChild("uid").equalTo(currentUid)
                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            var deviceMatch = true
                            if (snapshot.exists()) {
                                val userSnap = snapshot.children.first()
                                val savedDeviceId = userSnap.child("deviceId").getValue(String::class.java)
                                if (savedDeviceId != null && savedDeviceId != currentDeviceId) {
                                    deviceMatch = false
                                }
                            }

                            if (!deviceMatch) {
                                // Logout due to multi-device login
                                auth.signOut()
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
                                Toast.makeText(this@Splash, "Logged out: Logged in on another device", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this@Splash, phone::class.java))
                                finish()
                            } else {
                                proceedToNextActivity()
                            }
                        }

                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            // On error, let's just proceed to avoid blocking user, or handle as needed
                            proceedToNextActivity()
                        }
                    })
            } else {
                proceedToNextActivity()
            }
        }, 2000)
    }

    private fun proceedToNextActivity() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val isProfileCreated = sharedPreferences.getBoolean("profileCreated", false)
        val isGoalSet = sharedPreferences.getBoolean("goalSet", false)
        val isClassSet = sharedPreferences.getBoolean("classSet", false)

        if (isLoggedIn && isProfileCreated && isGoalSet && isClassSet) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else if (isLoggedIn && isProfileCreated && !isGoalSet) {
            val intent = Intent(this, PreferenceActivity::class.java)
            startActivity(intent)
        } else if (isLoggedIn && isProfileCreated && isGoalSet && !isClassSet) {
            // Should technically have goalId, but for safety let's go to Preference to be sure
            val intent = Intent(this, PreferenceActivity::class.java)
            startActivity(intent)
        } else if (isLoggedIn && !isProfileCreated) {
            val intent = Intent(this, phone::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, phone::class.java)
            startActivity(intent)
        }
        finish()
    }
}
