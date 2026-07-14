package com.example.akashvidya

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import java.util.Locale
import java.util.concurrent.TimeUnit

class OTP : AppCompatActivity() {

    private lateinit var tvResend: TextView
    private lateinit var tvSession: TextView
    private var resendTimer: CountDownTimer? = null
    private var sessionTimer: CountDownTimer? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var verificationId: String? = null
    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        verificationId = intent.getStringExtra("verificationId")
        phoneNumber = intent.getStringExtra("phoneNumber")

        tvResend = findViewById(R.id.tv_resend)
        tvSession = findViewById(R.id.tv_session_timeout)
        val otpInput = findViewById<EditText>(R.id.otp)
        val verifyBtn = findViewById<Button>(R.id.verify_btn)

        startResendTimer()
        startSessionTimer()

        verifyBtn.setOnClickListener {
            val otp = otpInput.text.toString().trim()
            if (otp.length == 6) {
                if (verificationId != null) {
                    signInWithPhoneAuthCredential(verificationId!!, otp)
                } else {
                    Toast.makeText(this, "Verification ID missing", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        tvResend.setOnClickListener {
            if (tvResend.text == getString(R.string.resend_otp)) {
                phoneNumber?.let {
                    resendOTP(it)
                } ?: Toast.makeText(this, "Phone number missing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resendOTP(number: String) {
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
            // Auto-retrieval or instant verification success during resend
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this@OTP) { task ->
                    if (task.isSuccessful) {
                        // Save login state in SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                        Toast.makeText(this@OTP, "Auto Verified Successfully", Toast.LENGTH_SHORT).show()
                        checkUserProfile()
                    } else {
                        Toast.makeText(this@OTP, "Auto Verification Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@OTP, "Resend Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            this@OTP.verificationId = verificationId
            Toast.makeText(this@OTP, "OTP Resent", Toast.LENGTH_SHORT).show()
            startResendTimer()
        }
    }

    private fun signInWithPhoneAuthCredential(verificationId: String, otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save login state in SharedPreferences
                    val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                    Toast.makeText(this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show()
                    checkUserProfile()
                } else {
                    Toast.makeText(this, "Verification Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    if (snapshot.exists()) {
                        // Profile already exists, check for goal
                        val userSnap = snapshot.children.first()
                        val goalName = userSnap.child("goalName").getValue(String::class.java)
                        val className = userSnap.child("selectedClassName").getValue(String::class.java)

                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("profileCreated", true).apply()

                        if (goalName != null && className != null) {
                            sharedPreferences.edit()
                                .putBoolean("goalSet", true)
                                .putString("userGoal", goalName)
                                .putBoolean("classSet", true)
                                .putString("userClass", className)
                                .apply()
                            val intent = Intent(this@OTP, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else if (goalName != null && className == null) {
                            val intent = Intent(this@OTP, ClassSelectionActivity::class.java)
                            intent.putExtra("goalId", userSnap.child("goalId").getValue(String::class.java))
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@OTP, PreferenceActivity::class.java)
                            startActivity(intent)
                        }
                    } else {
                        // Profile does not exist, go to Profile Create
                        val intent = Intent(this@OTP, Profile_Create::class.java)
                        intent.putExtra("phoneNumber", phoneNumber)
                        startActivity(intent)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@OTP, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvResend.text = String.format(Locale.getDefault(), "Resend OTP in 00:%02d", secondsRemaining)
                tvResend.isEnabled = false
            }

            override fun onFinish() {
                tvResend.text = getString(R.string.resend_otp)
                tvResend.isEnabled = true
            }
        }.start()
    }

    private fun startSessionTimer() {
        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(600000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                tvSession.text = String.format(Locale.getDefault(), "Session expires in %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                Toast.makeText(this@OTP, "Session expired", Toast.LENGTH_LONG).show()
                finish()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
        sessionTimer?.cancel()
    }
}