package com.akashascent.akashvidya

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PurchasedAdapter(private val itemList: List<ItemModel>) : RecyclerView.Adapter<PurchasedAdapter.PurchasedViewHolder>() {

    class PurchasedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.p_item_name)
        val className: TextView = itemView.findViewById(R.id.p_item_class)
        val price: TextView = itemView.findViewById(R.id.p_item_price)
        val btnInvoice: ImageView = itemView.findViewById(R.id.btn_invoice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchasedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_purchased, parent, false)
        return PurchasedViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchasedViewHolder, position: Int) {
        val item = itemList[position]
        holder.name.text = item.name ?: "No Name"
        
        if (item.isExpired) {
            holder.className.text = "EXPIRED on ${item.expiryDate?.split(" ")?.get(0) ?: "N/A"}"
            holder.className.setTextColor(android.graphics.Color.RED)
            holder.price.text = "Renew"
            holder.price.setTextColor(android.graphics.Color.RED)
        } else {
            holder.className.text = "Class: ${item.`class` ?: "N/A"}"
            holder.className.setTextColor(holder.itemView.context.getColor(R.color.gray_text))
            holder.price.text = "₹${item.price ?: "0"}"
            holder.price.setTextColor(holder.itemView.context.getColor(R.color.primary_blue))
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlaylistDetailActivity::class.java)
            intent.putExtra("playlistId", item.playlistId)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnInvoice.setOnClickListener {
            val context = holder.itemView.context
            val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val userName = sharedPreferences.getString("cachedFullName", "User") ?: "User"
            val userPhone = sharedPreferences.getString("cachedPhone", "N/A") ?: "N/A"

            if (item.paymentId != null && item.purchaseDate != null) {
                InvoiceGenerator.generateAndOpenInvoice(
                    context,
                    item.paymentId,
                    item.purchaseDate,
                    item,
                    userName,
                    userPhone
                )
            } else {
                android.widget.Toast.makeText(context, "Invoice data not available for this purchase", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = itemList.size
}
