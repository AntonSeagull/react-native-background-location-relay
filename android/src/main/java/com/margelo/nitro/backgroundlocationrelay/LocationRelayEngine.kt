package com.margelo.nitro.backgroundlocationrelay

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationRelayEngine {
  @Volatile
  private var config: BackgroundLocationRelayConfig? = null

  @Volatile
  private var running = false

  @Volatile
  private var lastDeliveryTimestamp = 0L

  private var locationCallback: LocationCallback? = null

  fun setConfig(context: Context, newConfig: BackgroundLocationRelayConfig) {
    config = newConfig
    ConfigStore.save(context.applicationContext, newConfig)
    if (running) {
      restartLocationUpdates(context.applicationContext)
    }
  }

  fun loadPersistedConfigIfNeeded(context: Context) {
    if (config == null) {
      config = ConfigStore.load(context.applicationContext)
    }
  }

  fun start(context: Context) {
    if (running) {
      return
    }

    loadPersistedConfigIfNeeded(context)

    val currentConfig =
      config ?: throw RelayException("BackgroundLocationRelay is not configured. Call initialize() first.")

    if (!AndroidPermissions.hasLocationPermission(context)) {
      throw RelayException("Location permission has not been granted.")
    }

    running = true
    startLocationUpdates(context.applicationContext, currentConfig)
    RelayLogger.info("Location relay started.")
  }

  fun stop(context: Context) {
    running = false
    lastDeliveryTimestamp = 0L
    stopLocationUpdates(context.applicationContext)
    RelayLogger.info("Location relay stopped.")
  }

  fun isRunning(): Boolean = running

  fun restartLocationUpdatesIfRunning(context: Context) {
    if (!running) {
      return
    }

    loadPersistedConfigIfNeeded(context)
    val currentConfig = config ?: return
    startLocationUpdates(context.applicationContext, currentConfig)
  }

  @SuppressLint("MissingPermission")
  fun startLocationUpdates(context: Context, currentConfig: BackgroundLocationRelayConfig) {
    stopLocationUpdates(context)

    val client = LocationServices.getFusedLocationProviderClient(context)
    val request = buildLocationRequest(currentConfig.location)

    val callback =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          val location = result.lastLocation ?: return
          handleLocationUpdate(currentConfig, location)
        }
      }

    locationCallback = callback
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
  }

  fun stopLocationUpdates(context: Context) {
    val callback = locationCallback ?: return
    LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(callback)
    locationCallback = null
  }

  private fun restartLocationUpdates(context: Context) {
    val currentConfig = config ?: return
    if (!running) {
      return
    }
    startLocationUpdates(context, currentConfig)
  }

  private fun handleLocationUpdate(
    currentConfig: BackgroundLocationRelayConfig,
    location: Location,
  ) {
    if (!running) {
      return
    }

    val intervalMs = currentConfig.location.interval.toLong()
    val now = System.currentTimeMillis()
    if (lastDeliveryTimestamp > 0L && now - lastDeliveryTimestamp < intervalMs) {
      return
    }

    lastDeliveryTimestamp = now
    deliver(currentConfig, location)
  }

  private fun deliver(currentConfig: BackgroundLocationRelayConfig, location: Location) {
    Thread {
        try {
          val body = GeoJsonBuilder.buildRequestBody(currentConfig.request.bodyTemplate, location)
          val result =
            HttpClient.post(
              endpoint = currentConfig.request.endpoint,
              headersJson = currentConfig.request.headersJson,
              timeoutMs = currentConfig.request.timeout,
              body = body,
            )

          if (result.success) {
            RelayLogger.debug(
              "Delivered location update (status ${result.statusCode ?: 0})."
            )
          } else {
            RelayLogger.error(
              "Failed to deliver location update: ${result.errorMessage ?: "unknown error"}"
            )
          }
        } catch (error: Exception) {
          RelayLogger.error("Failed to build request body: ${error.message}")
        }
      }
      .start()
  }

  private fun buildLocationRequest(locationConfig: LocationConfig): LocationRequest {
    val builder =
      LocationRequest.Builder(toPriority(locationConfig.accuracy), locationConfig.interval.toLong())

    locationConfig.fastestInterval?.let { builder.setMinUpdateIntervalMillis(it.toLong()) }
    locationConfig.distanceFilter?.let { builder.setMinUpdateDistanceMeters(it.toFloat()) }

    return builder.build()
  }

  private fun toPriority(accuracy: LocationAccuracy?): Int {
    return when (accuracy) {
      LocationAccuracy.LOWEST, LocationAccuracy.LOW -> Priority.PRIORITY_LOW_POWER
      LocationAccuracy.BALANCED -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
      LocationAccuracy.HIGH, LocationAccuracy.HIGHEST, null -> Priority.PRIORITY_HIGH_ACCURACY
    }
  }

  fun hasLocationPermission(context: Context): Boolean =
    AndroidPermissions.hasLocationPermission(context)
}
