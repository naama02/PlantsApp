package com.plants.assistance.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
        @SerializedName("current") val current: Current
) {
    data class Current(
            @SerializedName("time") val time: String,
            @SerializedName("temperature_2m") val temperature_2m: Double,
            @SerializedName("wind_speed_10m") val wind_speed_10m: Double
    )
}
