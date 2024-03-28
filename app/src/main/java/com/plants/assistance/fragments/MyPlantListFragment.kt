package com.plants.assistance.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.plants.assistance.R
import com.plants.assistance.adapter.PlantsAdapter
import com.plants.assistance.db.MyDatabse
import com.plants.assistance.model.PlantViewModel
import com.plants.assistance.model.PlantViewModelFactory

class MyPlantListFragment : Fragment() {
    private lateinit var viewModel: PlantViewModel
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_plant_list, container, false)
        initializeUI(rootView)
        setupViewModel()
        return rootView
    }

    private fun initializeUI(rootView: View) {
        recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView).apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1)
            adapter = PlantsAdapter(requireActivity(), mutableListOf(), true)
        }
        rootView.findViewById<Button>(R.id.add_button).setOnClickListener {
            val navController = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController.navController.navigate(R.id.addPlantFragment)
        }

        rootView.findViewById<TextView>(R.id.titleTextView).text = getString(R.string.plants)

    }

    private fun setupViewModel() {
        val db = MyDatabse.getInstance(requireContext().applicationContext)
        val factory = PlantViewModelFactory(db.plantProblemDao())
        viewModel = ViewModelProvider(this, factory).get(PlantViewModel::class.java)

        viewModel.allProblems.observe(viewLifecycleOwner, Observer { problems ->
            (recyclerView.adapter as PlantsAdapter).problems = problems
            recyclerView.adapter?.notifyDataSetChanged()
        })

        if (isOnline()) {
            FirebaseAuth.getInstance().currentUser?.email?.let {
                viewModel.fetchProblemsFromFirestore(
                    userEmail =  it
                )
            }
        } else {
            Toast.makeText(context, "Offline: Displaying cached plants", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
