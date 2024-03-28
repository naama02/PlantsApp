package com.plants.assistance.model;

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.plants.assistance.model.PlantProblem
import com.plants.assistance.db.PlantProblemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlantViewModel(private val plantProblemDao: PlantProblemDao) : ViewModel() {
    val allProblems: LiveData<List<PlantProblem>> = plantProblemDao.getAll()
    private lateinit var mAuth: FirebaseAuth

    fun fetchProblemsFromFirestore(userEmail: String? = null, suggestion: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            var quer = db.collection("Plants")
            var query : Query = db.collection("Plants");
            userEmail?.let {
                query = quer.whereEqualTo("userEmail", it)
            }

            query.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val problems = mutableListOf<PlantProblem>() // List of problems to be fetched

                    for (document in task.result!!) {
                        val imageName = document.getString("imageUrl")

                        val problem = PlantProblem(
                            key = document.id,
                            userEmail = document.getString("userEmail") ?: "",
                            imageUrl = imageName ?: "",
                            description = document.getString("description") ?: "",
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0,
                            title = document.getString("title") ?: "",
                            dateStarted = document.getString("dateStarted") ?: "",
                            ageOfPlant = document.getString("ageOfPlant") ?: "",
                            suggestion = document.getString("suggestion") ?: "",
                            address = document.getString("address") ?: ""
                        )
                        if(suggestion == null){
                            problems.add(problem)
                        }
                        else if( problem.suggestion.contains(suggestion)){
                            problems.add(problem)
                        }

                    }

                    // Insert problems into database within IO scope
                    CoroutineScope(Dispatchers.IO).launch {
                        plantProblemDao.deleteAll()
                        plantProblemDao.insertAll(problems)
                    }.invokeOnCompletion {
                        // Do something after insertion completes if needed
                    }
                } else {
                    Log.e("PlantViewModel", "Error fetching problems")
                }
            }
        }
    }
}
