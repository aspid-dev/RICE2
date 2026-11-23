package ru.rice.agent

import android.app.Application
import android.util.Log

class RiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RiceLog.initialize(this)
        RiceLog.info("APPLICATION", "Application started")
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            RiceLog.enablePublicLog(this)
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            RiceLog.error("FATAL", "Uncaught exception on ${thread.name}", error)
            try { previous?.uncaughtException(thread, error) }
            catch (handlerError: Throwable) { Log.e("RICE", "Crash handler failed", handlerError) }
        }
    }
}
