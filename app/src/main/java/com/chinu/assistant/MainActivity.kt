package com.chinu.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isFlashOn = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()
        tts = TextToSpeech(this, this)
        setupBackgroundSpeechRecognizer()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        webView.addJavascriptInterface(ChinuBridge(), "Android")
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("wake_triggered", false) == true) {
            runOnUiThread {
                webView.evaluateJavascript("if(window.onVoiceResult) window.onVoiceResult('hey chinu');", null)
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            val missing = perms.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                requestPermissions(missing.toTypedArray(), 101)
            }
        }
    }

    private fun setupBackgroundSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread { webView.evaluateJavascript("if(window.onListeningStart) window.onListeningStart();", null) }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                runOnUiThread { webView.evaluateJavascript("if(window.onListeningEnd) window.onListeningEnd();", null) }
            }
            override fun onError(error: Int) {
                runOnUiThread { webView.evaluateJavascript("if(window.onListeningEnd) window.onListeningEnd();", null) }
            }
            override fun onResults(results: Bundle?) {
                val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (spokenText.isNotEmpty()) {
                    val safeText = spokenText.replace("'", "\\'").replace("\n", " ")
                    runOnUiThread {
                        webView.evaluateJavascript("if(window.onVoiceResult) window.onVoiceResult('$safeText');", null)
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setVoiceForLanguage("gu")
        }
    }

    private fun setVoiceForLanguage(langCode: String) {
        val locale = when (langCode) {
            "gu" -> Locale("gu", "IN")
            "hi" -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }
        tts?.language = locale
        val voices = tts?.voices
        val femaleVoice = voices?.firstOrNull { v ->
            v.locale.language == locale.language &&
            !v.name.contains("end", true) &&
            !v.name.contains("ene", true) &&
            !v.name.contains("male", true)
        }
        if (femaleVoice != null) tts?.voice = femaleVoice
        tts?.setPitch(1.05f)
        tts?.setSpeechRate(0.95f)
    }

    inner class ChinuBridge {
        @JavascriptInterface
        fun setWakeWordMode(enabled: Boolean) {
            runOnUiThread {
                if (enabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                        val permIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(permIntent)
                        return@runOnUiThread
                    }
                    val serviceIntent = Intent(this@MainActivity, ChinuWakeService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } else {
                    stopService(Intent(this@MainActivity, ChinuWakeService::class.java))
                }
            }
        }

        @JavascriptInterface
        fun startListening() {
            runOnUiThread {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                speechRecognizer?.startListening(intent)
            }
        }

        @JavascriptInterface
        fun speak(text: String, langCode: String) {
            runOnUiThread {
                setVoiceForLanguage(langCode)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CHINU_TTS")
            }
        }

        @JavascriptInterface
        fun toggleFlashlight(): Boolean {
            return try {
                val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                isFlashOn = !isFlashOn
                cm.setTorchMode(cm.cameraIdList[0], isFlashOn)
                isFlashOn
            } catch (e: Exception) {
                false
            }
        }

        // SUPER ALL-APPS OPENER
        @JavascriptInterface
        fun openApp(appName: String): Boolean {
            return try {
                val query = appName.lowercase(Locale.getDefault()).trim()
                val cleanQuery = query.replace(" ", "")

                // 1. Direct Popular Aliases
                val directIntent: Intent? = when {
                    query.contains("camera") -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    query.contains("setting") -> Intent(Settings.ACTION_SETTINGS)
                    query.contains("dialer") || query == "phone" || query == "call" -> Intent(Intent.ACTION_DIAL)
                    query.contains("whatsapp") || cleanQuery == "wp" -> packageManager.getLaunchIntentForPackage("com.whatsapp")
                    query.contains("youtube") || cleanQuery == "yt" -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    query.contains("instagram") || query.contains("insta") -> packageManager.getLaunchIntentForPackage("com.instagram.android")
                    query.contains("spotify") -> packageManager.getLaunchIntentForPackage("com.spotify.music")
                    query.contains("chrome") -> packageManager.getLaunchIntentForPackage("com.android.chrome")
                    query.contains("play store") || cleanQuery == "playstore" -> packageManager.getLaunchIntentForPackage("com.android.vending")
                    query.contains("facebook") || cleanQuery == "fb" -> packageManager.getLaunchIntentForPackage("com.facebook.katana")
                    query.contains("telegram") -> packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                    query.contains("snapchat") || query.contains("snap") -> packageManager.getLaunchIntentForPackage("com.snapchat.android")
                    else -> null
                }
                if (directIntent != null) {
                    directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(directIntent)
                    return true
                }

                // 2. Search ALL Installed Apps (Exact -> StartsWith -> Contains)
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val apps = packageManager.queryIntentActivities(mainIntent, 0)

                var matchedPkg: String? = null
                for (info in apps) {
                    val label = info.loadLabel(packageManager).toString().lowercase(Locale.getDefault()).trim()
                    val cleanLabel = label.replace(" ", "")
                    if (label == query || cleanLabel == cleanQuery) {
                        matchedPkg = info.activityInfo.packageName
                        break
                    }
                }
                if (matchedPkg == null) {
                    for (info in apps) {
                        val label = info.loadLabel(packageManager).toString().lowercase(Locale.getDefault()).trim()
                        val cleanLabel = label.replace(" ", "")
                        if (label.startsWith(query) || cleanLabel.startsWith(cleanQuery)) {
                            matchedPkg = info.activityInfo.packageName
                            break
                        }
                    }
                }
                if (matchedPkg == null) {
                    for (info in apps) {
                        val label = info.loadLabel(packageManager).toString().lowercase(Locale.getDefault()).trim()
                        val cleanLabel = label.replace(" ", "")
                        if (label.contains(query) || cleanLabel.contains(cleanQuery) || query.contains(label)) {
                            matchedPkg = info.activityInfo.packageName
                            break
                        }
                    }
                }

                if (matchedPkg != null) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(matchedPkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        return true
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }

        // VOLUME CONTROL
        @JavascriptInterface
        fun controlVolume(action: String) {
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                when (action) {
                    "up" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    "down" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    "max" -> am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
                    "mute" -> am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                }
            } catch (_: Exception) {}
        }

        // BRIGHTNESS CONTROL (level: 0 to 255)
        @JavascriptInterface
        fun setBrightness(level: Int): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return false
                }
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, level.coerceIn(10, 255))
                true
            } catch (_: Exception) {
                false
            }
        }

        // SET ALARM
        @JavascriptInterface
        fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
            return try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }

        // SET TIMER
        @JavascriptInterface
        fun setTimer(seconds: Int, message: String): Boolean {
            return try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }

        // PLAY ON SPOTIFY
        @JavascriptInterface
        fun playSpotify(songQuery: String) {
            try {
                val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    setPackage("com.spotify.music")
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                    putExtra(SearchManager.QUERY, songQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {
                try {
                    val uri = Uri.parse("spotify:search:" + Uri.encode(songQuery))
                    val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(fallback)
                } catch (_: Exception) {}
            }
        }

        // WI-FI & BLUETOOTH PANELS
        @JavascriptInterface
        fun openWifiPanel() {
            try {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun openBluetoothSettings() {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }

        // LOCK SCREEN & SCREENSHOT
        @JavascriptInterface
        fun lockScreen(): Boolean {
            return ChinuAccessibilityService.instance?.lockScreen() ?: false
        }

        @JavascriptInterface
        fun takeScreenshot(): Boolean {
            return ChinuAccessibilityService.instance?.takeScreenshot() ?: false
        }

        @JavascriptInterface
        fun searchGoogle(query: String) {
            try {
                val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun searchYouTube(query: String) {
            try {
                val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun sendWhatsApp(message: String) {
            try {
                val uri = Uri.parse("https://wa.me/?text=" + Uri.encode(message))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun dialNumber(number: String) {
            try {
                val cleanNum = number.replace("[^0-9+]".toRegex(), "")
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNum"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun scrollDown(): Boolean {
            val service = ChinuAccessibilityService.instance ?: return false
            service.scrollDown()
            return true
        }

        @JavascriptInterface
        fun scrollUp(): Boolean {
            val service = ChinuAccessibilityService.instance ?: return false
            service.scrollUp()
            return true
        }

        @JavascriptInterface
        fun goBack(): Boolean {
            val service = ChinuAccessibilityService.instance ?: return false
            service.performBack()
            return true
        }

        @JavascriptInterface
        fun goHome(): Boolean {
            val service = ChinuAccessibilityService.instance ?: return false
            service.performHome()
            return true
        }

        @JavascriptInterface
        fun openAccessibilitySettings() {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
