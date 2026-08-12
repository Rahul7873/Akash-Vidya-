package com.akashascent.akashvidya

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter(
    private val videoList: List<VideoModel>,
    private val playlistId: String? = null,
    private var isPurchased: Boolean = false,
    private val onVideoClick: ((VideoModel) -> Unit)? = null
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var downloadManager: DownloadManager? = null

    fun updatePurchaseStatus(purchased: Boolean) {
        this.isPurchased = purchased
        notifyDataSetChanged()
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        val title: TextView = itemView.findViewById(R.id.video_title)
        val serial: TextView = itemView.findViewById(R.id.video_duration)
        val btnDownload: View = itemView.findViewById(R.id.btn_download)
        val ivLock: ImageView = itemView.findViewById(R.id.iv_lock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.title.text = video.title ?: "Untitled Video"
        holder.serial.text = "Video ${video.serial ?: (position + 1).toString()}"
        
        val canAccess = isPurchased || video.isDemo

        if (downloadManager == null) {
            downloadManager = DownloadManager(holder.itemView.context)
        }

        Glide.with(holder.itemView.context)
            .load(video.thumbnailUrl)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            if (canAccess) {
                val downloadedVideo = downloadManager?.getDownloadedVideo(video.videoUrl)
                val finalVideo = downloadedVideo ?: video

                if (onVideoClick != null) {
                    onVideoClick.invoke(finalVideo)
                } else {
                    val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
                    intent.putExtra("videoUrl", finalVideo.videoUrl)
                    intent.putExtra("localPath", finalVideo.localPath)
                    intent.putExtra("playlistId", playlistId)
                    holder.itemView.context.startActivity(intent)
                }
            } else {
                android.widget.Toast.makeText(holder.itemView.context, "Purchase the course to unlock this video", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        holder.btnDownload.visibility = if (isPurchased) View.VISIBLE else View.GONE
        holder.ivLock.visibility = if (canAccess) View.GONE else View.VISIBLE

        holder.btnDownload.setOnClickListener {
            if (isPurchased) {
                downloadManager?.startDownload(video)
                android.widget.Toast.makeText(holder.itemView.context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(holder.itemView.context, "Purchase required to download", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Demo Badge if needed
        if (video.isDemo && !isPurchased) {
            holder.serial.text = "DEMO VIDEO"
            holder.serial.setTextColor(android.graphics.Color.GREEN)
        } else {
            holder.serial.setTextColor(holder.itemView.context.getColor(R.color.gray_text))
        }
    }

    override fun getItemCount(): Int = videoList.size
}
