package com.akashascent.akashvidya

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PurchasedAdapter(private val itemList: List<ItemModel>) : RecyclerView.Adapter<PurchasedAdapter.PurchasedViewHolder>() {

    class PurchasedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.p_item_name)
        val className: TextView = itemView.findViewById(R.id.p_item_class)
        val price: TextView = itemView.findViewById(R.id.p_item_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchasedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_purchased, parent, false)
        return PurchasedViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchasedViewHolder, position: Int) {
        val item = itemList[position]
        holder.name.text = item.name ?: "No Name"
        holder.className.text = "Class: ${item.`class` ?: "N/A"}"
        holder.price.text = "₹${item.price ?: "0"}"

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlaylistDetailActivity::class.java)
            intent.putExtra("playlistId", item.playlistId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = itemList.size
}
