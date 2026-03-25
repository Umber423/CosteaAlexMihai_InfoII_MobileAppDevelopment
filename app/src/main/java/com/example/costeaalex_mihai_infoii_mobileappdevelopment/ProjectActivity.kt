package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.costeaalex_mihai_infoii_mobileappdevelopment.ui.theme.CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme

class ProjectActivity : ComponentActivity() {
    private lateinit var powerButtonReceiver: PowerButtonTracker
    private lateinit var motionTracker: InactivityTracker
    private lateinit var voiceAlert: VoiceAlert
    private lateinit var batteryMonitor: BatteryMonitor

    private var activeAlerts by mutableStateOf(listOf<NationalAlert>())
    private var currentScreen by mutableStateOf("main")
    private var countdownDialog: AlertDialog? = null
    private var countdownHandler = Handler(Looper.getMainLooper())
    private var isListening by mutableStateOf(false)

    // Receives the broadcast from the service when recording is saved
    private var recordingDoneReceiver: BroadcastReceiver? = null
    // Tracks whether the user cancelled during the recording phase
    private var emergencyCancelled = false

    private val requestAllPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            android.util.Log.d("Permissions", "Permissions check complete: $results")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestAllPermissions.launch(missingPermissions.toTypedArray())
        }

        powerButtonReceiver = PowerButtonTracker {
            runOnUiThread { triggerEmergency() }
        }

        motionTracker = InactivityTracker(
            context = this,
            stillThresholdMinutes = 1,
            onStillTooLong = {
                runOnUiThread { triggerEmergency() }
            }
        )

        voiceAlert = VoiceAlert(this) {
            runOnUiThread { triggerEmergency() }
        }

        batteryMonitor = BatteryMonitor(this) {
            runOnUiThread { showLowBatteryDialog() }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(powerButtonReceiver, filter)
        motionTracker.start()
        batteryMonitor.start()

        AlertMonitor.start(this) { alert ->
            activeAlerts = activeAlerts + alert
            showAlertDialog(alert)
        }

        if (GeofenceManager.isEnabled(this)) {
            GeofenceManager.startMonitoring(this) { lat, lon ->
                handleGeofenceExit(lat, lon)
            }
        }

        setContent {
            CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "main" -> AlphaScreen(
                            modifier = Modifier.padding(innerPadding),
                            onMicClick = {
                                if (isListening) {
                                    voiceAlert.stop()
                                    isListening = false
                                } else {
                                    voiceAlert.start()
                                    isListening = true
                                }
                            },
                            onEmergencyCall = { triggerEmergency() },
                            isListening = isListening,
                            onContactsClick = { currentScreen = "contacts" },
                            onAlertsClick = { currentScreen = "alerts" },
                            onScheduledMessageClick = { currentScreen = "scheduled" },
                            onGeofenceClick = { currentScreen = "geofence" }
                        )
                        "contacts" -> ContactsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBack = { currentScreen = "main" }
                        )
                        "alerts" -> AlertsScreen(
                            alerts = activeAlerts,
                            onBack = { currentScreen = "main" }
                        )
                        "scheduled" -> ScheduledMessageScreen(
                            onBack = { currentScreen = "main" }
                        )
                        "geofence" -> GeofenceScreen(
                            onBack = {
                                GeofenceManager.stopMonitoring(this)
                                if (GeofenceManager.isEnabled(this)) {
                                    GeofenceManager.startMonitoring(this) { lat, lon ->
                                        handleGeofenceExit(lat, lon)
                                    }
                                }
                                currentScreen = "main"
                            }
                        )
                    }
                }
            }
        }
    }

    // ─── Emergency flow ───────────────────────────────────────────────────────

    fun triggerEmergency() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            EmergencySms.sendToTopContacts(this)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            showRecordingThenCallDialog()
        }
    }

    private fun showRecordingThenCallDialog() {
        if (isFinishing || isDestroyed) return

        emergencyCancelled = false

        // Show dialog immediately so user sees something and can cancel
        val dialog = AlertDialog.Builder(this)
            .setTitle("Emergency")
            .setMessage("Recording evidence (8s)...\nCall will go out automatically.")
            .setPositiveButton("Cancel") { d, _ ->
                d.dismiss()
                emergencyCancelled = true
                unregisterRecordingReceiver()
                EmergencyRecordingService.stop(this)
                motionTracker.start()
            }
            .setCancelable(false)
            .create()

        countdownDialog = dialog
        dialog.show()

        // Register receiver to know when the 8s recording is saved
        registerRecordingDoneReceiver()

        // Start the 8-second recording
        EmergencyRecordingService.start(this)

        // Safety net: if broadcast never arrives (e.g. service crash),
        // place the call anyway after 12 seconds
        countdownHandler.removeCallbacksAndMessages(null)
        countdownHandler.postDelayed({
            if (!emergencyCancelled) {
                android.util.Log.w("ProjectActivity", "Safety net fired — placing call without broadcast")
                unregisterRecordingReceiver()
                placeEmergencyCall()
            }
        }, 12_000L)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerRecordingDoneReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == EmergencyRecordingService.ACTION_RECORDING_DONE) {
                    android.util.Log.d("ProjectActivity", "Recording done broadcast received")
                    unregisterRecordingReceiver()
                    countdownHandler.removeCallbacksAndMessages(null)
                    if (!emergencyCancelled) {
                        placeEmergencyCall()
                    }
                }
            }
        }
        recordingDoneReceiver = receiver

        val filter = IntentFilter(EmergencyRecordingService.ACTION_RECORDING_DONE)
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterRecordingReceiver() {
        recordingDoneReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            recordingDoneReceiver = null
        }
    }

    private fun placeEmergencyCall() {
        if (isFinishing || isDestroyed) return
        countdownDialog?.dismiss()
        countdownDialog = null
        android.util.Log.d("ProjectActivity", "Placing emergency call")
        EmergencyManager.call(this)
    }

    // ─── Other dialogs ────────────────────────────────────────────────────────

    private fun showLowBatteryDialog() {
        if (isFinishing || isDestroyed) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            BatterySms.send(this)
        }
        AlertDialog.Builder(this)
            .setTitle("Low battery")
            .setMessage("Battery is critical. Contacts notified.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAlertDialog(alert: NationalAlert) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("${alert.event}")
            .setMessage(alert.headline)
            .setPositiveButton("View") { dialog, _ ->
                dialog.dismiss()
                currentScreen = "alerts"
            }
            .setNegativeButton("Dismiss") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ─── Geofence ─────────────────────────────────────────────────────────────

    private fun handleGeofenceExit(lat: Double, lon: Double) {
        // play alarm sound
        try {
            val alarmUri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM
            )
            val ringtone = android.media.RingtoneManager.getRingtone(this, alarmUri)
            ringtone.play()
            Handler(Looper.getMainLooper()).postDelayed({
                ringtone.stop()
            }, 5000)
        } catch (e: Exception) {
            android.util.Log.e("GeofenceAlert", "Ringtone failed: ${e.message}")
        }

        val contacts = ContactsManager.getContacts(this).take(5)
        val smsManager = android.telephony.SmsManager.getDefault()
        val message = "GEOFENCE ALERT: I have left my designated safe zone. " +
                "Last known location: https://maps.google.com/?q=$lat,$lon " +
                "Track live: https://www.google.com/maps/search/?api=1&query=$lat,$lon"

        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
            } catch (e: Exception) {
                android.util.Log.e("ProjectActivity", "Geofence SMS failed: ${e.message}")
            }
        }

        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                AlertDialog.Builder(this)
                    .setTitle("⚠ Safe zone exited")
                    .setMessage("You have left your geofence zone.\nYour contacts have been notified.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerButtonReceiver)
        unregisterRecordingReceiver()
        motionTracker.stop()
        batteryMonitor.stop()
        voiceAlert.stop()
        AlertMonitor.stop()
        GeofenceManager.stopMonitoring(this)
        countdownHandler.removeCallbacksAndMessages(null)
        countdownDialog?.dismiss()
    }
}