package com.margelo.nitro.backgroundlocationrelay

import android.util.Log

object RelayLogger {
  private const val TAG = "BackgroundLocationRelay"

  fun debug(message: String) {
    Log.d(TAG, message)
  }

  fun info(message: String) {
    Log.i(TAG, message)
  }

  fun error(message: String) {
    Log.e(TAG, message)
  }
}
