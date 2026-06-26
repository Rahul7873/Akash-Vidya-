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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hbb20.CountryCodePicker
import java.util.concurrent.TimeUnit

class phone : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var countryCodePicker: CountryCodePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_phone)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        val phoneNumber = findViewById<EditText>(R.id.phone_number)
        countryCodePicker = findViewById(R.id.ccp)
        val signInBtn = findViewById<Button>(R.id.btn)

        // Register the phone number EditText with the CountryCodePicker
        countryCodePicker.registerCarrierNumberEditText(phoneNumber)

        signInBtn.setOnClickListener {
            val number = phoneNumber.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, "Please provide phone number", Toast.LENGTH_SHORT).show()
            } else {
                val fullNumber = countryCodePicker.fullNumberWithPlus
                sendVerificationCode(fullNumber)
            }
        }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This enables instant verification or auto-retrieval of the SMS code
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this@phone) { task ->
                    if (task.isSuccessful) {
                        // Save login state in SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                        Toast.makeText(this@phone, "Auto Verified Successfully", Toast.LENGTH_SHORT).show()
                        checkUserProfile()
                    } else {
                        Toast.makeText(this@phone, "Auto Verification Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@phone, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            val intent = Intent(this@phone, OTP::class.java)
            intent.putExtra("verificationId", verificationId)
            intent.putExtra("phoneNumber", countryCodePicker.fullNumberWithPlus)
            startActivity(intent)
        }
    }

    private fun checkUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Profile already exists
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("profileCreated", true).apply()

                        val intent = Intent(this@phone, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Profile does not exist, go to Profile Create
                        val intent = Intent(this@phone, Profile_Create::class.java)
                        intent.putExtra("phoneNumber", countryCodePicker.fullNumberWithPlus)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@phone, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
