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

class ClassSelectionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: ClassAdapter
    private val classList = mutableListOf<ClassModel>()
    private var goalId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_class_selection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        goalId = intent.getStringExtra("goalId")

        val rv = findViewById<RecyclerView>(R.id.rv_classes)
        val pb = findViewById<ProgressBar>(R.id.pb_loading_classes)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = ClassAdapter(classList) { selectedClass ->
            saveUserClassPreference(selectedClass)
        }
        rv.adapter = adapter

        if (goalId != null) {
            fetchClassesForGoal(pb)
        } else {
            Toast.makeText(this, "Error: No goal selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchClassesForGoal(pb: ProgressBar) {
        pb.visibility = View.VISIBLE
        // Fetch classes directly from the goal's 'classes' node
        database.getReference("preferences").child(goalId!!).child("classes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    pb.visibility = View.GONE
                    classList.clear()
                    
                    if (snapshot.exists()) {
                        for (classSnap in snapshot.children) {
                            val classModel = classSnap.getValue(ClassModel::class.java)
                            if (classModel != null) {
                                // Set the ID from the node key and add to list
                                classList.add(classModel.copy(id = classSnap.key))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ClassSelectionActivity, "No classes found for this goal", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    pb.visibility = View.GONE
                    Toast.makeText(this@ClassSelectionActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveUserClassPreference(classItem: ClassModel) {
        val userId = auth.currentUser?.uid ?: return
        
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnap in snapshot.children) {
                            userSnap.ref.child("selectedClassId").setValue(classItem.id)
                            userSnap.ref.child("selectedClassName").setValue(classItem.name)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                        sharedPreferences.edit()
                                            .putBoolean("classSet", true)
                                            .putString("userClass", classItem.name)
                                            .apply()

                                        val intent = Intent(this@ClassSelectionActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
