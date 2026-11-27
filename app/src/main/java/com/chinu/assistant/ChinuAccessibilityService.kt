package com.chinu.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ChinuAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ChinuAccessibilityService? = null
    }

    private val handler = Handler(Looper.getMainLooper())

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

    fun searchInCurrentApp(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val editNode = findEditableNode(root)
        if (editNode != null) {
            typeIntoNode(editNode, query)
            return true
        }

        val searchBtn = findSearchButton(root)
        if (searchBtn != null) {
            clickNodeOrParent(searchBtn)
            handler.postDelayed({
                val newRoot = rootInActiveWindow ?: return@postDelayed
                val newEdit = findEditableNode(newRoot)
                if (newEdit != null) {
                    typeIntoNode(newEdit, query)
                }
            }, 700L)
            return true
        }
        return false
    }

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
                        handler.postDelayed({
                            val listRoot = rootInActiveWindow ?: return@postDelayed
                            val contactNode = findContactByName(listRoot, contactName)
                            if (contactNode != null) {
                                clickNodeOrParent(contactNode)
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

    private fun typeIntoNode(node: AccessibilityNodeInfo, text: String) {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isVisibleToUser) return node
        for (i in 0 until node.childCount) {
            val found = findEditableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findSearchButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val txt = node.text?.toString()?.lowercase() ?: ""
        if (node.isVisibleToUser && (desc.contains("search") || txt.contains("search"))) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findSearchButton(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findContactByName(node: AccessibilityNodeInfo?, name: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val txt = node.text?.toString()?.lowercase() ?: ""
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

    private fun clickNodeOrParent(node: AccessibilityNodeInfo?) {
        var curr = node
        while (curr != null) {
            if (curr.isClickable) {
                curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
            curr = curr.parent
        }
    }
}
