package com.plants.assistance.viewmodel;

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.plants.assistance.model.PlantProblem
import com.plants.assistance.database.PlantProblemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlantViewModel(private val plantProblemDao: PlantProblemDao) : ViewModel() {
    val allProblems: LiveData<List<PlantProblem>> = plantProblemDao.getAll()

    fun fetchProblemsFromFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            db.collection("Problems").get().addOnCompleteListener { task ->
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
                            expertName = document.getString("expertName") ?: ""
                        )
                        problems.add(problem)
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

    fun fetchMyProblemsForUserEmail(userEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            db.collection("Problems").whereEqualTo("userEmail", userEmail).get().addOnCompleteListener { task ->
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
                            expertName = document.getString("expertName") ?: ""
                        )
                        problems.add(problem)
                    }

                    // Insert problems into database within IO scope
                    CoroutineScope(Dispatchers.IO).launch {
                        plantProblemDao.deleteAll()
                        plantProblemDao.insertAll(problems)
                    }.invokeOnCompletion {
                        // Do something after insertion completes if needed
                    }
                } else {
                    Log.e("PlantViewModel", "Error fetching problems for user email: $userEmail")
                }
            }
        }
    }
}
