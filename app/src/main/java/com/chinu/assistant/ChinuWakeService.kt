package com.chinu.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class ChinuWakeService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundNotification()
        setupSpeechRecognizer()
        startListeningLoop()
    }

    private fun startForegroundNotification() {
        val channelId = "chinu_wake_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CHINU Background Control",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("CHINU Active")
            .setContentText("Background voice control ON...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                restartListening(800L)
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = list?.firstOrNull()?.lowercase(Locale.getDefault())?.trim() ?: ""
                if (spoken.isNotEmpty()) {
                    handleBackgroundCommand(spoken)
                } else {
                    restartListening(600L)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleBackgroundCommand(cmd: String) {
        val access = ChinuAccessibilityService.instance

        when {
            // 1. Scroll Down
            cmd.contains("scroll down") || cmd.contains("niche scroll") || cmd.contains("niche kar") || cmd == "niche" -> {
                access?.scrollDown()
                restartListening(700L)
            }
            // 2. Scroll Up
            cmd.contains("scroll up") || cmd.contains("upar scroll") || cmd.contains("upar kar") || cmd == "upar" -> {
                access?.scrollUp()
                restartListening(700L)
            }
            // 3. Go Back
            cmd == "back" || cmd.contains("go back") || cmd.contains("pachhal") -> {
                access?.performBack()
                restartListening(700L)
            }
            // 4. Go Home
            cmd == "home" || cmd.contains("go home") || cmd.contains("home screen") -> {
                access?.performHome()
                restartListening(700L)
            }
            // 5. Open any app directly from background
            cmd.contains("open ") || cmd.contains(" khol") || cmd.contains(" chalu") -> {
                val appQuery = cmd.replace("open", "")
                    .replace("kholo", "")
                    .replace("khol", "")
                    .replace("chalu", "")
                    .replace("karo", "")
                    .replace("kar", "")
                    .replace("app", "")
                    .trim()
                if (appQuery.isNotEmpty()) {
                    openAppFromBackground(appQuery)
                }
                restartListening(1500L)
            }
            // 6. Wake Word -> Open CHINU App
            cmd.contains("chinu") || cmd.contains("cheenu") || cmd.contains("sinu") -> {
                openChinuApp()
                restartListening(3000L)
            }
            else -> {
                restartListening(600L)
            }
        }
    }

    private fun openAppFromBackground(query: String) {
        try {
            val directIntent: Intent? = when {
                query.contains("camera") -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                query.contains("settings") || query.contains("setting") -> Intent(Settings.ACTION_SETTINGS)
                query.contains("dialer") || query == "phone" || query == "call" -> Intent(Intent.ACTION_DIAL)
                query.contains("whatsapp") -> packageManager.getLaunchIntentForPackage("com.whatsapp")
                query.contains("youtube") -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                query.contains("instagram") || query.contains("insta") -> packageManager.getLaunchIntentForPackage("com.instagram.android")
                query.contains("chrome") -> packageManager.getLaunchIntentForPackage("com.android.chrome")
                else -> null
            }
            if (directIntent != null) {
                directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(directIntent)
                return
            }

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = packageManager.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in apps) {
                val label = resolveInfo.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
                val pkg = resolveInfo.activityInfo.packageName
                if (label.contains(query) || query.contains(label)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        return
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun startListeningLoop() {
        if (!isRunning) return
        handler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {
                restartListening(1200L)
            }
        }
    }

    private fun restartListening(delayMs: Long) {
        if (!isRunning) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ startListeningLoop() }, delayMs)
    }

    private fun openChinuApp() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            putExtra("wake_triggered", true)
        }
        startActivity(openIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
