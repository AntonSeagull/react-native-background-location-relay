package com.margelo.nitro.backgroundlocationrelay

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object LocationRelayScheduler {
  const val ACTION_HEARTBEAT =
    "com.margelo.nitro.backgroundlocationrelay.action.HEARTBEAT"
  const val ACTION_RESTART =
    "com.margelo.nitro.backgroundlocationrelay.action.RESTART"

  private const val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L
  private const val RESTART_DELAY_MS = 2_000L
  private const val REQUEST_CODE_HEARTBEAT = 1002
  private const val REQUEST_CODE_RESTART = 1003

  fun scheduleHeartbeat(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val triggerAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL_MS
    val pendingIntent = heartbeatPendingIntent(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAt,
        pendingIntent,
      )
    } else {
      alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
  }

  fun scheduleRestart(context: Context) {
    if (!ConfigStore.wasRunning(context)) {
      return
    }

    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val triggerAt = System.currentTimeMillis() + RESTART_DELAY_MS
    val pendingIntent = restartPendingIntent(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAt,
        pendingIntent,
      )
    } else {
      alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
  }

  fun cancelAll(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    alarmManager.cancel(heartbeatPendingIntent(context))
    alarmManager.cancel(restartPendingIntent(context))
  }

  private fun heartbeatPendingIntent(context: Context): PendingIntent {
    val intent =
      Intent(context, LocationRelayBroadcastReceiver::class.java).apply {
        action = ACTION_HEARTBEAT
      }
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_HEARTBEAT,
      intent,
      pendingIntentFlags(),
    )
  }

  private fun restartPendingIntent(context: Context): PendingIntent {
    val intent =
      Intent(context, LocationRelayBroadcastReceiver::class.java).apply {
        action = ACTION_RESTART
      }
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_RESTART,
      intent,
      pendingIntentFlags(),
    )
  }

  private fun pendingIntentFlags(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
  }
}
