/*
 * ============================================================================
 * PROPRIETARY AND CONFIDENTIAL PROPERTY OF CLEMENT KOFI ACHEAMPONG
 * (BIBINII TECH) - ALL RIGHTS RESERVED.
 *
 * Application: Internal Auditing Support & Statutory Reporting Platform (IAA App)
 * Author: Clement Kofi Acheampong (Internal Auditor & Software Engineer)
 * Copyright (c) 2024-2026 Clement Kofi Acheampong. All Rights Reserved.
 *
 * NOTICE: All information, source code, algorithms, and data designs contained
 * herein are protected under the Ghana Copyright Act, 2005 (Act 690) and
 * international intellectual property treaties. Unauthorized copying,
 * decompilation, reverse engineering, redistribution, or modification of this
 * file via any medium is strictly prohibited without prior written consent.
 * ============================================================================
 */

package com.bibiniitech.iaaapp

import android.content.Context
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for decrypting proprietary and trade-secret asset databases in memory.
 * Prevents raw extraction of curated statutory criteria from APK packages.
 */
object AssetCryptoUtils {

    // 128-bit key segments constructed at runtime to prevent string constant extraction
    private val K1 = byteArrayOf(0x49, 0x41, 0x41, 0x5f) // "IAA_"
    private val K2 = byteArrayOf(0x41, 0x75, 0x64, 0x69) // "Audi"
    private val K3 = byteArrayOf(0x74, 0x5f, 0x53, 0x65) // "t_Se"
    private val K4 = byteArrayOf(0x63, 0x75, 0x72, 0x65) // "cure"

    private fun getSecretKey(): SecretKeySpec {
        val combined = K1 + K2 + K3 + K4
        return SecretKeySpec(combined, "AES")
    }

    /**
     * Opens an encrypted asset using AES-CBC with prepended 16-byte IV.
     * Falls back to plain file if encrypted file is not present.
     */
    fun openAssetStream(context: Context, encryptedName: String, fallbackPlainName: String): InputStream {
        return try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (assetList.contains(encryptedName)) {
                val rawInput = context.assets.open(encryptedName)
                val iv = ByteArray(16)
                val read = rawInput.read(iv)
                if (read == 16) {
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))
                    CipherInputStream(rawInput, cipher)
                } else {
                    rawInput.close()
                    context.assets.open(fallbackPlainName)
                }
            } else {
                context.assets.open(fallbackPlainName)
            }
        } catch (_: Exception) {
            context.assets.open(fallbackPlainName)
        }
    }

    /**
     * Reads asset text transparently from encrypted file or fallback plain file.
     */
    fun readAssetString(context: Context, encryptedName: String, fallbackPlainName: String): String {
        return openAssetStream(context, encryptedName, fallbackPlainName).bufferedReader().use { it.readText() }
    }
}
