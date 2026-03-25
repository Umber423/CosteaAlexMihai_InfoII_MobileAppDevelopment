package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SmsManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object EmergencySms {

    @SuppressLint("MissingPermission")
    fun sendToTopContacts(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // try to get last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val locationText = if (location != null) {
                "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location unavailable"
            }
            sendSms(context, locationText)
        }.addOnFailureListener {
            // if last location fails, send without location
            sendSms(context, "Location unavailable")
        }
    }

    private fun sendSms(context: Context, locationText: String) {
        val contacts = ContactsManager.getContacts(context).take(5)
        val smsManager = SmsManager.getDefault()
        val message = "EMERGENCY ALERT: I need immediate help! My location: $locationText"

        contacts.forEach { contact ->
            try {
                // use sendMultipartTextMessage in case message is long
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    contact.phone,
                    null,
                    parts,
                    null,
                    null
                )
                android.util.Log.d("EmergencySms", "SMS sent to ${contact.name}")
            } catch (e: Exception) {
                android.util.Log.e("EmergencySms", "Failed to send to ${contact.name}: ${e.message}")
            }
        }
    }
}