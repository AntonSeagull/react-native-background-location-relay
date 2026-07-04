import CoreLocation
import Foundation

final class LocationRelayEngine: NSObject, CLLocationManagerDelegate {
  static let shared = LocationRelayEngine()

  private let locationManager = CLLocationManager()
  private let stateQueue = DispatchQueue(label: "BackgroundLocationRelay.engine")

  private var config: BackgroundLocationRelayConfig?
  private var running = false
  private var lastDeliveryTimestamp: TimeInterval = 0

  override private init() {
    super.init()
    locationManager.delegate = self
    locationManager.allowsBackgroundLocationUpdates = true
    locationManager.pausesLocationUpdatesAutomatically = true
    if #available(iOS 11.0, *) {
      locationManager.showsBackgroundLocationIndicator = false
    }
  }

  func setConfig(_ config: BackgroundLocationRelayConfig) {
    stateQueue.sync {
      self.config = config
      ConfigStore.save(config)
      applyLocationSettings(config.location)
    }
  }

  func loadPersistedConfigIfNeeded() {
    stateQueue.sync {
      if config == nil {
        config = ConfigStore.load()
      }
    }
  }

  func start() throws {
    if isRunning() {
      return
    }

    loadPersistedConfigIfNeeded()

    guard config != nil else {
      throw RelayError.configurationMissing
    }

    guard hasLocationPermission() else {
      throw RelayError.locationPermissionDenied
    }

    stateQueue.sync {
      running = true
    }

    DispatchQueue.main.async {
      self.locationManager.startUpdatingLocation()
    }

    RelayLogger.info("Location relay started.")
  }

  func stop() {
    stateQueue.sync {
      running = false
      lastDeliveryTimestamp = 0
    }

    DispatchQueue.main.async {
      self.locationManager.stopUpdatingLocation()
    }

    RelayLogger.info("Location relay stopped.")
  }

  func isRunning() -> Bool {
    stateQueue.sync { running }
  }

  func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location = locations.last else {
      return
    }

    let shouldDeliver = stateQueue.sync { () -> (Bool, BackgroundLocationRelayConfig?) in
      guard running, let currentConfig = config else {
        return (false, nil)
      }

      let intervalMs = currentConfig.location.interval
      let now = Date().timeIntervalSince1970 * 1000
      if lastDeliveryTimestamp > 0, now - lastDeliveryTimestamp < intervalMs {
        return (false, nil)
      }

      lastDeliveryTimestamp = now
      return (true, currentConfig)
    }

    guard shouldDeliver.0, let currentConfig = shouldDeliver.1 else {
      return
    }

    deliver(location: location, config: currentConfig)
  }

  func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    RelayLogger.error("Location manager failed: \(error.localizedDescription)")
  }

  private func deliver(location: CLLocation, config: BackgroundLocationRelayConfig) {
    DispatchQueue.global(qos: .utility).async {
      do {
        let body = try GeoJsonBuilder.buildRequestBody(
          bodyTemplate: config.request.bodyTemplate,
          location: location
        )

        let result = HttpClient.post(
          endpoint: config.request.endpoint,
          headersJson: config.request.headersJson,
          timeoutMs: config.request.timeout,
          body: body
        )

        if result.success {
          RelayLogger.debug(
            "Delivered location update (status \(result.statusCode ?? 0))."
          )
        } else {
          RelayLogger.error(
            "Failed to deliver location update: \(result.errorMessage ?? "unknown error")"
          )
        }
      } catch {
        RelayLogger.error("Failed to build request body: \(error.localizedDescription)")
      }
    }
  }

  private func applyLocationSettings(_ locationConfig: LocationConfig) {
    DispatchQueue.main.async {
      self.locationManager.desiredAccuracy = Self.desiredAccuracy(for: locationConfig.accuracy)
      self.locationManager.distanceFilter = locationConfig.distanceFilter ?? kCLDistanceFilterNone

      if let pauses = locationConfig.pausesLocationUpdatesAutomatically {
        self.locationManager.pausesLocationUpdatesAutomatically = pauses
      }

      if #available(iOS 11.0, *) {
        if let showsIndicator = locationConfig.showsBackgroundLocationIndicator {
          self.locationManager.showsBackgroundLocationIndicator = showsIndicator
        }
      }

      if Self.hasAlwaysPermission() {
        self.locationManager.allowsBackgroundLocationUpdates = true
      }
    }
  }

  private func hasLocationPermission() -> Bool {
    switch locationManager.authorizationStatus {
    case .authorizedAlways, .authorizedWhenInUse:
      return true
    default:
      return false
    }
  }

  private static func hasAlwaysPermission() -> Bool {
    CLLocationManager.authorizationStatus() == .authorizedAlways
  }

  private static func desiredAccuracy(for accuracy: LocationAccuracy?) -> CLLocationAccuracy {
    switch accuracy {
    case .some(.lowest):
      return kCLLocationAccuracyThreeKilometers
    case .some(.low):
      return kCLLocationAccuracyKilometer
    case .some(.balanced):
      return kCLLocationAccuracyHundredMeters
    case .some(.high):
      return kCLLocationAccuracyNearestTenMeters
    case .some(.highest):
      return kCLLocationAccuracyBest
    case .none:
      return kCLLocationAccuracyBest
    }
  }
}
