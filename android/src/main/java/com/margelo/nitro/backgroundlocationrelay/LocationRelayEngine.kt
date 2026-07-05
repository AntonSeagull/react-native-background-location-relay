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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object LocationRelayEngine {
  private const val MIN_INTERVAL_MS = 1_000L

  @Volatile
  private var config: BackgroundLocationRelayConfig? = null

  @Volatile
  private var running = false

  @Volatile
  private var latestLocation: Location? = null

  @Volatile
  private var hasDeliveredInitial = false

  private var locationCallback: LocationCallback? = null
  private var deliveryScheduler: ScheduledExecutorService? = null
  private var deliveryTask: ScheduledFuture<*>? = null

  fun setConfig(context: Context, newConfig: BackgroundLocationRelayConfig) {
    config = newConfig
    ConfigStore.save(context.applicationContext, newConfig)
    RelayLogger.info(ConfigLogger.format(newConfig))
    if (running) {
      RelayLogger.info("Relay is running — applying new configuration.")
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
      throw RelayException(
        "Location permission (ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION) has not been granted. Request it via react-native-permissions before start().",
      )
    }

    running = true
    startLocationUpdates(context.applicationContext, currentConfig)
    RelayLogger.info("Location relay started.")
  }

  fun stop(context: Context) {
    running = false
    latestLocation = null
    hasDeliveredInitial = false
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
          onLocationReceived(location)
        }
      }

    locationCallback = callback
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    startDeliveryTimer(currentConfig)
  }

  fun stopLocationUpdates(context: Context) {
    stopDeliveryTimer()

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

  private fun onLocationReceived(location: Location) {
    latestLocation = location

    if (!running) {
      return
    }

    // Deliver the very first fix immediately so consumers do not have to wait a
    // full interval for the initial location; the timer drives every delivery
    // afterwards, including while the device is stationary.
    if (!hasDeliveredInitial) {
      hasDeliveredInitial = true
      deliverLatest()
    }
  }

  private fun startDeliveryTimer(currentConfig: BackgroundLocationRelayConfig) {
    stopDeliveryTimer()

    val intervalMs = currentConfig.location.interval.toLong().coerceAtLeast(MIN_INTERVAL_MS)
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    deliveryScheduler = scheduler
    deliveryTask =
      scheduler.scheduleWithFixedDelay(
        {
          try {
            if (running) {
              deliverLatest()
            }
          } catch (error: Exception) {
            RelayLogger.error("Delivery timer error: ${error.message}")
          }
        },
        intervalMs,
        intervalMs,
        TimeUnit.MILLISECONDS,
      )
  }

  private fun stopDeliveryTimer() {
    deliveryTask?.cancel(false)
    deliveryTask = null
    deliveryScheduler?.shutdownNow()
    deliveryScheduler = null
  }

  private fun deliverLatest() {
    val currentConfig = config ?: return
    val location =
      latestLocation
        ?: run {
          RelayLogger.debug("Skipping delivery: no location fix yet")
          return
        }

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
