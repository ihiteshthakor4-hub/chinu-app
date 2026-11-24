package com.chinu.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class ChinuAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ChinuAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun scrollDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val dm = resources.displayMetrics
            val x = dm.widthPixels / 2f
            val path = Path().apply {
                moveTo(x, dm.heightPixels * 0.75f)
                lineTo(x, dm.heightPixels * 0.25f)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    fun scrollUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val dm = resources.displayMetrics
            val x = dm.widthPixels / 2f
            val path = Path().apply {
                moveTo(x, dm.heightPixels * 0.25f)
                lineTo(x, dm.heightPixels * 0.75f)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
