package com.akashascent.akashvidya

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class VideoPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var videoListContainer: View
    private lateinit var rvVideos: RecyclerView
    private val videoList = mutableListOf<VideoModel>()
    private lateinit var videoAdapter: VideoAdapter
    private var playlistId: String? = null
    private var isPurchased: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_player)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.loading_progress)
        videoListContainer = findViewById(R.id.video_list_container)
        rvVideos = findViewById(R.id.rv_player_videos)
        val btnVideoList = findViewById<ImageButton>(R.id.btn_video_list)

        playlistId = intent.getStringExtra("playlistId")
        val videoUrl = intent.getStringExtra("videoUrl")
        val localPath = intent.getStringExtra("localPath")

        if (playlistId != null) {
            checkPurchaseStatus()
        }

        // Setup RecyclerView
        rvVideos.layoutManager = LinearLayoutManager(this)
        videoAdapter = VideoAdapter(videoList, playlistId = playlistId, onVideoClick = { selectedVideo ->
            val urlToPlay = selectedVideo.localPath ?: selectedVideo.videoUrl
            urlToPlay?.let { url ->
                playVideo(url)
                videoListContainer.visibility = View.GONE
                Toast.makeText(this, "Playing: ${selectedVideo.title}", Toast.LENGTH_SHORT).show()
            }
        })
        rvVideos.adapter = videoAdapter

        if (localPath != null && java.io.File(localPath).exists()) {
            initializePlayer(localPath)
        } else if (videoUrl != null) {
            initializePlayer(videoUrl)
        } else {
            finish()
        }

        if (playlistId != null) {
            fetchPlaylistVideos()
        }

        btnVideoList.setOnClickListener {
            if (videoListContainer.visibility == View.VISIBLE) {
                videoListContainer.visibility = View.GONE
            } else {
                videoListContainer.visibility = View.VISIBLE
            }
        }

        // Handle back press to enter Picture-in-Picture mode
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (videoListContainer.visibility == View.VISIBLE) {
                    videoListContainer.visibility = View.GONE
                } else {
                    enterPiPMode()
                }
            }
        })
    }

    private fun fetchPlaylistVideos() {
        if (playlistId == null) return
        
        val database = FirebaseDatabase.getInstance()
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
                    
                    if (videoList.isEmpty()) {
                        Toast.makeText(this@VideoPlayerActivity, "No other videos in this playlist", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VideoPlayerActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkPurchaseStatus() {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: return
        
        database.getReference("users").orderByChild("uid").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    if (snapshot.exists()) {
                        val userSnap = snapshot.children.first()
                        isPurchased = userSnap.child("purchased_playlists").hasChild(playlistId!!)
                        videoAdapter.updatePurchaseStatus(isPurchased)
                        
                        // Also check if the current playing video is allowed
                        // This is a safety check. If they somehow opened the player, we check here.
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun playVideo(url: String) {
        progressBar.visibility = View.VISIBLE
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun enterPiPMode() {
        val aspectRatio = Rational(16, 9)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPiPMode()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerView.useController = false
            progressBar.visibility = View.GONE
            videoListContainer.visibility = View.GONE
            findViewById<View>(R.id.btn_video_list).visibility = View.GONE
        } else {
            playerView.useController = true
            findViewById<View>(R.id.btn_video_list).visibility = View.VISIBLE
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(videoUrl: String) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                        Player.STATE_READY -> progressBar.visibility = View.GONE
                        else -> progressBar.visibility = View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    progressBar.visibility = View.GONE
                }
            })
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
