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

import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

@Keep
data class CloudConfig(
    @SerializedName("mda_to") val mdaTo: List<String>? = null,
    @SerializedName("mda_cc") val mdaCc: List<String>? = null,
    @SerializedName("mmda_to") val mmdaTo: List<String>? = null,
    @SerializedName("region_cc") val regionCc: Map<String, List<String>>? = null,
    @SerializedName("mmda_institution_cc") val mmdaInstitutionCc: Map<String, Map<String, List<String>>>? = null,
)

object CloudEmailManager {
    private val gson = Gson()
    private var firestoreConfigJson: String = ""

    fun updateFirestoreConfig(json: String) {
        firestoreConfigJson = json
    }

    fun getMdaTo(category: String): List<String> {
        val cloudData = getCloudData(category)
        val list = cloudData?.mdaTo?.filter { it.isNotBlank() }
        if (!list.isNullOrEmpty()) return list
        return when (category) {
            "quarterly" -> EmailConfig.Quarterly.MDA_TO
            "annual" -> EmailConfig.Annual.MDA_TO
            "ccc" -> EmailConfig.CCC.MDA_TO
            "soi" -> EmailConfig.SOI.MDA_TO
            "other" -> EmailConfig.Other.MDA_TO
            else -> emptyList()
        }.filter { it.isNotBlank() }
    }

    fun getMdaCc(category: String): List<String> {
        val cloudData = getCloudData(category)
        val list = cloudData?.mdaCc?.filter { it.isNotBlank() }
        if (!list.isNullOrEmpty()) return list
        return when (category) {
            "quarterly" -> EmailConfig.Quarterly.MDA_CC
            "annual" -> EmailConfig.Annual.MDA_CC
            "ccc" -> EmailConfig.CCC.MDA_CC
            "soi" -> EmailConfig.SOI.MDA_CC
            "other" -> EmailConfig.Other.MDA_CC
            else -> emptyList()
        }.filter { it.isNotBlank() }
    }

    fun getMmdaTo(category: String): List<String> {
        val cloudData = getCloudData(category)
        val list = cloudData?.mmdaTo?.filter { it.isNotBlank() }
        if (!list.isNullOrEmpty()) return list
        return when (category) {
            "quarterly" -> EmailConfig.Quarterly.MMDA_TO
            "annual" -> EmailConfig.Annual.MMDA_TO
            "ccc" -> EmailConfig.CCC.MMDA_TO
            "soi" -> EmailConfig.SOI.MMDA_TO
            "other" -> EmailConfig.Other.MMDA_TO
            else -> emptyList()
        }.filter { it.isNotBlank() }
    }

    fun getRegionCc(category: String, regionName: String): List<String> {
        val cloudData = getCloudData(category)
        val cloudRegionCc = cloudData?.regionCc?.get(regionName)?.filter { it.isNotBlank() }
        if (!cloudRegionCc.isNullOrEmpty()) return cloudRegionCc
        val fallbackMap = when (category) {
            "quarterly" -> EmailConfig.Quarterly.REGION_CC
            "annual" -> EmailConfig.Annual.REGION_CC
            "ccc" -> EmailConfig.CCC.REGION_CC
            "soi" -> EmailConfig.SOI.REGION_CC
            "other" -> EmailConfig.Other.REGION_CC
            else -> emptyMap()
        }
        return fallbackMap[regionName]?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun getMmdaInstitutionCc(category: String, regionName: String, institutionName: String): List<String> {
        val cloudData = getCloudData(category)
        val cloudMmdaInstitutionCc = cloudData?.mmdaInstitutionCc?.get(regionName)?.get(institutionName)?.filter { it.isNotBlank() }
        if (!cloudMmdaInstitutionCc.isNullOrEmpty()) return cloudMmdaInstitutionCc
        val fallbackMap = when (category) {
            "quarterly" -> EmailConfig.Quarterly.MMDA_INSTITUTION_CC
            "annual" -> EmailConfig.Annual.MMDA_INSTITUTION_CC
            else -> null
        }
        return fallbackMap?.get(regionName)?.get(institutionName)?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun getCloudData(category: String): CloudConfig? {
        if (firestoreConfigJson.isNotEmpty()) {
            try {
                val type = object : TypeToken<Map<String, CloudConfig>>() {}.type
                val map = gson.fromJson<Map<String, CloudConfig>>(firestoreConfigJson, type)
                if ((map != null) && map.containsKey(category)) {
                    return map[category]
                }
            } catch (_: Exception) {}

            try {
                return gson.fromJson(firestoreConfigJson, CloudConfig::class.java)
            } catch (_: Exception) {}
        }
        val json = Firebase.remoteConfig.getString("config_$category")
        return if (json.isNotEmpty()) {
            try {
                gson.fromJson(json, CloudConfig::class.java)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
}
