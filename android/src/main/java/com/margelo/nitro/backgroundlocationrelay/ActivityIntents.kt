package com.margelo.nitro.backgroundlocationrelay

import android.content.Context
import android.content.Intent
import android.os.Build

object ActivityIntents {
  fun start(context: Context, intent: Intent): Boolean {
    return try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    } catch (error: Exception) {
      RelayLogger.error("Failed to start activity: ${error.message}")
      false
    }
  }
}
