package ru.rice.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes a private backup and, when permitted, Documents/RICE/RICE_LOG.txt. */
object RiceLog {
    private const val TAG = "RICE"
    private const val MAX_BYTES = 1024L * 1024L
    private val lock = Any()
    @Volatile private var privateFile: File? = null
    @Volatile private var publicFile: File? = null

    fun initialize(context: Context) = synchronized(lock) {
        val dir = File(context.filesDir, "rice_logs")
        if (!dir.exists()) dir.mkdirs()
        privateFile = File(dir, "RICE_LOG_PRIVATE.txt")
        writeLocked("INFO", "LOGGER", "RICE 0.2.0 logger initialized")
        writeLocked("INFO", "DEVICE", "Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}; SDK ${android.os.Build.VERSION.SDK_INT}")
    }

    fun enablePublicLog(context: Context): Boolean = synchronized(lock) {
        try {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) return@synchronized false
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(docs, "RICE")
            if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Cannot create ${dir.absolutePath}")
            val target = File(dir, "RICE_LOG.txt")
            if (!target.exists()) privateFile?.takeIf { it.exists() }?.copyTo(target, overwrite = false)
            publicFile = target
            writeLocked("INFO", "LOGGER", "Public log enabled: ${target.absolutePath}")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Public log unavailable", t)
            writeLocked("ERROR", "LOGGER", "Public log unavailable: ${Log.getStackTraceString(t)}")
            false
        }
    }

    fun info(tag: String, message: String) = write("INFO", tag, message)
    fun warn(tag: String, message: String) = write("WARN", tag, message)
    fun error(tag: String, message: String, error: Throwable? = null) {
        write("ERROR", tag, message + if (error == null) "" else "\n${Log.getStackTraceString(error)}")
    }

    fun logFile(context: Context): File {
        if (privateFile == null) initialize(context)
        return publicFile ?: privateFile!!
    }

    fun publicPath(): String = publicFile?.absolutePath ?: "Documents/RICE/RICE_LOG.txt (после разрешения)"

    private fun write(level: String, tag: String, message: String) = synchronized(lock) {
        if (privateFile == null) return@synchronized
        writeLocked(level, tag, message)
    }

    private fun writeLocked(level: String, tag: String, message: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "[$stamp] $level/$tag: $message\n"
        privateFile?.let { append(it, line, "RICE_LOG_PREVIOUS.txt") }
        publicFile?.let { append(it, line, "RICE_LOG_PREVIOUS.txt") }
    }

    private fun append(file: File, line: String, previousName: String) {
        try {
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            if (file.exists() && file.length() > MAX_BYTES) {
                val previous = File(file.parentFile, previousName)
                if (previous.exists()) previous.delete()
                file.renameTo(previous)
            }
            FileOutputStream(file, true).use { out ->
                out.write(line.toByteArray(Charsets.UTF_8))
                out.flush()
                out.fd.sync()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Cannot append log to ${file.absolutePath}", t)
        }
    }
}
