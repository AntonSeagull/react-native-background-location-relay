package com.margelo.nitro.backgroundlocationrelay

import android.content.Context
import org.json.JSONObject

object ConfigStore {
  private const val PREFS_NAME = "BackgroundLocationRelay"
  private const val CONFIG_KEY = "config"
  private const val WAS_RUNNING_KEY = "was_running"

  fun save(context: Context, config: BackgroundLocationRelayConfig) {
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(CONFIG_KEY, toJson(config).toString())
      .apply()
  }

  fun setWasRunning(context: Context, running: Boolean) {
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(WAS_RUNNING_KEY, running)
      .apply()
  }

  fun wasRunning(context: Context): Boolean {
    return context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(WAS_RUNNING_KEY, false)
  }

  fun load(context: Context): BackgroundLocationRelayConfig? {
    val raw =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(CONFIG_KEY, null)
        ?: return null

    return try {
      fromJson(JSONObject(raw))
    } catch (error: Exception) {
      RelayLogger.error("Failed to load persisted config: ${error.message}")
      null
    }
  }

  private fun toJson(config: BackgroundLocationRelayConfig): JSONObject {
    val location = config.location
    val request = config.request

    val locationJson =
      JSONObject()
        .put("interval", location.interval)
        .put("fastestInterval", location.fastestInterval)
        .put("distanceFilter", location.distanceFilter)
        .put("accuracy", location.accuracy?.name?.lowercase())
        .put(
          "pausesLocationUpdatesAutomatically",
          location.pausesLocationUpdatesAutomatically,
        )
        .put(
          "showsBackgroundLocationIndicator",
          location.showsBackgroundLocationIndicator,
        )

    val requestJson =
      JSONObject()
        .put("endpoint", request.endpoint)
        .put("headersJson", request.headersJson)
        .put("timeout", request.timeout)
        .put("bodyTemplate", request.bodyTemplate)

    return JSONObject().put("location", locationJson).put("request", requestJson)
  }

  private fun fromJson(json: JSONObject): BackgroundLocationRelayConfig {
    val locationJson = json.getJSONObject("location")
    val requestJson = json.getJSONObject("request")

    val accuracyValue = locationJson.optString("accuracy", "")
    val accuracy =
      if (accuracyValue.isEmpty()) {
        null
      } else {
        LocationAccuracy.values().firstOrNull {
          it.name.equals(accuracyValue, ignoreCase = true)
        }
      }

    return BackgroundLocationRelayConfig(
      location =
        LocationConfig(
          interval = locationJson.getDouble("interval"),
          fastestInterval = locationJson.optNullableDouble("fastestInterval"),
          distanceFilter = locationJson.optNullableDouble("distanceFilter"),
          accuracy = accuracy,
          pausesLocationUpdatesAutomatically =
            locationJson.optNullableBoolean("pausesLocationUpdatesAutomatically"),
          showsBackgroundLocationIndicator =
            locationJson.optNullableBoolean("showsBackgroundLocationIndicator"),
        ),
      request =
        RequestConfig(
          endpoint = requestJson.getString("endpoint"),
          headersJson = requestJson.optNullableString("headersJson"),
          timeout = requestJson.optNullableDouble("timeout"),
          bodyTemplate = requestJson.getString("bodyTemplate"),
        ),
    )
  }

  private fun JSONObject.optNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) {
      return null
    }
    return getDouble(key)
  }

  private fun JSONObject.optNullableBoolean(key: String): Boolean? {
    if (!has(key) || isNull(key)) {
      return null
    }
    return getBoolean(key)
  }

  private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) {
      return null
    }
    val value = getString(key)
    return if (value.isEmpty()) null else value
  }
}
