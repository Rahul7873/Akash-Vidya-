package com.akashascent.akashvidya

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ItemAdapter(private val itemList: List<ItemModel>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.item_thumbnail)
        val title: TextView = itemView.findViewById(R.id.item_title)
        val author: TextView = itemView.findViewById(R.id.item_author)
        val currentPrice: TextView = itemView.findViewById(R.id.item_current_price)
        // Hidden fields since they are not in the new data structure
        val rating: TextView = itemView.findViewById(R.id.item_rating)
        val reviews: TextView = itemView.findViewById(R.id.item_reviews)
        val originalPrice: TextView = itemView.findViewById(R.id.item_original_price)
        val premiumBadge: TextView = itemView.findViewById(R.id.item_premium_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        
        val displayName = if (item.`class` != null) "${item.name ?: "No Name"} - ${item.`class`}" else item.name ?: "No Name"
        holder.title.text = displayName
        holder.author.text = item.author ?: "Unknown Author"
        holder.currentPrice.text = "₹${item.price ?: "0"}"
        
        // Handle fields not present in current Firebase structure
        holder.rating.visibility = View.GONE
        holder.reviews.visibility = View.GONE
        holder.originalPrice.visibility = View.GONE
        holder.premiumBadge.visibility = View.GONE
        // Hide the static stars as well (would need IDs for those ImageViews to hide them individually)
        
        Glide.with(holder.itemView.context)
            .load(item.thumbnailUrl)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlaylistDetailActivity::class.java)
            intent.putExtra("playlistId", item.playlistId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = itemList.size
}
