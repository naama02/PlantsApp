package com.plants.assistance.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.plants.assistance.R

class ProblemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val cardView: CardView = itemView.findViewById(R.id.cardView)
    val problemImage: ImageView = itemView.findViewById(R.id.plantImage)
    val problemName: TextView = itemView.findViewById(R.id.name)
}
