package com.example.config

import android.content.Context
import com.google.firebase.FirebaseApp

object AdminConfig {
    /**
     * Single source of truth for the allowed owner Gmail address inside the Android app.
     * Replace this placeholder with your exact Gmail address before building the release APK.
     */
    const val ADMIN_EMAIL: String = "REPLACE_WITH_MY_GMAIL"

    const val FUNCTIONS_REGION: String = "us-central1"
    const val DEFAULT_INTERSTITIAL_INTERVAL_SEC: Int = 120
    const val MIN_INTERSTITIAL_INTERVAL_SEC: Int = 10
    const val MAX_INTERSTITIAL_INTERVAL_SEC: Int = 600

    fun isFirebaseConfigured(context: Context): Boolean {
        return FirebaseApp.getApps(context.applicationContext).isNotEmpty()
    }

    /**
     * Resolves R.string.default_web_client_id dynamically so the project compiles
     * cleanly even before app/google-services.json is added.
     */
    fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
