package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun GeofenceScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var radiusKm by remember { mutableFloatStateOf(1f) }
    var isEnabled by remember { mutableStateOf(GeofenceManager.isEnabled(context)) }
    var statusMessage by remember { mutableStateOf("") }
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var isLocating by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    // permission launcher — only used once from the button or on first load
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasPermission) statusMessage = "Location permission denied"
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                currentLat = location.latitude
                currentLon = location.longitude
                statusMessage = "✓ Location acquired (±${"%.0f".format(location.accuracy)}m)"
                if (location.accuracy <= 50f) {
                    isLocating = false
                    fusedClient.removeLocationUpdates(this)
                }
            }
        }
    }

    // load saved radius once
    LaunchedEffect(Unit) {
        GeofenceManager.getZone(context)?.let { radiusKm = it.radiusKm.toFloat() }

        // request permission only once on first open if not already granted
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // start locating only when permission is granted
    DisposableEffect(hasPermission) {
        if (hasPermission) {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                    as LocationManager
            val isLocationEnabled =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isLocationEnabled) {
                statusMessage = "⚠ Location services disabled. Please enable in Settings."
                isLocating = false
            } else {
                isLocating = true
                statusMessage = "Acquiring location..."
                try {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                currentLat = location.latitude
                                currentLon = location.longitude
                                statusMessage = "✓ Got location (±${"%.0f".format(location.accuracy)}m)"
                                isLocating = false
                            } else {
                                statusMessage = "Trying updates..."
                                startLocationUpdates(fusedClient, locationCallback)
                            }
                        }
                        .addOnFailureListener {
                            statusMessage = "Trying updates..."
                            startLocationUpdates(fusedClient, locationCallback)
                        }
                } catch (e: SecurityException) {
                    statusMessage = "Permission error: ${e.message}"
                    isLocating = false
                }
            }
        }

        onDispose {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        FilledTonalButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("← Back")
        }

        Text(
            "Geofence Zone",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            "Set a safe area around your current location. If your phone leaves this area, your top 5 contacts will be notified.",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLocating) CircularProgressIndicator()
                Column {
                    Text("Current Location", fontWeight = FontWeight.Bold)
                    if (currentLat != 0.0 && currentLon != 0.0) {
                        Text("Lat: ${"%.5f".format(currentLat)}", fontSize = 13.sp, color = Color.Gray)
                        Text("Lon: ${"%.5f".format(currentLon)}", fontSize = 13.sp, color = Color.Gray)
                    }
                    if (statusMessage.isNotBlank()) {
                        Text(statusMessage, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        // retry + grant permission buttons only when needed
        if (!isEnabled) {
            if (!hasPermission) {
                FilledTonalButton(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE65100),
                        contentColor = Color.White
                    )
                ) {
                    Text("Grant Location Permission")
                }
            } else {
                FilledTonalButton(
                    onClick = {
                        isLocating = true
                        currentLat = 0.0
                        currentLon = 0.0
                        statusMessage = "Retrying..."
                        try {
                            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        currentLat = location.latitude
                                        currentLon = location.longitude
                                        statusMessage = "✓ Got location (±${"%.0f".format(location.accuracy)}m)"
                                        isLocating = false
                                    } else {
                                        statusMessage = "Still unavailable — ensure GPS is on"
                                        isLocating = false
                                    }
                                }
                                .addOnFailureListener {
                                    statusMessage = "Failed: ${it.message}"
                                    isLocating = false
                                }
                        } catch (e: SecurityException) {
                            statusMessage = "Permission error"
                            isLocating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    enabled = !isLocating
                ) {
                    Text("🔄 Retry Location")
                }
            }
        }

        Text(
            "Safe zone radius: ${if (radiusKm < 1f) "${"%.0f".format(radiusKm * 1000)}m" else "${"%.1f".format(radiusKm)}km"}",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Slider(
            value = radiusKm,
            onValueChange = { radiusKm = it },
            valueRange = 0.001f..50f,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isEnabled
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isEnabled) {
                FilledTonalButton(
                    onClick = {
                        if (currentLat != 0.0 && currentLon != 0.0) {
                            GeofenceManager.saveZone(
                                context,
                                GeofenceZone(
                                    centerLat = currentLat,
                                    centerLon = currentLon,
                                    radiusKm = radiusKm.toDouble()
                                )
                            )
                            isEnabled = true
                            fusedClient.removeLocationUpdates(locationCallback)
                            isLocating = false
                            statusMessage = "Geofence active — ${if (radiusKm < 1f) "${"%.0f".format(radiusKm * 1000)}m" else "${"%.1f".format(radiusKm)}km"} zone set"
                        } else {
                            statusMessage = "Still waiting for location..."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission && currentLat != 0.0,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isLocating) "Getting location..." else "Activate Zone")
                }
            } else {
                FilledTonalButton(
                    onClick = {
                        GeofenceManager.disableZone(context)
                        isEnabled = false
                        statusMessage = "Geofence disabled"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color.White
                    )
                ) {
                    Text("Disable Zone")
                }
            }
        }

        if (isEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🟢", fontSize = 16.sp)
                    Column {
                        Text("Geofence Active", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Monitoring ${if (radiusKm < 1f) "${"%.0f".format(radiusKm * 1000)}m" else "${"%.1f".format(radiusKm)}km"} zone around saved location",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
    callback: LocationCallback
) {
    try {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        )
            .setMinUpdateDistanceMeters(0f)
            .setMinUpdateIntervalMillis(500L)
            .build()
        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            android.os.Looper.getMainLooper()
        )
    } catch (e: Exception) {
        android.util.Log.e("GeofenceScreen", "Location update error: ${e.message}")
    }
}