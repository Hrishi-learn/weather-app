package com.hrishi.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.create
import com.google.gson.Gson
import com.hrishi.weatherapp.databinding.ActivityMainBinding
import com.hrishi.weatherapp.model.WeatherResponse
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.Callback
import retrofit.GsonConverterFactory
import retrofit.Response
import retrofit.Retrofit
import java.net.URI.create
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentLocation:Location?=null
    private var dialog:Dialog?=null
    private var binding:ActivityMainBinding?=null
    //shared preferences
    private var mSharedPreferences:SharedPreferences?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        //sharedPreferences Handler
            mSharedPreferences=getSharedPreferences(Constants.WEATHER_DATA,Context.MODE_PRIVATE)
            showUi()
        //Location API
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        if(!isLocationEnabled()){
            val intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if(report.areAllPermissionsGranted()){
                        requestLocationData()
                    }
                    if(report.isAnyPermissionPermanentlyDenied){

                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken?
                ) {
                    showOnRationaleDialog()
                }
            }).check()

        refresh()
    }
    private fun refresh() {
        binding?.refreshLayout?.setOnRefreshListener {
            requestLocationData()
            binding?.refreshLayout?.isRefreshing=false
        }
    }
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest=com.google.android.gms.location.LocationRequest()
        locationRequest.priority=Priority.PRIORITY_HIGH_ACCURACY

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            currentLocation = locationResult.lastLocation
            val lat=currentLocation?.latitude
            val lon=currentLocation?.longitude
            if (lat != null && lon!=null) {
                getWeatherData(lat,lon)
            }
        }
    }
    private fun getWeatherData(lat:Double,lon:Double){
        if(Constants.isNetworkAvailable(this)){
            val retrofit=Retrofit.Builder().
            baseUrl(Constants.BASE_URL).
            addConverterFactory(GsonConverterFactory.create()).
            build()
            showProgressDialog()
            val service=retrofit.create(WeatherApi::class.java)
            service.getData(lat,lon,Constants.api_key,Constants.METRIC)
                .enqueue(object : Callback<WeatherResponse?> {
                    override fun onResponse(response: Response<WeatherResponse?>?, retrofit: Retrofit?){
                        if(response?.isSuccess == true){
                            val data: WeatherResponse? =response.body()
                            dialog?.dismiss()
                            if (data != null) {
                                val pref=mSharedPreferences?.edit()
                                pref?.putString(Constants.WEATHER_DATA_JSON, Gson().toJson(data))
                                pref?.apply()
                                showUi()
                            }
                        }else{
                            dialog?.dismiss()
                            Log.e("errorCode","${response?.code()}")
                        }
                    }
                    override fun onFailure(t: Throwable?) {
                        Log.e("error","$t")
                        dialog?.dismiss()
                    }
                })
        }
        else{
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show()
        }
    }
    fun showUi(){
        val dataJson=mSharedPreferences?.getString(Constants.WEATHER_DATA_JSON,"")
        val data=Gson().fromJson(dataJson,WeatherResponse::class.java)
        if(data!=null){
            binding?.tvMain?.text=data.weather[0].main
            binding?.tvMainDescription?.text=data.weather[0].description
            binding?.tvTemp?.text=data.main.temp.toString()+"°C"
            binding?.tvHumidity?.text=data.main.humidity.toString()+"%"
            binding?.tvMax?.text=data.main.temp_max.toString()+"°C"
            binding?.tvMin?.text=data.main.temp_min.toString()+"°C"
            binding?.tvSpeed?.text=data.wind.speed.toString()
            binding?.tvCountry?.text=data.sys.country
            binding?.tvName?.text=data.name
            binding?.tvSunriseTime?.text=rightTime(data.sys.sunrise)+"AM"
            binding?.tvSunsetTime?.text=rightTime(data.sys.sunset)+"PM"

            when (data.weather[0].icon) {
                "01d" -> binding?.ivMain?.setImageResource(R.drawable.sunny)
                "02d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "03d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "04d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "04n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)
                "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                "01n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "02n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "03n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "10n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "11n" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                "13n" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
            }
        }
    }
    private fun rightTime(time:Long):String?{
        val date= Date(time*1000L)
        val sdf = SimpleDateFormat("hh:mm")
        return sdf.format(date)
    }
    private fun showOnRationaleDialog(){
        val dialog=AlertDialog.Builder(this)
        dialog.setCancelable(false)
        dialog.setMessage("In order to use the app please grant location permission")
        dialog.setPositiveButton("Go to settings"){dialog,which->
            try {
                val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri= Uri.fromParts("package",packageName,null)
                intent.data=uri
                startActivity(intent)
            }
            catch (e:Exception){
                e.printStackTrace()
            }
        }.show()
    }
    private fun isLocationEnabled():Boolean{
        val mLocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||
                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun showProgressDialog(){
        dialog= Dialog(this)
        dialog?.setContentView(R.layout.progress_bar_dialog)
        dialog?.show()
    }
}