package com.hrishi.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val BASE_URL:String="https://api.openweathermap.org/data/"
    const val api_key:String="7659f02087a98ef44a3a985b6e92922f"
    const val METRIC ="metric"
    const val WEATHER_DATA="weather_data"
    const val WEATHER_DATA_JSON="weather_data_json"

    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val network=connectivityManager.activeNetwork?:return false
            val activeNetwork=connectivityManager.getNetworkCapabilities(network)?:return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)-> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->true
                else->false
            }
        }else{
            val networkInfo=connectivityManager.activeNetworkInfo
            return networkInfo!=null && networkInfo.isConnectedOrConnecting
        }
    }
}