package com.highmobility.exploreautoapis

import android.content.Context

class Credentials(context: Context) {
    val resources = context.resources
    val packageName = context.packageName

    val environment: String? by lazy { getResource("environment") }

    fun getResource(key: String): String? {
        return try {
            val resId = resources.getIdentifier(key, "string", packageName)
            resources.getString(resId)
        } catch (e: Exception) {
            null
        }
    }

    fun getEnvironmentResource(key: String): String? {
        // keys use format: {dev/prod}Key, eg devDeviceSerial
        if (environment != null) {
            return getResource("${environment}${key.capitalize()}")
        }

        return null
    }
}