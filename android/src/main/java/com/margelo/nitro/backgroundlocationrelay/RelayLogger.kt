package com.margelo.nitro.backgroundlocationrelay

import android.util.Log

object RelayLogger {
  private const val TAG = "BackgroundLocationRelay"
  private const val PREFIX = "⏫ BackgroundLocationRelay: "

  fun debug(message: String) {
    Log.d(TAG, "$PREFIX$message")
  }

  fun info(message: String) {
    Log.i(TAG, "$PREFIX$message")
  }

  fun error(message: String) {
    Log.e(TAG, "$PREFIX$message")
  }
}
