package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryMonitor(
    private val context: Context,
    private val onLowBattery: () -> Unit
) : BroadcastReceiver() {

    private var hasAlerted = false

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        context.registerReceiver(this, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            android.util.Log.e("BatteryMonitor", "Receiver not registered: ${e.message}")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BATTERY_LOW -> {
                // fires when critically low, send final location immediately
                android.util.Log.d("BatteryMonitor", "Battery critically low — sending final location")
                BatterySms.send(context ?: return)
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (scale == -1) return
                val batteryPct = (level / scale.toFloat() * 100).toInt()
                android.util.Log.d("BatteryMonitor", "Battery: $batteryPct%")
                if (batteryPct <= 10 && !hasAlerted) {
                    hasAlerted = true
                    onLowBattery()
                }
                if (batteryPct > 20) hasAlerted = false
            }
        }
    }
}