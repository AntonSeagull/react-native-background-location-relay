package com.margelo.nitro.backgroundlocationrelay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object AndroidBatteryOptimization {
  fun isIgnoring(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true
    }

    val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun requestExemption(context: Context): Boolean {
    if (isIgnoring(context)) {
      return true
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true
    }

    return try {
      val intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
          data = Uri.parse("package:${context.packageName}")
        }
      ActivityIntents.start(context, intent)
      false
    } catch (error: Exception) {
      RelayLogger.error(
        "Failed to request battery optimization exemption: ${error.message}",
      )
      openSettings(context)
      false
    }
  }

  fun openSettings(context: Context) {
    try {
      ActivityIntents.start(
        context,
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
      )
    } catch (error: Exception) {
      RelayLogger.error("Failed to open battery optimization settings: ${error.message}")
      ActivityIntents.start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", context.packageName, null)
        },
      )
    }
  }
}
