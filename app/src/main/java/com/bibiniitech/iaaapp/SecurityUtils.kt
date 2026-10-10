package com.bibiniitech.iaaapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.io.File
import java.security.MessageDigest

object SecurityUtils {

    /**
     * Checks if the device is likely rooted.
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        
        // Execute 'su' command check
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = process.inputStream.bufferedReader()
            reader.readLine() != null
        } catch (_: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * Checks if the app is running on an emulator.
     */
    fun isRunningOnEmulator(): Boolean {
        val build = Build.FINGERPRINT
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || build.startsWith("generic")
                || build.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Detects if a debugger is attached.
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected()
    }

    /**
     * Checks if the app was modified or resigned.
     * @param context Application context.
     * @param expectedSignatureHash The Base64 encoded SHA-256 hash of the app's signing certificate.
     */
    fun isAppSignatureValid(context: Context, expectedSignatureHash: String): Boolean {
        if (expectedSignatureHash.isEmpty()) return true // Skip if no hash provided (e.g. debug mode)
        
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)

            val signatures = packageInfo.signingInfo?.signingCertificateHistory

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                if (currentHash == expectedSignatureHash) return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Detects if common patching tools or overlay apps are active.
     */
    fun isTamperToolDetected(context: Context): Boolean {
        val tamperApps = listOf(
            "com.chelpus.lackypatch", // Lucky Patcher
            "com.dimonvideo.luckypatcher",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.blackmartalpha",
        )
        val pm = context.packageManager
        for (pkg in tamperApps) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
                // Not found
            }
        }
        return false
    }

    /**
     * Checks if USB Debugging is enabled.
     */
    fun isUsbDebuggingEnabled(context: Context): Boolean {
        return android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.ADB_ENABLED, 0
        ) != 0
    }
}
