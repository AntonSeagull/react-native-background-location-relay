package com.margelo.nitro.backgroundlocationrelay

import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise

@DoNotStrip
class BackgroundLocationRelay : HybridBackgroundLocationRelaySpec() {
  override fun initialize(config: BackgroundLocationRelayConfig): Promise<Unit> {
    return Promise.async {
      val context = requireContext()
      LocationRelayEngine.setConfig(context, config)
    }
  }

  override fun start(): Promise<Unit> {
    return Promise.async {
      val context = requireContext()
      if (LocationRelayEngine.isRunning()) {
        return@async
      }
      if (!AndroidPermissions.hasLocationPermission(context)) {
        throw RelayException(
          "Location permission (ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION) has not been granted. Request it via react-native-permissions before start().",
        )
      }
      if (!AndroidPermissions.hasBackgroundLocationPermission(context)) {
        throw RelayException(
          "Background location permission (ACCESS_BACKGROUND_LOCATION) has not been granted. Request it via react-native-permissions before start().",
        )
      }
      if (!AndroidPermissions.hasNotificationPermission(context)) {
        throw RelayException(
          "Notification permission (POST_NOTIFICATIONS) has not been granted. Request it via react-native-permissions before start().",
        )
      }
      LocationRelayForegroundService.start(context)
    }
  }

  override fun stop(): Promise<Unit> {
    return Promise.async {
      val context = requireContext()
      LocationRelayForegroundService.stop(context)
    }
  }

  override fun isRunning(): Promise<Boolean> {
    return Promise.async {
      LocationRelayEngine.isRunning()
    }
  }

  override fun checkBatteryOptimization(): Promise<Boolean> {
    return Promise.async {
      AndroidBatteryOptimization.isIgnoring(requireContext())
    }
  }

  override fun requestBatteryOptimization(): Promise<Boolean> {
    return Promise.async {
      AndroidBatteryOptimization.requestExemption(requireContext())
    }
  }

  override fun openBatteryOptimizationSettings(): Promise<Unit> {
    return Promise.async {
      AndroidBatteryOptimization.openSettings(requireContext())
    }
  }

  override fun enableAutostartSettings(): Promise<Boolean> {
    return Promise.async {
      AndroidAutostartSettings.isAvailable(requireContext())
    }
  }

  override fun openAutostartSettings(): Promise<Boolean> {
    return Promise.async {
      AndroidAutostartSettings.openSettings(requireContext())
    }
  }

  private fun requireContext() =
    NitroModules.applicationContext
      ?: throw RelayException("React Native application context is not available.")
}
