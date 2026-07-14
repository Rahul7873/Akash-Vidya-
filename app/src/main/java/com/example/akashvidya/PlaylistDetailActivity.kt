package com.example.akashvidya

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class PlaylistDetailActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<VideoModel>()
    private var playlistId: String? = null
    private var currentItem: ItemModel? = null
    private lateinit var btnBuyNow: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        btnBuyNow = findViewById(R.id.btn_buy_now)
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
                    if (snapshot.exists()) {
                        val userSnap = snapshot.children.first()
                        if (userSnap.child("purchased_playlists").hasChild(playlistId!!)) {
                            isPurchased = true
                        }
                    }
                    btnBuyNow.visibility = if (isPurchased) View.GONE else View.VISIBLE
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
                        findViewById<TextView>(R.id.playlist_description).text = item.description
                        
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
                                videoList.add(video)
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
        checkout.setKeyID("rzp_test_TD1yL2jrvH9r9a")
        
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
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userSnap = snapshot.children.first()
                        userSnap.ref.child("purchased_playlists").child(playlistId!!).setValue(true)
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
