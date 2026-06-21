package com.tejaslabs.beauty.ai.camera.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var firebaseCrashlytics: FirebaseCrashlytics? = null
    private var firebaseRemoteConfig: FirebaseRemoteConfig? = null

    fun init(context: Context) {
        try {
            // Check if Firebase is initialized (either automatically or manually)
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                firebaseAnalytics = Firebase.analytics
                firebaseCrashlytics = Firebase.crashlytics
                firebaseRemoteConfig = Firebase.remoteConfig

                // Configure Remote Config settings
                val configSettings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 3600 // 1 hour fetch interval
                }
                firebaseRemoteConfig?.setConfigSettingsAsync(configSettings)

                // Set default values for Remote Config
                val defaults = mapOf(
                    "enable_advanced_beauty_filters" to true,
                    "default_filter_intensity" to 0.60f,
                    "min_face_detection_confidence" to 0.5f
                )
                firebaseRemoteConfig?.setDefaultsAsync(defaults)

                // Fetch and activate config
                firebaseRemoteConfig?.fetchAndActivate()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Remote Config fetch and activate succeeded")
                        } else {
                            Log.w(TAG, "Remote Config fetch and activate failed")
                        }
                    }

                isInitialized = true
                Log.d(TAG, "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }
    }

    fun logEvent(name: String, params: Bundle? = null) {
        if (!isInitialized) return
        try {
            firebaseAnalytics?.logEvent(name, params)
            Log.d(TAG, "Logged event: $name with params: $params")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $name", e)
        }
    }

    fun recordException(throwable: Throwable) {
        if (!isInitialized) return
        try {
            firebaseCrashlytics?.recordException(throwable)
            Log.d(TAG, "Recorded exception: ${throwable.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception", e)
        }
    }

    fun getRemoteConfigBoolean(key: String, defaultValue: Boolean): Boolean {
        if (!isInitialized) return defaultValue
        return try {
            firebaseRemoteConfig?.getBoolean(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getRemoteConfigDouble(key: String, defaultValue: Double): Double {
        if (!isInitialized) return defaultValue
        return try {
            firebaseRemoteConfig?.getDouble(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getRemoteConfigString(key: String, defaultValue: String): String {
        if (!isInitialized) return defaultValue
        return try {
            firebaseRemoteConfig?.getString(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
}
