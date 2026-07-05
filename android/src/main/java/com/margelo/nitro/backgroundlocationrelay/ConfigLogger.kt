package com.margelo.nitro.backgroundlocationrelay

object ConfigLogger {
  fun format(config: BackgroundLocationRelayConfig): String {
    val location = config.location
    val request = config.request

    return buildString {
      appendLine("Initialized with configuration:")
      appendLine("  location.interval: ${location.interval} ms")
      appendLine(
        "  location.fastestInterval: ${location.fastestInterval?.let { "$it ms" } ?: "(default)"}",
      )
      appendLine(
        "  location.distanceFilter: ${location.distanceFilter?.let { "$it m" } ?: "(none)"}",
      )
      appendLine(
        "  location.accuracy: ${location.accuracy?.name?.lowercase() ?: "(default)"}",
      )
      location.pausesLocationUpdatesAutomatically?.let {
        appendLine("  location.pausesLocationUpdatesAutomatically: $it")
      }
      location.showsBackgroundLocationIndicator?.let {
        appendLine("  location.showsBackgroundLocationIndicator: $it")
      }
      appendLine("  request.endpoint: ${request.endpoint}")
      appendLine(
        "  request.headers: ${request.headersJson?.takeIf { it.isNotEmpty() } ?: "(none)"}",
      )
      appendLine(
        "  request.timeout: ${request.timeout?.let { "$it ms" } ?: "(default)"}",
      )
      append("  request.bodyTemplate: ${request.bodyTemplate}")
    }
  }
}
