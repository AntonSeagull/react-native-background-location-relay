import Foundation

enum ConfigLogger {
  static func format(_ config: BackgroundLocationRelayConfig) -> String {
    let location = config.location
    let request = config.request

    var lines = ["Initialized with configuration:"]
    lines.append("  location.interval: \(location.interval) ms")

    if let fastestInterval = location.fastestInterval {
      lines.append("  location.fastestInterval: \(fastestInterval) ms")
    } else {
      lines.append("  location.fastestInterval: (default)")
    }

    if let distanceFilter = location.distanceFilter {
      lines.append("  location.distanceFilter: \(distanceFilter) m")
    } else {
      lines.append("  location.distanceFilter: (none)")
    }

    if let accuracy = location.accuracy {
      lines.append("  location.accuracy: \(accuracy.stringValue)")
    } else {
      lines.append("  location.accuracy: (default)")
    }

    if let pauses = location.pausesLocationUpdatesAutomatically {
      lines.append("  location.pausesLocationUpdatesAutomatically: \(pauses)")
    }

    if let showsIndicator = location.showsBackgroundLocationIndicator {
      lines.append("  location.showsBackgroundLocationIndicator: \(showsIndicator)")
    }

    lines.append("  request.endpoint: \(request.endpoint)")

    if let headersJson = request.headersJson, !headersJson.isEmpty {
      lines.append("  request.headers: \(headersJson)")
    } else {
      lines.append("  request.headers: (none)")
    }

    if let timeout = request.timeout {
      lines.append("  request.timeout: \(timeout) ms")
    } else {
      lines.append("  request.timeout: (default)")
    }

    lines.append("  request.bodyTemplate: \(request.bodyTemplate)")

    return lines.joined(separator: "\n")
  }
}
