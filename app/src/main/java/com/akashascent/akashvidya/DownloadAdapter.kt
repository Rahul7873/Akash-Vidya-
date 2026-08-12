package com.akashascent.akashvidya

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DownloadAdapter(
    private val downloadList: MutableList<VideoModel>,
    private val onDeleteClick: (VideoModel) -> Unit
) : RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder>() {

    class DownloadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        val title: TextView = itemView.findViewById(R.id.video_title)
        val serial: TextView = itemView.findViewById(R.id.video_duration)
        val playlistName: TextView = itemView.findViewById(R.id.tv_playlist_name)
        val btnMore: ImageButton = itemView.findViewById(R.id.btn_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_download, parent, false)
        return DownloadViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val video = downloadList[position]
        holder.title.text = video.title ?: "Downloaded Video"
        holder.serial.text = "Video ${video.serial ?: (position + 1).toString()}"
        holder.playlistName.text = video.playlistName ?: "Unknown Playlist"

        Glide.with(holder.itemView.context)
            .load(video.thumbnailUrl)
            .placeholder(R.drawable.logo)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
            intent.putExtra("videoUrl", video.videoUrl)
            intent.putExtra("localPath", video.localPath)
            intent.putExtra("playlistId", video.playlistId)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnMore.setOnClickListener {
            val popup = PopupMenu(holder.itemView.context, holder.btnMore)
            popup.menu.add("Delete")
            popup.setOnMenuItemClickListener {
                if (it.title == "Delete") {
                    onDeleteClick(video)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = downloadList.size
}
