package com.akashascent.akashvidya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PreferenceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: PreferenceAdapter
    private val prefList = mutableListOf<PreferenceModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preference)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val rv = findViewById<RecyclerView>(R.id.rv_preferences)
        val pb = findViewById<ProgressBar>(R.id.pb_loading)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = PreferenceAdapter(prefList) { selectedPref ->
            saveUserPreference(selectedPref)
        }
        rv.adapter = adapter

        fetchPreferences(pb)
    }

    private fun fetchPreferences(pb: ProgressBar) {
        pb.visibility = View.VISIBLE
        database.getReference("preferences").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                pb.visibility = View.GONE
                prefList.clear()
                for (prefSnap in snapshot.children) {
                    val pref = prefSnap.getValue(PreferenceModel::class.java)
                    if (pref != null) {
                        prefList.add(pref)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                pb.visibility = View.GONE
                Toast.makeText(this@PreferenceActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserPreference(pref: PreferenceModel) {
        val userId = auth.currentUser?.uid ?: return
        
        // Find user node by UID
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnap in snapshot.children) {
                            userSnap.ref.child("goalId").setValue(pref.id)
                            userSnap.ref.child("goalName").setValue(pref.name)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                        sharedPreferences.edit()
                                            .putBoolean("goalSet", true)
                                            .putString("userGoal", pref.name)
                                            .apply()

                                        Toast.makeText(this@PreferenceActivity, "Goal set to ${pref.name}", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this@PreferenceActivity, ClassSelectionActivity::class.java)
                                        intent.putExtra("goalId", pref.id)
                                        startActivity(intent)
                                        // Do not finish so user can go back to change goal
                                    }
                                }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
