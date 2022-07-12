package com.hrishi.weatherapp

import com.hrishi.weatherapp.model.WeatherResponse
import retrofit.http.GET
import retrofit.Call
import retrofit.http.Query

interface WeatherApi{
@GET("2.5/weather")
fun getData(
    @Query("lat")
    lat:Double,
    @Query("lon")
    lon:Double,
    @Query("appid")
    appid:String?,
    @Query("units")
    units:String?
):Call<WeatherResponse>
}