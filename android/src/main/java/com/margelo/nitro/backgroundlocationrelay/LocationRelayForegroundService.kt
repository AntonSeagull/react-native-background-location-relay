package com.margelo.nitro.backgroundlocationrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LocationRelayForegroundService : Service() {
  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START, null -> startRelay()
      ACTION_STOP -> stopRelay()
    }

    return START_STICKY
  }

  private fun startRelay() {
    startForeground(NOTIFICATION_ID, buildNotification())
    try {
      LocationRelayEngine.start(applicationContext)
      ConfigStore.setWasRunning(applicationContext, true)
      LocationRelayScheduler.scheduleHeartbeat(applicationContext)
    } catch (error: Exception) {
      RelayLogger.error("Failed to start location relay: ${error.message}")
      ConfigStore.setWasRunning(applicationContext, false)
      LocationRelayScheduler.cancelAll(applicationContext)
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
  }

  private fun stopRelay() {
    ConfigStore.setWasRunning(applicationContext, false)
    LocationRelayScheduler.cancelAll(applicationContext)
    LocationRelayEngine.stop(applicationContext)
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    if (ConfigStore.wasRunning(applicationContext)) {
      RelayLogger.info("Task removed — scheduling location relay restart.")
      LocationRelayScheduler.scheduleRestart(applicationContext)
    }
  }

  override fun onDestroy() {
    LocationRelayEngine.stop(applicationContext)
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun buildNotification(): Notification {
    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setContentTitle(getString(R.string.location_relay_notification_title))
      .setContentText(getString(R.string.location_relay_notification_body))
      .setSmallIcon(android.R.drawable.ic_menu_mylocation)
      .setOngoing(true)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    val channel =
      NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        getString(R.string.location_relay_notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      )

    val manager = getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
  }

  companion object {
    private const val NOTIFICATION_CHANNEL_ID = "background_location_relay"
    private const val NOTIFICATION_ID = 1001

    const val ACTION_START = "com.margelo.nitro.backgroundlocationrelay.action.START"
    const val ACTION_STOP = "com.margelo.nitro.backgroundlocationrelay.action.STOP"

    fun start(context: Context) {
      if (LocationRelayEngine.isRunning()) {
        return
      }

      val intent =
        Intent(context, LocationRelayForegroundService::class.java).apply {
          action = ACTION_START
        }
      ContextCompatStartForegroundService.start(context, intent)
    }

    fun stop(context: Context) {
      val intent =
        Intent(context, LocationRelayForegroundService::class.java).apply {
          action = ACTION_STOP
        }
      context.startService(intent)
    }
  }
}

private object ContextCompatStartForegroundService {
  fun start(context: Context, intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }
}
