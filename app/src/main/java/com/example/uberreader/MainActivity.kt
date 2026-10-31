package com.example.uberreader

import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        status = TextView(this).apply { textSize = 18f }
        root.addView(status)

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(button("開啟權限") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        buttons.addView(button("重新整理") { refresh() })
        buttons.addView(button("分享") { share() })
        buttons.addView(button("清除") {
            SnapshotStore.clear(this)
            refresh()
        })
        root.addView(buttons)

        log = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        scroll = ScrollView(this).apply {
            addView(log)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(scroll)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun button(label: String, onClick: (View) -> Unit) = Button(this).apply {
        text = label
        setOnClickListener(onClick)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun refresh() {
        status.text = if (isServiceEnabled()) {
            "✅ 服務已開啟：打開 Uber，等訂單進來後回來這裡看"
        } else {
            "⚠️ 服務未開啟：按「開啟權限」→ 已安裝的應用程式 → ${getString(R.string.service_label)} → 開啟"
        }
        log.text = SnapshotStore.readAll(this).ifEmpty { "（還沒有擷取到任何內容）" }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun share() {
        // 太長的話只分享最後 100KB，避免 Intent 爆掉
        val text = SnapshotStore.readAll(this).takeLast(100_000)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "分享擷取紀錄"))
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, UberWatchService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
