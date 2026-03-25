package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.graphics.SurfaceTexture
import android.view.Surface
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmergencyRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var pendingUri: Uri? = null
    private var pendingPfd: ParcelFileDescriptor? = null
    private var isAudioOnly = false
    private val serviceHandler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "START_RECORDING"
        const val ACTION_STOP = "STOP_RECORDING"
        const val ACTION_RECORDING_DONE = "com.example.costeaalex_mihai_infoii_mobileappdevelopment.RECORDING_DONE"
        const val CHANNEL_ID = "emergency_recording_channel"
        const val NOTIFICATION_ID = 999
        private const val TAG = "EmergencyRecSvc"
        private const val RECORDING_DURATION_MS = 8 * 1000L // 8 seconds, then call goes out

        fun start(context: Context) {
            android.util.Log.d(TAG, "start() called")
            val intent = Intent(context, EmergencyRecordingService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to start service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            android.util.Log.d(TAG, "stop() called")
            val intent = Intent(context, EmergencyRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d(TAG, "onCreate()")
        createNotificationChannel()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EmergencyApp::RecordingWakeLock"
        )
        wakeLock?.acquire(RECORDING_DURATION_MS + 15_000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasAudio = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCamera = checkSelfPermission(android.Manifest.permission.CAMERA) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED

                var foregroundType = 0
                if (hasAudio) foregroundType = foregroundType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (hasCamera) foregroundType = foregroundType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA

                if (foregroundType != 0) {
                    startForeground(NOTIFICATION_ID, buildNotification(isFinished = false), foregroundType)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification(isFinished = false))
                }
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(isFinished = false))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "startForeground() failed: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startRecording()
                serviceHandler.removeCallbacksAndMessages(null)
                // After 8 seconds, save and broadcast so the Activity can place the call
                serviceHandler.postDelayed({ stopAndSave() }, RECORDING_DURATION_MS)
            }
            ACTION_STOP -> {
                serviceHandler.removeCallbacksAndMessages(null)
                stopAndSave()
            }
        }
        return START_STICKY
    }

    // ─── Recording ────────────────────────────────────────────────────────────

    private fun startRecording() {
        isAudioOnly = false
        android.util.Log.d(TAG, "startRecording() — attempting video")

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        try {
            mediaRecorder = createMediaRecorder()

            // Dummy surface to satisfy hardware encoder — no visible preview needed
            val dummyTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(1280, 720) }
            val dummySurface = Surface(dummyTexture)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "emergency_$timestamp.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/EmergencyRecordings")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)
                    ?: throw Exception("MediaStore insert returned null")
                pendingUri = uri
                val pfd = contentResolver.openFileDescriptor(uri, "w")
                    ?: throw Exception("openFileDescriptor returned null")
                pendingPfd = pfd

                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setVideoSize(1280, 720)
                    setVideoFrameRate(30)
                    setVideoEncodingBitRate(2_000_000)
                    setPreviewDisplay(dummySurface)  // ← fixes hardware encoder
                    setOutputFile(pfd.fileDescriptor)
                    prepare()
                    start()
                }
                android.util.Log.d(TAG, "Video recording started → Movies/EmergencyRecordings/emergency_$timestamp.mp4")

            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "EmergencyRecordings"
                ).also { it.mkdirs() }
                currentFile = File(dir, "emergency_$timestamp.mp4")

                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setVideoSize(1280, 720)
                    setVideoFrameRate(30)
                    setVideoEncodingBitRate(2_000_000)
                    setPreviewDisplay(dummySurface)  // ← fixes hardware encoder
                    setOutputFile(currentFile!!.absolutePath)
                    prepare()
                    start()
                }
                android.util.Log.d(TAG, "Video recording started → ${currentFile!!.absolutePath}")
            }

            // Release the dummy surface after recording starts — encoder holds its own ref
            dummySurface.release()
            dummyTexture.release()

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Video failed (${e.message}), falling back to audio-only")
            releaseRecorder()
            pendingPfd?.close(); pendingPfd = null
            pendingUri?.let { contentResolver.delete(it, null, null) }; pendingUri = null
            currentFile = null
            startAudioOnly(timestamp)
        }
    }

    private fun startAudioOnly(timestamp: String) {
        isAudioOnly = true
        android.util.Log.d(TAG, "startAudioOnly()")

        try {
            mediaRecorder = createMediaRecorder()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "emergency_audio_$timestamp.mp4")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/EmergencyRecordings")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv)
                    ?: throw Exception("MediaStore insert returned null")
                pendingUri = uri
                val pfd = contentResolver.openFileDescriptor(uri, "w")
                    ?: throw Exception("openFileDescriptor returned null")
                pendingPfd = pfd

                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128_000)
                    setOutputFile(pfd.fileDescriptor)
                    prepare()
                    start()
                }
                android.util.Log.d(TAG, "Audio recording started → Music/EmergencyRecordings/emergency_audio_$timestamp.mp4")

            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "EmergencyRecordings"
                ).also { it.mkdirs() }
                currentFile = File(dir, "emergency_audio_$timestamp.mp4")

                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128_000)
                    setOutputFile(currentFile!!.absolutePath)
                    prepare()
                    start()
                }
                android.util.Log.d(TAG, "Audio recording started → ${currentFile!!.absolutePath}")
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Audio-only also failed: ${e.message}")
            releaseRecorder()
            pendingPfd?.close(); pendingPfd = null
            pendingUri?.let { contentResolver.delete(it, null, null) }; pendingUri = null
            currentFile = null
            // Broadcast done even on failure so the call still goes out
            broadcastDone()
            stopSelf()
        }
    }

    // ─── Saving ───────────────────────────────────────────────────────────────

    private fun stopAndSave() {
        android.util.Log.d(TAG, "stopAndSave()")
        try {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "stop() error: ${e.message}")
            }
            releaseRecorder()

            pendingPfd?.close()
            pendingPfd = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pendingUri?.let { uri ->
                    val cv = ContentValues().apply {
                        if (isAudioOnly) {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                        } else {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                    }
                    val rows = contentResolver.update(uri, cv, null, null)
                    android.util.Log.d(TAG, "IS_PENDING cleared, rows updated: $rows — file visible in gallery")
                    pendingUri = null
                }
            } else {
                currentFile?.let { file ->
                    MediaScannerConnection.scanFile(
                        this, arrayOf(file.absolutePath), null
                    ) { path, uri ->
                        android.util.Log.d(TAG, "MediaScanner completed: $path → $uri")
                    }
                }
            }

            updateNotificationFinished()
            notifyContactsSaved()

            // Broadcast to Activity so it can now place the call
            broadcastDone()

            serviceHandler.postDelayed({ stopSelf() }, 3000)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "stopAndSave critical failure: ${e.message}")
            releaseRecorder()
            pendingPfd?.close(); pendingPfd = null
            // Still broadcast so the call isn't stuck waiting forever
            broadcastDone()
            stopSelf()
        } finally {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
    }

    private fun broadcastDone() {
        android.util.Log.d(TAG, "Broadcasting RECORDING_DONE")
        val intent = Intent(ACTION_RECORDING_DONE)
        intent.setPackage(packageName) // explicit so only our app receives it
        sendBroadcast(intent)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun releaseRecorder() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
    }

    private fun updateNotificationFinished() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(isFinished = true))
    }

    private fun notifyContactsSaved() {
        val contacts = ContactsManager.getContacts(this).take(5)
        val smsManager = android.telephony.SmsManager.getDefault()
        val type = if (isAudioOnly) "audio" else "video"
        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isAudioOnly) "Music/EmergencyRecordings" else "Movies/EmergencyRecordings"
        } else {
            currentFile?.absolutePath ?: "device storage"
        }
        val message = "EMERGENCY: A $type recording has been saved to $location on the device."
        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "SMS failed to ${contact.name}: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Emergency Recording", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Active emergency recording in progress" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isFinished: Boolean): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val title = if (isFinished) "Recording saved" else "Recording evidence..."
        val text = if (isFinished) "Saved to gallery — placing emergency call" else "Capturing 8 seconds before call"
        return builder
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(
                if (isFinished) android.R.drawable.stat_sys_download_done
                else android.R.drawable.ic_btn_speak_now
            )
            .setOngoing(!isFinished)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceHandler.removeCallbacksAndMessages(null)
        releaseRecorder()
        pendingPfd?.close()
        pendingPfd = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }
}