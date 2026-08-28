package com.akashascent.akashvidya

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.*

@IgnoreExtraProperties
data class ItemModel(
    val playlistId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val author: String? = null,
    val price: String? = null,
    val thumbnailUrl: String? = null,
    val `class`: String? = null,
    val courseDuration: String? = null, // Direct display field from Firebase
    
    // Support for specific naming conventions from Firebase (used for auto-cancellation logic)
    val durationEndDay: Any? = null,
    val durationEndMonth: Any? = null,
    val durationDays: Any? = null,
    val durationMonths: Any? = null,
    val days: Any? = null,
    val months: Any? = null,
    val day: Any? = null,
    val month: Any? = null,
    
    val validity: String? = null, 
    val createdAt: Long? = null,
    
    // Purchase specific fields
    val paymentId: String? = null,
    val purchaseDate: String? = null,
    val expiryDate: String? = null,
    val isExpired: Boolean = false
) {
    @Exclude
    fun getEndDay(): Int {
        val raw = (durationEndDay ?: days ?: day ?: validity)?.toString() ?: return 0
        val digits = raw.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
    
    @Exclude
    fun getEndMonth(): Int {
        val raw = (durationEndMonth ?: months ?: month)?.toString()?.lowercase() ?: return 0
        val monthInt = raw.toIntOrNull()
        if (monthInt != null) return monthInt
        
        return when {
            raw.contains("jan") -> 1
            raw.contains("feb") -> 2
            raw.contains("mar") -> 3
            raw.contains("apr") -> 4
            raw.contains("may") -> 5
            raw.contains("jun") -> 6
            raw.contains("jul") -> 7
            raw.contains("aug") -> 8
            raw.contains("sep") -> 9
            raw.contains("oct") -> 10
            raw.contains("nov") -> 11
            raw.contains("dec") -> 12
            else -> 0
        }
    }

    @Exclude
    fun getFormattedValidity(): String {
        // Priority 1: Use the exact string from Firebase courseDuration if available
        if (!courseDuration.isNullOrEmpty()) {
            return courseDuration
        }

        // Priority 2: Try to format from Day/Month fields
        val d = getEndDay()
        val m = getEndMonth()
        
        if (d > 0 && m > 0 && m <= 12) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, m - 1)
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
            
            val suffix = when {
                d in 11..13 -> "th"
                d % 10 == 1 -> "st"
                d % 10 == 2 -> "nd"
                d % 10 == 3 -> "rd"
                else -> "th"
            }
            return "$d$suffix $monthName Every Year"
        }
        
        // Priority 3: Fallback to duration logic
        val durMonths = (durationMonths ?: months ?: month)?.toString()?.toIntOrNull() ?: 0
        val durDays = (durationDays ?: days ?: day)?.toString()?.toIntOrNull() ?: 0
        
        val sb = StringBuilder()
        if (durMonths > 0) sb.append("$durMonths Months ")
        if (durDays > 0) sb.append("$durDays Days")
        
        return if (sb.isEmpty()) "Lifetime" else sb.toString().trim()
    }
}
