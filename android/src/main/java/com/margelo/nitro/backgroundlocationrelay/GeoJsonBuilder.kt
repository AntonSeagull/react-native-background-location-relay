package com.margelo.nitro.backgroundlocationrelay

import android.location.Location
import org.json.JSONArray
import org.json.JSONObject

object GeoJsonBuilder {
  private const val PLACEHOLDER = "$GEO$"

  fun geoJson(location: Location): JSONObject {
    return JSONObject()
      .put("latitude", location.latitude)
      .put("longitude", location.longitude)
      .put("accuracy", validAccuracy(location.accuracy))
      .put("altitude", validAltitude(location.altitude))
      .put("altitudeAccuracy", validAccuracy(location.verticalAccuracyMeters))
      .put("speed", validSpeed(location.speed))
      .put("speedAccuracy", validSpeedAccuracy(location))
      .put("heading", validHeading(location.bearing))
      .put("headingAccuracy", validHeadingAccuracy(location))
      .put("timestamp", location.time)
  }

  fun buildRequestBody(bodyTemplate: String, location: Location): String {
    val template = JSONObject(bodyTemplate)
    val geo = geoJson(location)
    val injected = injectGeo(template, geo)
    return injected.toString()
  }

  private fun injectGeo(value: Any, geo: JSONObject): Any {
    if (value is String && value == PLACEHOLDER) {
      return geo
    }

    if (value is JSONObject) {
      val result = JSONObject()
      value.keys().forEach { key ->
        result.put(key, injectGeo(value.get(key), geo))
      }
      return result
    }

    if (value is JSONArray) {
      val result = JSONArray()
      for (index in 0 until value.length()) {
        result.put(injectGeo(value.get(index), geo))
      }
      return result
    }

    return value
  }

  private fun validAccuracy(value: Float): Any {
    return if (value >= 0f) value.toDouble() else JSONObject.NULL
  }

  private fun validAltitude(value: Double): Any {
    return if (value.isFinite()) value else JSONObject.NULL
  }

  private fun validSpeed(value: Float): Any {
    return if (value >= 0f) value.toDouble() else JSONObject.NULL
  }

  private fun validSpeedAccuracy(location: Location): Any {
    return if (location.hasSpeedAccuracy() && location.speedAccuracyMetersPerSecond >= 0f) {
      location.speedAccuracyMetersPerSecond.toDouble()
    } else {
      JSONObject.NULL
    }
  }

  private fun validHeading(value: Float): Any {
    return if (value >= 0f) value.toDouble() else JSONObject.NULL
  }

  private fun validHeadingAccuracy(location: Location): Any {
    return if (location.hasBearingAccuracy() && location.bearingAccuracyDegrees >= 0f) {
      location.bearingAccuracyDegrees.toDouble()
    } else {
      JSONObject.NULL
    }
  }
}

class RelayException(message: String) : Exception(message)
