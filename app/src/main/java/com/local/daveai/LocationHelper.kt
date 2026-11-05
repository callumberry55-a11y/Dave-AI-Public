package com.local.daveai

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationWeatherContext(
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val weatherSummary: String? = null
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun fetchLocationAndWeather(): LocationWeatherContext = withContext(Dispatchers.IO) {
        try {
            val location = suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    continuation.resume(loc)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } ?: return@withContext LocationWeatherContext()

            val lat = location.latitude
            val lon = location.longitude

            var locationName: String? = null
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            locationName = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        locationName = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                    }
                }
            } catch (_: Exception) {}

            try {
                if (locationName == null) {
                    val nomResp = FreeEnvironmentClient.nominatim.reverseGeocode(lat, lon)
                    val addr = nomResp.address
                    if (addr != null) {
                        locationName = listOfNotNull(addr.suburb ?: addr.neighbourhood, addr.city ?: addr.town ?: addr.county, addr.country).joinToString(", ")
                    }
                }
            } catch (_: Exception) {}

            var weatherDetails: String? = null
            try {
                val weatherResp = FreeEnvironmentClient.openMeteoWeather.getCurrentWeather(lat, lon)
                val cw = weatherResp.currentWeather
                if (cw != null) {
                    val aqiResp = try { FreeEnvironmentClient.openMeteoAirQuality.getAirQuality(lat, lon) } catch (_: Exception) { null }
                    val aqiText = aqiResp?.current?.usAqi?.let { " | AQI: $it" } ?: ""
                    weatherDetails = "${cw.temperature}°C, Wind ${cw.windspeed} km/h$aqiText"
                }
            } catch (_: Exception) {}

            val summary = buildString {
                append("Location: ${locationName ?: "Lat: $lat, Lon: $lon"}")
                if (weatherDetails != null) {
                    append(" ($weatherDetails)")
                }
            }

            LocationWeatherContext(
                locationName = locationName ?: "Lat: $lat, Lon: $lon",
                latitude = lat,
                longitude = lon,
                weatherSummary = summary
            )
        } catch (_: Exception) {
            LocationWeatherContext()
        }
    }
}
