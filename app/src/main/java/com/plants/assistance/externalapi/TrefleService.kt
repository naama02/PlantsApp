package com.plants.assistance.externalapi
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TrefleService {
    @GET("plants")
    suspend fun searchPlants(
        @Query("q") query: String,
        @Query("token") token: String
    ): Response<Object>
}