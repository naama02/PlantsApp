package com.plants.assistance.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plants.assistance.model.PlantProblem

@Dao
interface PlantProblemDao {
    @Query("SELECT * FROM PlantProblem")
    fun getAll(): LiveData<List<PlantProblem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(plants: List<PlantProblem>)



    @Query("DELETE FROM PlantProblem")
    fun deleteAll()

    @Delete
    fun delete(plant: PlantProblem)

    @Update
    fun update(plant: PlantProblem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(plant: PlantProblem)
}
