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
          "Location permission has not been granted. Request ACCESS_FINE_LOCATION before start().",
        )
      }
      if (!AndroidPermissions.hasBackgroundLocationPermission(context)) {
        throw RelayException(
          "Background location permission has not been granted. On Android 10+, request ACCESS_BACKGROUND_LOCATION in a separate step before start().",
        )
      }
      if (!AndroidPermissions.hasNotificationPermission(context)) {
        throw RelayException(
          "Notification permission has not been granted. On Android 13+, request POST_NOTIFICATIONS before start().",
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

  override fun getAndroidSetupStatus(): Promise<AndroidSetupStatus> {
    return Promise.async {
      buildAndroidSetupStatus(requireContext())
    }
  }

  override fun requestIgnoreBatteryOptimizations(): Promise<Boolean> {
    return Promise.async {
      AndroidBatteryOptimization.requestExemption(requireContext())
    }
  }

  override fun openBatteryOptimizationSettings(): Promise<Unit> {
    return Promise.async {
      AndroidBatteryOptimization.openSettings(requireContext())
    }
  }

  override fun openManufacturerSettings(): Promise<Boolean> {
    return Promise.async {
      AndroidManufacturerSettings.openSettings(requireContext())
    }
  }

  private fun buildAndroidSetupStatus(context: android.content.Context): AndroidSetupStatus {
    val manufacturer =
      AndroidManufacturerSettings.getManufacturerLabel()?.let {
        Variant_NullType_String.create(it)
      }

    return AndroidSetupStatus(
      location = AndroidPermissions.hasLocationPermission(context),
      backgroundLocation = AndroidPermissions.hasBackgroundLocationPermission(context),
      notifications = AndroidPermissions.hasNotificationPermission(context),
      batteryOptimizationIgnored = AndroidBatteryOptimization.isIgnoring(context),
      manufacturer = manufacturer,
      manufacturerSettingsAvailable = AndroidManufacturerSettings.isSettingsAvailable(context),
    )
  }

  private fun requireContext() =
    NitroModules.applicationContext
      ?: throw RelayException("React Native application context is not available.")
}
