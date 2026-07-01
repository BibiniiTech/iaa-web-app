package com.bibiniitech.iaaapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
            "/su/bin/su"
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
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * Checks if the app is running on an emulator.
     */
    fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
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
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                if (currentHash == expectedSignatureHash) return true
            }
            false
        } catch (e: Exception) {
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
            "org.adaway"
        )
        val pm = context.packageManager
        for (pkg in tamperApps) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Not found
            }
        }
        return false
    }
}
