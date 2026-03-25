package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object BatterySms {

    @SuppressLint("MissingPermission")
    fun send(context: Context) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        // try to get location first then send SMS
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    val locationText = if (location != null) {
                        "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    } else {
                        "Location unavailable"
                    }
                    sendSms(context, locationText)
                }
                .addOnFailureListener {
                    // send without location if it fails
                    sendSms(context, "Location unavailable")
                }
        } catch (e: Exception) {
            sendSms(context, "Location unavailable")
        }
    }

    private fun sendSms(context: Context, locationText: String) {
        val contacts = ContactsManager.getContacts(context).take(5)
        val smsManager = android.telephony.SmsManager.getDefault()
        val message = "BATTERY LOW ALERT: My phone battery is at 10% or below and may turn off soon. " +
                "My last known location: $locationText. " +
                "I have been advised to enable power saving mode. Please check on me."

        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    contact.phone, null, parts, null, null
                )
                android.util.Log.d("BatterySms", "Battery SMS sent to ${contact.name}")
            } catch (e: Exception) {
                android.util.Log.e("BatterySms", "Failed to send to ${contact.name}: ${e.message}")
            }
        }
    }
}