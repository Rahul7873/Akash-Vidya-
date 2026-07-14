package com.example.akashvidya

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PreferenceAdapter(
    private val prefList: List<PreferenceModel>,
    private val onPrefClick: (PreferenceModel) -> Unit
) : RecyclerView.Adapter<PreferenceAdapter.PrefViewHolder>() {

    class PrefViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_pref_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrefViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preference, parent, false)
        return PrefViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrefViewHolder, position: Int) {
        val pref = prefList[position]
        holder.name.text = pref.name ?: "Unknown"
        holder.itemView.setOnClickListener { onPrefClick(pref) }
    }

    override fun getItemCount(): Int = prefList.size
}
