package com.margelo.nitro.backgroundlocationrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager

class LocationRelayBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val appContext = context.applicationContext

    when (intent?.action) {
      Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(appContext)
      LocationRelayScheduler.ACTION_HEARTBEAT -> handleHeartbeat(appContext)
      LocationRelayScheduler.ACTION_RESTART -> handleRestart(appContext)
      LocationManager.PROVIDERS_CHANGED_ACTION -> handleProvidersChanged(appContext)
    }
  }

  private fun handleBootCompleted(context: Context) {
    if (!ConfigStore.wasRunning(context)) {
      return
    }

    if (ConfigStore.load(context) == null) {
      ConfigStore.setWasRunning(context, false)
      return
    }

    RelayLogger.info("Device booted — restarting location relay.")
    LocationRelayForegroundService.start(context)
  }

  private fun handleHeartbeat(context: Context) {
    if (!ConfigStore.wasRunning(context)) {
      LocationRelayScheduler.cancelAll(context)
      return
    }

    if (!LocationRelayEngine.isRunning()) {
      RelayLogger.info("Heartbeat: relay not running — restarting.")
      LocationRelayForegroundService.start(context)
    }

    LocationRelayScheduler.scheduleHeartbeat(context)
  }

  private fun handleRestart(context: Context) {
    if (!ConfigStore.wasRunning(context)) {
      return
    }

    RelayLogger.info("Scheduled restart — starting location relay.")
    LocationRelayForegroundService.start(context)
  }

  private fun handleProvidersChanged(context: Context) {
    if (!ConfigStore.wasRunning(context)) {
      return
    }

    val locationManager = context.getSystemService(LocationManager::class.java) ?: return
    val providersEnabled =
      locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!providersEnabled) {
      RelayLogger.error("Location providers disabled.")
      return
    }

    if (LocationRelayEngine.isRunning()) {
      RelayLogger.info("Location providers changed — restarting location updates.")
      LocationRelayEngine.restartLocationUpdatesIfRunning(context)
      return
    }

    RelayLogger.info("Location providers changed — restarting location relay.")
    LocationRelayForegroundService.start(context)
  }
}
