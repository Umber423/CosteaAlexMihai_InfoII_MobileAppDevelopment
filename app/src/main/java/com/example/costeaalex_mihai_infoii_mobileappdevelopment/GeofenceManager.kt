package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

data class GeofenceZone(
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double
)

object GeofenceManager {
    private const val PREFS_NAME = "geofence_prefs"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_RADIUS = "radius"
    private const val KEY_ENABLED = "enabled"

    private var locationCallback: LocationCallback? = null
    private var hasAlerted = false
    private val handler = Handler(Looper.getMainLooper())

    fun saveZone(context: Context, zone: GeofenceZone) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_LAT, zone.centerLat.toFloat())
            .putFloat(KEY_LON, zone.centerLon.toFloat())
            .putFloat(KEY_RADIUS, zone.radiusKm.toFloat())
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun getZone(context: Context): GeofenceZone? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return null
        return GeofenceZone(
            centerLat = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            centerLon = prefs.getFloat(KEY_LON, 0f).toDouble(),
            radiusKm = prefs.getFloat(KEY_RADIUS, 1f).toDouble()
        )
    }

    fun disableZone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, false)
            .apply()
        hasAlerted = false
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    @SuppressLint("MissingPermission")
    fun startMonitoring(context: Context, onExit: (Double, Double) -> Unit) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L // check every 60 seconds
        ).setMinUpdateDistanceMeters(50f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val zone = getZone(context) ?: return

                val distance = distanceKm(
                    location.latitude, location.longitude,
                    zone.centerLat, zone.centerLon
                )

                android.util.Log.d("GeofenceManager", "Distance from zone: ${"%.2f".format(distance)}km / ${zone.radiusKm}km")

                if (distance > zone.radiusKm && !hasAlerted) {
                    hasAlerted = true
                    handler.post { onExit(location.latitude, location.longitude) }
                }

                // reset alert if back inside zone
                if (distance <= zone.radiusKm) {
                    hasAlerted = false
                }
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    fun stopMonitoring(context: Context) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    // Haversine formula to calculate distance between two coordinates in km
    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}