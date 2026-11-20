package app.sole.tracker.data

import android.content.Context

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("sole_prefs", Context.MODE_PRIVATE)

    var isOnboarded: Boolean
        get() = prefs.getBoolean("is_onboarded", false)
        set(value) = prefs.edit().putBoolean("is_onboarded", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "Noah") ?: "Noah"
        set(value) = prefs.edit().putString("user_name", value.trim().ifEmpty { "Noah" }).apply()

    var avatarUri: String?
        get() = prefs.getString("avatar_uri", null)
        set(value) = prefs.edit().putString("avatar_uri", value).apply()

    var dailyStepGoal: Int
        get() = prefs.getInt("daily_step_goal", 10_000)
        set(value) = prefs.edit().putInt("daily_step_goal", value.coerceAtLeast(500)).apply()

    var isImperial: Boolean
        get() = prefs.getBoolean("is_imperial", false)
        set(value) = prefs.edit().putBoolean("is_imperial", value).apply()

    var useBodyMetrics: Boolean
        get() = prefs.getBoolean("use_body_metrics", false)
        set(value) = prefs.edit().putBoolean("use_body_metrics", value).apply()

    var heightCm: Float
        get() = prefs.getFloat("height_cm", 175f)
        set(value) = prefs.edit().putFloat("height_cm", value).apply()

    var weightKg: Float
        get() = prefs.getFloat("weight_kg", 70f)
        set(value) = prefs.edit().putFloat("weight_kg", value).apply()

    var backgroundServiceEnabled: Boolean
        get() = prefs.getBoolean("bg_service_enabled", false)
        set(value) = prefs.edit().putBoolean("bg_service_enabled", value).apply()

    var aiEnabled: Boolean
        get() = prefs.getBoolean("ai_enabled", true)
        set(value) = prefs.edit().putBoolean("ai_enabled", value).apply()

    var googleApiKey: String
        get() = prefs.getString("google_api_key", "") ?: ""
        set(value) = prefs.edit().putString("google_api_key", value.trim()).apply()

    // Hardware step counter baseline tracking
    var lastSensorTotal: Float
        get() = prefs.getFloat("last_sensor_total", -1f)
        set(value) = prefs.edit().putFloat("last_sensor_total", value).apply()

    var lastSensorDate: String
        get() = prefs.getString("last_sensor_date", "") ?: ""
        set(value) = prefs.edit().putString("last_sensor_date", value).apply()

    fun shouldShowAiFeatures(): Boolean = aiEnabled && googleApiKey.isNotBlank()
}
