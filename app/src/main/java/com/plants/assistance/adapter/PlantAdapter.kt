package com.plants.assistance.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.plants.assistance.R
import com.plants.assistance.model.Plant
import com.squareup.picasso.Picasso

class PlantAdapter : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private var plants: List<Plant> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.bind(plant)
    }

    override fun getItemCount(): Int {
        return plants.size
    }

    fun setPlants(plants: List<Plant>) {
        this.plants = plants
        notifyDataSetChanged()
    }

    inner class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val commonNameTextView: TextView = itemView.findViewById(R.id.commonNameTextView)
        private val scientificNameTextView: TextView = itemView.findViewById(R.id.scientificNameTextView)
        private val familyNameTextView: TextView = itemView.findViewById(R.id.familyNameTextView)
        private val plantImageView: ImageView = itemView.findViewById(R.id.plantImageView)

        fun bind(plant: Plant) {
            commonNameTextView.text = plant.common_name
            scientificNameTextView.text = plant.scientific_name
            familyNameTextView.text = plant.bibliography
            Picasso.get().load(plant.image_url).into(plantImageView)
        }
    }
}
