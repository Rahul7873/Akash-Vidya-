package com.example.akashvidya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            val isProfileCreated = sharedPreferences.getBoolean("profileCreated", false)

            if (isLoggedIn && isProfileCreated) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else if (isLoggedIn && !isProfileCreated) {
                // This case handles if the app was closed during profile creation
                // We go back to phone to ensure fresh verification or we could go to Profile_Create directly
                // For safety, let's go to phone. If already logged in, Firebase can handle it.
                val intent = Intent(this, phone::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, phone::class.java)
                startActivity(intent)
            }
            finish()
        }, 2000)
    }
}