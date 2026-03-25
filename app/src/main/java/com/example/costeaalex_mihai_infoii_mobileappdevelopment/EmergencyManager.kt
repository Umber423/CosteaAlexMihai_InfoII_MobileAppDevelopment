package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.content.Context
import android.content.Intent
import android.net.Uri

object EmergencyManager {
    private const val PREFS_NAME = "emergency_prefs"
    private const val KEY_CONTACT = "emergency_contact"
    private const val DEFAULT_NUMBER = "0740081270"

    fun getEmergencyNumber(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CONTACT, DEFAULT_NUMBER) ?: DEFAULT_NUMBER
    }

    fun setEmergencyNumber(context: Context, number: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONTACT, number)
            .apply()
    }

    fun call(context: Context) {
        val number = getEmergencyNumber(context)
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}