package com.akashascent.akashvidya

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.content.FileProvider

class PlaylistDetailActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<VideoModel>()
    private var playlistId: String? = null
    private var currentItem: ItemModel? = null
    private lateinit var btnBuyNow: Button
    
    private var userFirstName: String? = null
    private var userLastName: String? = null
    private var userPhoneNumber: String? = null
    
    private var isExpanded = false
    private lateinit var layoutShowMore: View
    private lateinit var tvDescription: TextView
    private lateinit var tvShowMore: TextView
    private lateinit var ivShowMoreArrow: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_playlist_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        playlistId = intent.getStringExtra("playlistId")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        btnBuyNow = findViewById(R.id.btn_buy_now)
        layoutShowMore = findViewById(R.id.layout_show_more)
        tvDescription = findViewById(R.id.playlist_description)
        tvShowMore = findViewById(R.id.tv_show_more)
        ivShowMoreArrow = findViewById(R.id.iv_show_more_arrow)

        val toggleDescription = {
            isExpanded = !isExpanded
            tvDescription.maxLines = if (isExpanded) Integer.MAX_VALUE else 3
            tvShowMore.text = if (isExpanded) "Show Less" else "Show More"
            ivShowMoreArrow.setImageResource(if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
        }

        layoutShowMore.setOnClickListener { toggleDescription() }
        tvDescription.setOnClickListener {
            if (layoutShowMore.visibility == View.VISIBLE) {
                toggleDescription()
            }
        }

        val rvVideos = findViewById<RecyclerView>(R.id.rv_videos)
        rvVideos.layoutManager = LinearLayoutManager(this)
        videoAdapter = VideoAdapter(videoList, playlistId)
        rvVideos.adapter = videoAdapter

        // Preload Razorpay
        Checkout.preload(applicationContext)

        if (playlistId != null) {
            checkPurchaseStatus()
            fetchPlaylistDetails()
            fetchVideos()
        } else {
            Toast.makeText(this, "Error: Invalid Playlist", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnBuyNow.setOnClickListener {
            currentItem?.let { startPayment(it) }
        }
    }

    private fun checkPurchaseStatus() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    var isPurchased = false
                    var isCourseExpired = false
                    
                    if (snapshot.exists()) {
                        val userSnap = snapshot.children.first()
                        
                        userFirstName = userSnap.child("firstName").getValue(String::class.java)
                        userLastName = userSnap.child("lastName").getValue(String::class.java)
                        userPhoneNumber = userSnap.child("phoneNumber").getValue(String::class.java)

                        val purchaseSnap = userSnap.child("purchased_playlists").child(playlistId!!)
                        if (purchaseSnap.exists()) {
                            isPurchased = true
                            val expiryStr = purchaseSnap.child("expiryDate").getValue(String::class.java)
                            if (expiryStr != null) {
                                try {
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    val expiryDate = sdf.parse(expiryStr)
                                    if (expiryDate != null && expiryDate.before(Date())) {
                                        isCourseExpired = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    
                    // Hide buy button only if purchased AND NOT expired
                    btnBuyNow.visibility = if (isPurchased && !isCourseExpired) View.GONE else View.VISIBLE
                    if (isCourseExpired) {
                        btnBuyNow.text = "Validity Expired - Buy Again"
                        videoAdapter.updatePurchaseStatus(false)
                    } else {
                        videoAdapter.updatePurchaseStatus(isPurchased)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchPlaylistDetails() {
        database.getReference("playlists").child(playlistId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    currentItem = snapshot.getValue(ItemModel::class.java)
                    currentItem?.let { item ->
                        findViewById<TextView>(R.id.playlist_name).text = item.name
                        tvDescription.text = item.description
                        
                        // Initial state: No max lines so we can count them
                        tvDescription.maxLines = Integer.MAX_VALUE
                        
                        tvDescription.post {
                            if (tvDescription.lineCount > 3) {
                                layoutShowMore.visibility = View.VISIBLE
                                tvDescription.maxLines = 3
                                isExpanded = false
                            } else {
                                layoutShowMore.visibility = View.GONE
                            }
                        }
                        
                        if (!isFinishing && !isDestroyed) {
                            Glide.with(this@PlaylistDetailActivity)
                                .load(item.thumbnailUrl)
                                .placeholder(R.drawable.logo)
                                .into(findViewById<ImageView>(R.id.playlist_thumbnail))
                        }
                        
                        btnBuyNow.text = "Buy Now - ₹${item.price ?: "0"}"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchVideos() {
        database.getReference("playlists").child(playlistId!!).child("videos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    videoList.clear()
                    if (snapshot.exists()) {
                        for (videoSnap in snapshot.children) {
                            val video = videoSnap.getValue(VideoModel::class.java)
                            if (video != null) {
                                // Inject playlist info for downloads
                                val updatedVideo = video.copy(
                                    playlistId = playlistId,
                                    playlistName = currentItem?.name
                                )
                                videoList.add(updatedVideo)
                            }
                        }
                    }
                    videoAdapter.notifyDataSetChanged()
                    findViewById<TextView>(R.id.video_count).text = "${videoList.size} Videos"
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PlaylistDetailActivity, "Failed to load videos: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startPayment(item: ItemModel) {
        val checkout = Checkout()
        // Replace with your real Razorpay Key ID
        checkout.setKeyID("rzp_live_TV4wOG6zm1OgDJ")
        
        try {
            val options = JSONObject()
            options.put("name", "Akash Ascent")
            options.put("description", item.name)
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#007BFF")
            options.put("currency", "INR")
            
            // Amount is in paisa (100 paisa = 1 Rupee)
            val price = (item.price?.replace(",", "")?.toDoubleOrNull() ?: 0.0) * 100
            options.put("amount", price.toInt().toString())
            
            val prefill = JSONObject()
            prefill.put("email", "") // Optional: Get from user profile if available
            prefill.put("contact", auth.currentUser?.phoneNumber ?: "")
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val userId = auth.currentUser?.uid ?: return
        val pId = playlistId ?: return
        val item = currentItem ?: return
        val paymentId = razorpayPaymentId ?: "N/A"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val now = Date()
        val purchaseDate = sdf.format(now)
        
        // Calculate Expiry
        val calendar = Calendar.getInstance()
        calendar.time = now
        
        val endDay = item.getEndDay()
        val endMonth = item.getEndMonth()
        
        if (endDay > 0 && endMonth > 0 && endMonth <= 12) {
            // Fixed Date Logic: Expires on the next occurrence of this date (e.g. 5th March)
            calendar.set(Calendar.MONTH, endMonth - 1)
            calendar.set(Calendar.DAY_OF_MONTH, endDay)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            
            // If that date has already passed this year, move to next year
            if (calendar.time.before(now)) {
                calendar.add(Calendar.YEAR, 1)
            }
        } else {
            // Duration Logic: e.g. "6 months" or "1 year" from now
            val months = (item.durationMonths ?: item.months ?: item.month)?.toString()?.toIntOrNull() ?: 0
            val days = (item.durationDays ?: item.days ?: item.day)?.toString()?.toIntOrNull() ?: 0
            
            if (months == 0 && days == 0) {
                calendar.add(Calendar.YEAR, 1)
            } else {
                if (months > 0) calendar.add(Calendar.MONTH, months)
                if (days > 0) calendar.add(Calendar.DAY_OF_YEAR, days)
            }
        }

        val expiryDate = sdf.format(calendar.time)

        // Direct Success handling without backend verification
        savePurchaseToFirebase(userId, pId, paymentId, purchaseDate, expiryDate)
        
        // Automatically generate a local invoice
        InvoiceGenerator.generateAndOpenInvoice(
            this,
            paymentId,
            purchaseDate,
            item,
            "${userFirstName ?: ""} ${userLastName ?: ""}".trim(),
            userPhoneNumber ?: ""
        )
    }

    private fun savePurchaseToFirebase(userId: String, pId: String, paymentId: String, purchaseDate: String, expiryDate: String) {
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userSnap = snapshot.children.first()
                        val purchaseData = mapOf(
                            "paymentId" to paymentId,
                            "purchaseDate" to purchaseDate,
                            "expiryDate" to expiryDate,
                            "playlistId" to pId,
                            "isPurchased" to true
                        )
                        userSnap.ref.child("purchased_playlists").child(pId).setValue(purchaseData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@PlaylistDetailActivity, "Purchase Successful!", Toast.LENGTH_LONG).show()
                                    btnBuyNow.visibility = View.GONE
                                }
                            }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}
