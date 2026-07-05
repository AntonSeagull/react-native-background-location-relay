import Foundation

enum ConfigStore {
  private static let configKey = "BackgroundLocationRelay.config"
  private static let wasRunningKey = "BackgroundLocationRelay.wasRunning"

  static func setWasRunning(_ running: Bool) {
    UserDefaults.standard.set(running, forKey: wasRunningKey)
  }

  static func wasRunning() -> Bool {
    UserDefaults.standard.bool(forKey: wasRunningKey)
  }

  static func save(_ config: BackgroundLocationRelayConfig) {
    let payload: [String: Any] = [
      "location": locationDictionary(config.location),
      "request": requestDictionary(config.request),
    ]

    UserDefaults.standard.set(payload, forKey: configKey)
  }

  static func load() -> BackgroundLocationRelayConfig? {
    guard let payload = UserDefaults.standard.dictionary(forKey: configKey) else {
      return nil
    }

    guard
      let locationPayload = payload["location"] as? [String: Any],
      let requestPayload = payload["request"] as? [String: Any]
    else {
      return nil
    }

    return BackgroundLocationRelayConfig(
      location: locationConfig(from: locationPayload),
      request: requestConfig(from: requestPayload)
    )
  }

  private static func locationDictionary(_ location: LocationConfig) -> [String: Any] {
    var payload: [String: Any] = ["interval": location.interval]

    if let fastestInterval = location.fastestInterval {
      payload["fastestInterval"] = fastestInterval
    }
    if let distanceFilter = location.distanceFilter {
      payload["distanceFilter"] = distanceFilter
    }
    if let accuracy = location.accuracy {
      payload["accuracy"] = accuracy.stringValue
    }
    if let pauses = location.pausesLocationUpdatesAutomatically {
      payload["pausesLocationUpdatesAutomatically"] = pauses
    }
    if let showsIndicator = location.showsBackgroundLocationIndicator {
      payload["showsBackgroundLocationIndicator"] = showsIndicator
    }

    return payload
  }

  private static func requestDictionary(_ request: RequestConfig) -> [String: Any] {
    var payload: [String: Any] = [
      "endpoint": request.endpoint,
      "bodyTemplate": request.bodyTemplate,
    ]

    if let headersJson = request.headersJson {
      payload["headersJson"] = headersJson
    }
    if let timeout = request.timeout {
      payload["timeout"] = timeout
    }

    return payload
  }

  private static func locationConfig(from payload: [String: Any]) -> LocationConfig {
    LocationConfig(
      interval: payload["interval"] as? Double ?? 60_000,
      fastestInterval: payload["fastestInterval"] as? Double,
      distanceFilter: payload["distanceFilter"] as? Double,
      accuracy: (payload["accuracy"] as? String).flatMap(LocationAccuracy.init),
      pausesLocationUpdatesAutomatically:
        payload["pausesLocationUpdatesAutomatically"] as? Bool,
      showsBackgroundLocationIndicator:
        payload["showsBackgroundLocationIndicator"] as? Bool
    )
  }

  private static func requestConfig(from payload: [String: Any]) -> RequestConfig {
    RequestConfig(
      endpoint: payload["endpoint"] as? String ?? "",
      headersJson: payload["headersJson"] as? String,
      timeout: payload["timeout"] as? Double,
      bodyTemplate: payload["bodyTemplate"] as? String ?? ""
    )
  }
}
