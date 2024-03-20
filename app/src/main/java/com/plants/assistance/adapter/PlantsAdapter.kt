package com.plants.assistance.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.plants.assistance.R
import com.plants.assistance.model.PlantProblem
import com.squareup.picasso.Picasso

class PlantsAdapter(
    private val fragmentActivity: FragmentActivity,
    var problems: List<PlantProblem>,
    private val isExpert: Boolean
) : RecyclerView.Adapter<ProblemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return ProblemViewHolder(view)
    }

    fun updateProblems(problems: List<PlantProblem>) {
        this.problems = problems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ProblemViewHolder, position: Int) {
        val problem = problems[position]

        // Load image using Picasso library
        Picasso.get().load(problem.imageUrl).into(holder.problemImage)

        // Set problem title
        holder.problemName.text = problem.title

        // Set click listener to navigate to problem detail fragment
        holder.itemView.setOnClickListener {
            // Pass problem data to ProblemDetailFragment using bundle
            val bundle = Bundle().apply {
                putString("problemId", problem.key)
                putString("userEmail", problem.userEmail)
                putString("imageUrl", problem.imageUrl)
                putString("description", problem.description)
                putString("dateStarted", problem.dateStarted)
                putString("ageOfPlant", problem.ageOfPlant)
                putString("suggestion", problem.suggestion)
                if (isExpert) {
                    putString("expertName", problem.expertName)
                }
            }

            // Navigate to ProblemDetailFragment
            val navController = fragmentActivity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController.navController.navigate(R.id.plantPageFragment, bundle)
        }
    }

    override fun getItemCount(): Int {
        return problems.size
    }
}
