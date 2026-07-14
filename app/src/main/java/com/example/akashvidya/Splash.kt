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
        }, 2000)
    }
}