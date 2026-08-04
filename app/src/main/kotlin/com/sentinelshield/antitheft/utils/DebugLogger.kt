package com.sentinelshield.antitheft.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sentinelshield.antitheft.SecurityPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object DebugLogger {
    private const val TAG = "DebugLogger"
    private const val MAX_LOG_MEMORY_ENTRIES = 500
    private const val LOG_FILE_NAME = "sentinel_debug_logs.txt"
    private val memoryLogs = ArrayDeque<String>()
    private val executor = Executors.newSingleThreadExecutor()

    fun log(context: Context, tag: String, message: String, force: Boolean = false) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logLine = "[$timestamp] [$tag] $message"
        Log.d(tag, message)

        if (!force && !SecurityPreferences.isDebugLoggingEnabled(context)) return

        synchronized(memoryLogs) {
            if (memoryLogs.size >= MAX_LOG_MEMORY_ENTRIES) {
                memoryLogs.removeFirst()
            }
            memoryLogs.addLast(logLine)
        }

        executor.execute {
            try {
                val file = File(context.filesDir, LOG_FILE_NAME)
                file.appendText("$logLine\n")
                if (file.length() > 2 * 1024 * 1024) {
                    val lines = file.readLines().takeLast(300)
                    file.writeText(lines.joinToString("\n") + "\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log to file", e)
            }
        }
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                val text = file.readText()
                if (text.isNotBlank()) text else getMemoryLogsAsString()
            } else {
                getMemoryLogsAsString()
            }
        } catch (e: Exception) {
            getMemoryLogsAsString()
        }
    }

    private fun getMemoryLogsAsString(): String {
        return synchronized(memoryLogs) {
            if (memoryLogs.isEmpty()) {
                "No diagnostic logs recorded yet. Enable 'Debug Logging' above to start collecting diagnostic logs."
            } else {
                memoryLogs.joinToString("\n")
            }
        }
    }

    fun clearLogs(context: Context) {
        synchronized(memoryLogs) {
            memoryLogs.clear()
        }
        executor.execute {
            try {
                val file = File(context.filesDir, LOG_FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear log file", e)
            }
        }
    }

    fun shareLogs(context: Context) {
        var logs = getLogs(context)
        // Cap logs length to avoid TransactionTooLargeException in Intent extras
        if (logs.length > 100_000) {
            logs = logs.takeLast(100_000)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SentinelShield Diagnostic Logs")
            putExtra(Intent.EXTRA_TEXT, logs)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Diagnostic Logs").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
