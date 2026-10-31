package com.example.uberreader

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 監聽 Uber 系列 App 的畫面變化與通知，把畫面上所有文字（含元件 id）整理成樹狀文字存起來。
 * 目前只做「看」：先搞清楚訂單進來時畫面上有哪些欄位，之後再決定要朗讀或放大哪些。
 */
class UberWatchService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingPackage: String? = null
    private var lastDumpHash = 0

    // 畫面內容變化很頻繁，等畫面穩定 800ms 再擷取一次
    private val dumpRunnable = Runnable { dumpActiveWindow() }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!pkg.startsWith("com.ubercab")) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> recordNotification(pkg, event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                pendingPackage = pkg
                handler.removeCallbacks(dumpRunnable)
                handler.postDelayed(dumpRunnable, 800)
            }
        }
    }

    private fun recordNotification(pkg: String, event: AccessibilityEvent) {
        val sb = StringBuilder()
        event.text.forEach { sb.appendLine("text: $it") }
        val n = event.parcelableData as? Notification
        n?.extras?.let { extras ->
            extras.getCharSequence(Notification.EXTRA_TITLE)?.let { sb.appendLine("標題: $it") }
            extras.getCharSequence(Notification.EXTRA_TEXT)?.let { sb.appendLine("內容: $it") }
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { sb.appendLine("詳細: $it") }
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { sb.appendLine("副標: $it") }
        }
        if (sb.isNotBlank()) SnapshotStore.append(this, "通知 $pkg", sb.toString().trimEnd())
    }

    private fun dumpActiveWindow() {
        val pkg = pendingPackage ?: return
        // 叫車／派單畫面有時是浮在上面的獨立視窗，所以把 Uber 的所有視窗都掃一遍
        val roots = windows.mapNotNull { it.root }
            .filter { it.packageName?.toString()?.startsWith("com.ubercab") == true }
            .ifEmpty { listOfNotNull(rootInActiveWindow) }

        val sb = StringBuilder()
        roots.forEach { walk(it, 0, sb) }
        val text = sb.toString().trimEnd()
        if (text.isEmpty()) return

        // 同一個畫面不要重複存
        val hash = text.hashCode()
        if (hash == lastDumpHash) return
        lastDumpHash = hash

        SnapshotStore.append(this, "畫面 $pkg", text)
    }

    private fun walk(node: AccessibilityNodeInfo, depth: Int, sb: StringBuilder) {
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        if (text.isNotEmpty() || desc.isNotEmpty()) {
            val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
            sb.append("  ".repeat(depth.coerceAtMost(20)))
            if (text.isNotEmpty()) sb.append(text)
            if (desc.isNotEmpty() && desc != text) sb.append(" 〔$desc〕")
            if (id.isNotEmpty()) sb.append("  #").append(id)
            if (node.isClickable) sb.append("  [可點]")
            sb.append('\n')
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { walk(it, depth + 1, sb) }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(dumpRunnable)
        super.onDestroy()
    }
}
