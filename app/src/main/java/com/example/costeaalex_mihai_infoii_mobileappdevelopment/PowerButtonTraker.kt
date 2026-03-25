package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerButtonTracker(private val onTriplePress: () -> Unit) : BroadcastReceiver() {
    private var pressCount = 0
    private var lastPressTime = 0L
    private val timeWindow = 3000L

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_SCREEN_ON) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPressTime > timeWindow) pressCount = 0
            pressCount++
            lastPressTime = currentTime
            if (pressCount >= 3) {
                onTriplePress()
                pressCount = 0
            }
        }
    }
}