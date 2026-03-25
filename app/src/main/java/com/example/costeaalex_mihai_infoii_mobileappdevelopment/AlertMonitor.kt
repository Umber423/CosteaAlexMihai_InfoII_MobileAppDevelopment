package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.LocationServices
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

data class NationalAlert(
    val id: String,
    val headline: String,
    val description: String,
    val severity: String,
    val event: String,
    val area: String
)

object AlertMonitor {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val sentAlertIds = mutableSetOf<String>()

    private val meteoEvents = listOf(
        "Tornado", "Flood", "Flash Flood", "Hurricane", "Tsunami",
        "Wildfire", "Fire Weather", "Extreme Wind", "Blizzard",
        "Ice Storm", "Winter Storm", "Thunderstorm", "Hail",
        "Drought", "Heat", "Cold", "Freeze", "Fog", "Avalanche",
        "Dust Storm", "Tropical Storm", "Typhoon", "Cyclone",
        "Storm", "Snow", "Wind", "Rain", "Weather"
    )

    @SuppressLint("MissingPermission")
    fun start(context: Context, onAlert: (NationalAlert) -> Unit) {
        isRunning = true
        check(context, onAlert)
        handler.postDelayed({ if (isRunning) start(context, onAlert) }, 5 * 60 * 1000L)

        // ← TEST ALERT — remove before release
        val testAlert = NationalAlert(
            id = "test-alert-001",
            headline = "Test Alert: Severe Thunderstorm Warning",
            description = "This is a test alert for development purposes.",
            severity = "Severe",
            event = "Thunderstorm",
            area = "Your current area"
        )
        handler.postDelayed({
            onAlert(testAlert)
        }, 2000L) // shows 2 seconds after app opens
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("MissingPermission")
    private fun check(context: Context, onAlert: (NationalAlert) -> Unit) {
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location ->
                if (location == null) return@addOnSuccessListener
                val url = "https://api.weather.gov/alerts/active?status=actual" +
                        "&message_type=alert&point=${location.latitude},${location.longitude}"
                fetchAlerts(url, context, onAlert)
            }
            .addOnFailureListener {
                android.util.Log.e("AlertMonitor", "Location failed: ${it.message}")
            }
    }

    private fun fetchAlerts(url: String, context: Context, onAlert: (NationalAlert) -> Unit) {
        client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("AlertMonitor", "Fetch failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val features = org.json.JSONObject(body).getJSONArray("features")
                    for (i in 0 until features.length()) {
                        val props = features.getJSONObject(i).getJSONObject("properties")
                        val id = props.getString("id")
                        val event = props.optString("event", "")
                        if (meteoEvents.none { event.contains(it, ignoreCase = true) }) continue
                        if (id in sentAlertIds) continue
                        sentAlertIds.add(id)
                        val alert = NationalAlert(
                            id = id,
                            headline = props.optString("headline", "Weather Alert"),
                            description = props.optString("description", ""),
                            severity = props.optString("severity", "Unknown"),
                            event = event,
                            area = props.optString("areaDesc", "Your area")
                        )
                        handler.post {
                            onAlert(alert)
                            notifyContacts(context, alert)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AlertMonitor", "Parse error: ${e.message}")
                }
            }
        })
    }

    private fun notifyContacts(context: Context, alert: NationalAlert) {
        val smsManager = android.telephony.SmsManager.getDefault()
        val message = "WEATHER ALERT: ${alert.event} - ${alert.headline}. Area: ${alert.area}. Stay safe."
        ContactsManager.getContacts(context).take(5).forEach { contact ->
            try {
                smsManager.sendMultipartTextMessage(contact.phone, null, smsManager.divideMessage(message), null, null)
            } catch (e: Exception) {
                android.util.Log.e("AlertMonitor", "SMS failed: ${e.message}")
            }
        }
    }
}