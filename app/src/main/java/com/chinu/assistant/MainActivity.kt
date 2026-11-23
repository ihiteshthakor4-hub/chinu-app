package com.chinu.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

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

    private fun requestPermissionsIfNeeded() {
        val perms = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
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
            tts?.language = Locale("en", "IN")
            val voices = tts?.voices
            val femaleVoice = voices?.firstOrNull { v ->
                v.locale.language == "en" && v.locale.country == "IN" &&
                !v.isNetworkConnectionRequired &&
                (v.name.contains("ene", true) || v.name.contains("end", true) || v.name.contains("female", true))
            } ?: voices?.firstOrNull { v -> v.locale.language == "en" && v.locale.country == "IN" }

            if (femaleVoice != null) tts?.voice = femaleVoice
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(0.95f)
        }
    }

    inner class ChinuBridge {
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
        fun speak(text: String) {
            runOnUiThread { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CHINU_TTS") }
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

        @JavascriptInterface
        fun openApp(appName: String): Boolean {
            return try {
                val name = appName.lowercase(Locale.getDefault())
                val intent: Intent? = when {
                    name.contains("whatsapp") -> packageManager.getLaunchIntentForPackage("com.whatsapp")
                    name.contains("youtube") -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    name.contains("instagram") || name.contains("insta") -> packageManager.getLaunchIntentForPackage("com.instagram.android")
                    name.contains("chrome") -> packageManager.getLaunchIntentForPackage("com.android.chrome")
                    name.contains("camera") -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    name.contains("settings") || name.contains("setting") -> Intent(Settings.ACTION_SETTINGS)
                    name.contains("call") || name.contains("phone") || name.contains("dialer") -> Intent(Intent.ACTION_DIAL)
                    else -> null
                }
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    true
                } else false
            } catch (e: Exception) {
                false
            }
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
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
