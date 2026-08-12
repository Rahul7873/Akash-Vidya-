package com.akashascent.akashvidya

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ClassAdapter(
    private val classList: List<ClassModel>,
    private val onClassClick: (ClassModel) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_class_name)
        val icon: ImageView = itemView.findViewById(R.id.iv_class_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classItem = classList[position]
        holder.name.text = classItem.name ?: "Unknown Class"
        
        if (classItem.iconUrl != null) {
            Glide.with(holder.itemView.context)
                .load(classItem.iconUrl)
                .placeholder(R.drawable.ic_learning)
                .into(holder.icon)
        }
        
        holder.itemView.setOnClickListener { onClassClick(classItem) }
    }

    override fun getItemCount(): Int = classList.size
}
