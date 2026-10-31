package com.example.uberreader

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 把擷取到的畫面／通知內容存成本機文字檔，只存在手機裡。 */
object SnapshotStore {
    private const val FILE_NAME = "snapshots.txt"
    private const val MAX_BYTES = 2L * 1024 * 1024 // 超過 2MB 就砍掉舊的一半

    private val timeFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.TAIWAN)

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun append(context: Context, title: String, body: String) {
        val f = file(context)
        if (f.exists() && f.length() > MAX_BYTES) {
            val text = f.readText()
            f.writeText(text.substring(text.length / 2))
        }
        val header = "===== ${timeFmt.format(Date())}  $title ====="
        f.appendText("$header\n$body\n\n")
    }

    @Synchronized
    fun readAll(context: Context): String {
        val f = file(context)
        return if (f.exists()) f.readText() else ""
    }

    @Synchronized
    fun clear(context: Context) {
        file(context).delete()
    }
}
