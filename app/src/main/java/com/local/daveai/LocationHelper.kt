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
            } catch (ignored: Exception) {
            }

            LocationWeatherContext(
                locationName = locationName ?: "Lat: $lat, Lon: $lon",
                latitude = lat,
                longitude = lon,
                weatherSummary = "Current user location: ${locationName ?: "Lat: $lat, Lon: $lon"} (Coordinates: $lat, $lon)"
            )
        } catch (ignored: Exception) {
            LocationWeatherContext()
        }
    }
}
