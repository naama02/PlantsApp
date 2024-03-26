package com.plants.assistance.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.plants.assistance.R
import com.plants.assistance.adapter.PlantAdapter
import com.plants.assistance.model.Plant
import com.plants.assistance.externalapi.TrefleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

class PlantFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlantAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var errorMessageTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        errorMessageTextView = view.findViewById(R.id.errorMessageTextView)

        adapter = PlantAdapter()
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter

        fetchDataFromAPI()

        return view
    }

    private fun fetchDataFromAPI() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://trefle.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(TrefleService::class.java)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = service.searchPlants("rose", "LD6K2fESYDxVlBh-0laThjLEzJbQ1a06uGsj3G4wgDA")
                if (response.isSuccessful) {
                    val plantsResponse = response.body() as Map<String, Any>?
                    progressBar.visibility = View.GONE
                    if (plantsResponse != null) {
                        val plantListType: Type = object : TypeToken<List<Plant>>() {}.type
                        val gson = Gson()
                        val jsonData = gson.toJson(plantsResponse["data"])
                        val plantList: List<Plant> = gson.fromJson(jsonData, plantListType)
                        adapter.setPlants(plantList)
                    }


                } else {
                    progressBar.visibility = View.GONE
                    errorMessageTextView.visibility = View.VISIBLE
                    errorMessageTextView.text = "Failed to fetch data: ${response.message()}"
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                errorMessageTextView.visibility = View.VISIBLE
                errorMessageTextView.text = "Error: ${e.message}"
            }
        }
    }
}
