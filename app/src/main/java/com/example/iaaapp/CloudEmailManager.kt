package com.example.iaaapp

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class CloudConfig(
    @SerializedName("mda_to") val mdaTo: List<String>? = null,
    @SerializedName("mda_cc") val mdaCc: List<String>? = null,
    @SerializedName("mmda_to") val mmdaTo: List<String>? = null,
    @SerializedName("region_cc") val regionCc: Map<String, List<String>>? = null
)

object CloudEmailManager {
    private val gson = Gson()

    fun getMdaTo(category: String): List<String> {
        val cloudData = getCloudData(category)
        return cloudData?.mdaTo ?: when(category) {
            "quarterly" -> EmailConfig.Quarterly.MDA_TO
            "annual" -> EmailConfig.Annual.MDA_TO
            "ccc" -> EmailConfig.CCC.MDA_TO
            "soi" -> EmailConfig.SOI.MDA_TO
            "other" -> EmailConfig.Other.MDA_TO
            else -> emptyList()
        }
    }

    fun getMdaCc(category: String): List<String> {
        val cloudData = getCloudData(category)
        return cloudData?.mdaCc ?: when(category) {
            "quarterly" -> EmailConfig.Quarterly.MDA_CC
            "annual" -> EmailConfig.Annual.MDA_CC
            "ccc" -> EmailConfig.CCC.MDA_CC
            "soi" -> EmailConfig.SOI.MDA_CC
            "other" -> EmailConfig.Other.MDA_CC
            else -> emptyList()
        }
    }

    fun getMmdaTo(category: String): List<String> {
        val cloudData = getCloudData(category)
        return cloudData?.mmdaTo ?: when(category) {
            "quarterly" -> EmailConfig.Quarterly.MMDA_TO
            "annual" -> EmailConfig.Annual.MMDA_TO
            "ccc" -> EmailConfig.CCC.MMDA_TO
            "soi" -> EmailConfig.SOI.MMDA_TO
            "other" -> EmailConfig.Other.MMDA_TO
            else -> emptyList()
        }
    }

    fun getRegionCc(category: String, regionName: String): List<String> {
        val cloudData = getCloudData(category)
        val cloudRegionCc = cloudData?.regionCc?.get(regionName)
        
        if (cloudRegionCc != null) return cloudRegionCc

        return when(category) {
            "quarterly" -> EmailConfig.Quarterly.REGION_CC[regionName]
            "annual" -> EmailConfig.Annual.REGION_CC[regionName]
            "ccc" -> EmailConfig.CCC.REGION_CC[regionName]
            "soi" -> EmailConfig.SOI.REGION_CC[regionName]
            "other" -> EmailConfig.Other.REGION_CC[regionName]
            else -> null
        } ?: listOf("")
    }

    fun getCloudData(category: String): CloudConfig? {
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
