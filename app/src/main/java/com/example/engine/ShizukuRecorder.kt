package com.example.engine

import android.util.Log
import rikka.shizuku.Shizuku
import java.io.File

object ShizukuRecorder {
    private var process: Process? = null

    fun isReady(): Boolean {
        return try {
            Shizuku.pingBinder()
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(onResult: (Boolean) -> Unit) {
        try {
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                onResult(true)
                return
            }
            Shizuku.addRequestPermissionResultListener(object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    onResult(grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED)
                }
            })
            Shizuku.requestPermission(1001)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun startRecording(outputFile: File, width: Int, height: Int, fps: Int, bitrateBps: Int, internalAudio: Boolean) {
        if (!isReady()) return
        try {
            // Using ADB screenrecord command for true raw hardware-level recording
            val audioFlag = if (internalAudio) "--audio-source=internal" else ""
            val command = "screenrecord --size ${width}x${height} --bit-rate $bitrateBps --time-limit 0 $audioFlag ${outputFile.absolutePath}"
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method = shizukuClass.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            Log.d("ShizukuRecorder", "Started Shizuku screenrecord: $command")
        } catch (e: Exception) {
            Log.e("ShizukuRecorder", "Failed to start", e)
        }
    }

    fun stopRecording() {
        try {
            process?.destroy()
            process = null
        } catch (e: Exception) {
            Log.e("ShizukuRecorder", "Failed to stop", e)
        }
    }
}
