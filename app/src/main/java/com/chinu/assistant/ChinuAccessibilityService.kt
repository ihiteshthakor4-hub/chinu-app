package com.chinu.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
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

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun scrollDown() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val startY = dm.heightPixels * 0.75f
        val endY = dm.heightPixels * 0.25f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun scrollUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val startY = dm.heightPixels * 0.25f
        val endY = dm.heightPixels * 0.75f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }
}
    // WhatsApp માં જાતે કોન્ટેક્ટ સર્ચ કરીને ખાલી મેસેજ મોકલવા માટે
    fun sendWhatsAppAuto(contactName: String, message: String) {
        handler.postDelayed({
            val root = rootInActiveWindow ?: return@postDelayed
            val searchBtn = findSearchButton(root)
            if (searchBtn != null) {
                clickNodeOrParent(searchBtn)
                handler.postDelayed({
                    val searchRoot = rootInActiveWindow ?: return@postDelayed
                    val searchBox = findEditableNode(searchRoot)
                    if (searchBox != null) {
                        typeIntoNode(searchBox, contactName)
                        // ૧.૨ સેકન્ડ પછી કોન્ટેક્ટ પર ક્લિક કરો
                        handler.postDelayed({
                            val listRoot = rootInActiveWindow ?: return@postDelayed
                            val contactNode = findContactByName(listRoot, contactName)
                            if (contactNode != null) {
                                clickNodeOrParent(contactNode)
                                // ૧ સેકન્ડ પછી મેસેજ ટાઈપ કરીને Send દબાવો
                                handler.postDelayed({
                                    val chatRoot = rootInActiveWindow ?: return@postDelayed
                                    val msgBox = findEditableNode(chatRoot)
                                    if (msgBox != null) {
                                        typeIntoNode(msgBox, message)
                                        handler.postDelayed({
                                            val sendRoot = rootInActiveWindow ?: return@postDelayed
                                            val sendBtn = findButtonByDesc(sendRoot, "send")
                                            if (sendBtn != null) clickNodeOrParent(sendBtn)
                                        }, 500L)
                                    }
                                }, 1000L)
                            }
                        }, 1200L)
                    }
                }, 800L)
            }
        }, 1500L)
    }

    private fun findContactByName(node: AccessibilityNodeInfo?, name: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val txt = node.text?.toString()?.lowercase() ?: ""
        // Search box ને છોડીને નીચેના લિસ્ટમાંથી નામ શોધો
        if (node.isVisibleToUser && !node.isEditable && txt.contains(name.lowercase())) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findContactByName(node.getChild(i), name)
            if (found != null) return found
        }
        return null
    }

    private fun findButtonByDesc(node: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (node.isVisibleToUser && desc.contains(keyword)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findButtonByDesc(node.getChild(i), keyword)
            if (found != null) return found
        }
        return null
    }
