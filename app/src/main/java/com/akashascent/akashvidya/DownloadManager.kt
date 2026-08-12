package com.akashascent.akashvidya

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class DownloadManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("Downloads", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun getDownloadedVideos(): MutableList<VideoModel> {
        val json = prefs.getString("downloaded_videos", "[]")
        val type = object : TypeToken<MutableList<VideoModel>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveDownloadedVideos(videos: List<VideoModel>) {
        val json = gson.toJson(videos)
        prefs.edit().putString("downloaded_videos", json).apply()
    }

    fun startDownload(video: VideoModel) {
        if (video.videoUrl == null) return
        
        val videos = getDownloadedVideos()
        if (videos.any { it.videoUrl == video.videoUrl }) return

        // Create a unique filename
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        
        // Use getExternalFilesDir so it's private to the app and NOT in the gallery
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)

        val request = DownloadManager.Request(Uri.parse(video.videoUrl))
            .setTitle(video.title ?: "Downloading Video")
            .setDescription("Downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        systemDownloadManager.enqueue(request)

        // Save metadata with the local path
        val videoWithLocalPath = video.copy(localPath = destinationFile.absolutePath)
        videos.add(videoWithLocalPath)
        saveDownloadedVideos(videos)
    }

    fun isDownloaded(videoUrl: String?): Boolean {
        if (videoUrl == null) return false
        val videos = getDownloadedVideos()
        return videos.any { it.videoUrl == videoUrl }
    }

    fun getDownloadedVideo(videoUrl: String?): VideoModel? {
        if (videoUrl == null) return null
        return getDownloadedVideos().find { it.videoUrl == videoUrl }
    }

    fun removeDownload(videoUrl: String) {
        val videos = getDownloadedVideos()
        val video = videos.find { it.videoUrl == videoUrl }
        if (video != null) {
            // Delete the physical file
            video.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            videos.remove(video)
            saveDownloadedVideos(videos)
        }
    }
}
