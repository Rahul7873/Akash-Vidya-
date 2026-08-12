package com.akashascent.akashvidya

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.akashascent.akashvidya.network.OtpSendRequest
import com.akashascent.akashvidya.network.RetrofitClient
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.launch

class phone : AppCompatActivity() {

    private lateinit var countryCodePicker: CountryCodePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_phone)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val phoneNumber = findViewById<EditText>(R.id.phone_number)
        countryCodePicker = findViewById(R.id.ccp)
        val signInBtn = findViewById<Button>(R.id.btn)

        countryCodePicker.registerCarrierNumberEditText(phoneNumber)

        signInBtn.setOnClickListener {
            val number = phoneNumber.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, "Please provide phone number", Toast.LENGTH_SHORT).show()
            } else {
                val fullNumber = countryCodePicker.fullNumberWithPlus // Keep the + sign
                
                // Show loading state
                signInBtn.isEnabled = false
                signInBtn.text = "Sending..."
                
                val generatedOtp = (100000..999999).random().toString()
                
                sendOtpViaBackend(fullNumber, generatedOtp) { success, message ->
                    signInBtn.isEnabled = true
                    signInBtn.text = "Sign In"
                    
                    if (success) {
                        val intent = Intent(this@phone, OTP::class.java)
                        intent.putExtra("phoneNumber", fullNumber)
                        intent.putExtra("sentOtp", generatedOtp)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Failed to send OTP: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun sendOtpViaBackend(number: String, otp: String, onResult: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            try {
                // Remove + for the API if it expects it without +
                val apiNumber = number.replace("+", "")
                val request = OtpSendRequest(mobile = apiNumber, otp = otp)
                val response = RetrofitClient.apiService.sendOtp(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, "Success")
                } else {
                    val errorBody = response.errorBody()?.string()
                    onResult(false, response.body()?.message ?: "Server Error: ${response.code()} $errorBody")
                }
            } catch (e: Exception) {
                onResult(false, "Network Error: ${e.message}")
            }
        }
    }
}
