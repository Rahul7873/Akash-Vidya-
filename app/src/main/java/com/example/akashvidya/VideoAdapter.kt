package com.example.akashvidya

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
    private val onVideoClick: ((VideoModel) -> Unit)? = null
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        val title: TextView = itemView.findViewById(R.id.video_title)
        val serial: TextView = itemView.findViewById(R.id.video_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.title.text = video.title ?: "Untitled Video"
        holder.serial.text = "Video ${video.serial ?: (position + 1).toString()}"
        
        Glide.with(holder.itemView.context)
            .load(video.thumbnailUrl)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            if (onVideoClick != null) {
                onVideoClick.invoke(video)
            } else {
                val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
                intent.putExtra("videoUrl", video.videoUrl)
                intent.putExtra("playlistId", playlistId)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = videoList.size
}
