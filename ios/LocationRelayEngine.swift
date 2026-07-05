import CoreLocation
import Foundation

final class LocationRelayEngine: NSObject, CLLocationManagerDelegate {
  static let shared = LocationRelayEngine()

  private static let minIntervalMs: Double = 1_000

  private let locationManager = CLLocationManager()
  private let stateQueue = DispatchQueue(label: "BackgroundLocationRelay.engine")
  private let deliveryQueue = DispatchQueue(label: "BackgroundLocationRelay.delivery")

  private var config: BackgroundLocationRelayConfig?
  private var running = false
  private var latestLocation: CLLocation?
  private var hasDeliveredInitial = false
  private var deliveryTimer: DispatchSourceTimer?

  override private init() {
    super.init()
    locationManager.delegate = self
    locationManager.allowsBackgroundLocationUpdates = false
    locationManager.pausesLocationUpdatesAutomatically = false
    if #available(iOS 11.0, *) {
      locationManager.showsBackgroundLocationIndicator = false
    }
  }

  func setConfig(_ config: BackgroundLocationRelayConfig) {
    stateQueue.sync {
      self.config = config
      ConfigStore.save(config)
    }

    runOnMain {
      self.applyLocationSettings(config.location)
    }

    RelayLogger.info(ConfigLogger.format(config))

    if isRunning() {
      RelayLogger.info("Relay is running — applying new configuration.")
      startDeliveryTimer()
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

    guard let currentConfig = stateQueue.sync(execute: { config }) else {
      throw RelayError.configurationMissing
    }

    switch locationManager.authorizationStatus {
    case .authorizedAlways:
      break
    case .authorizedWhenInUse:
      RelayLogger.error(
        "Cannot start: background location permission (Always) is required, current status is \(Self.describeAuthorizationStatus(locationManager.authorizationStatus))."
      )
      throw RelayError.backgroundLocationPermissionMissing
    default:
      RelayLogger.error(
        "Cannot start: location permission is \(Self.describeAuthorizationStatus(locationManager.authorizationStatus))."
      )
      throw RelayError.locationPermissionMissing
    }

    stateQueue.sync {
      running = true
      latestLocation = nil
      hasDeliveredInitial = false
    }

    ConfigStore.setWasRunning(true)

    runOnMainSync {
      self.applyLocationSettings(currentConfig.location)
      self.locationManager.startUpdatingLocation()
      // Significant-location-change monitoring keeps the relay alive in the
      // background and lets iOS relaunch the app after it has been terminated.
      if Self.hasAlwaysPermission(for: self.locationManager) {
        self.locationManager.startMonitoringSignificantLocationChanges()
      }
      self.locationManager.requestLocation()
    }

    startDeliveryTimer()

    RelayLogger.info("Location relay started.")
    RelayLogger.info(
      "Location authorization: \(Self.describeAuthorizationStatus(locationManager.authorizationStatus))."
    )
    RelayLogger.info(
      "Location manager settings: pausesAutomatically=\(locationManager.pausesLocationUpdatesAutomatically), distanceFilter=\(locationManager.distanceFilter), allowsBackgroundUpdates=\(locationManager.allowsBackgroundLocationUpdates)."
    )
  }

  func stop() {
    stateQueue.sync {
      running = false
      latestLocation = nil
      hasDeliveredInitial = false
    }

    ConfigStore.setWasRunning(false)
    stopDeliveryTimer()

    runOnMain {
      self.locationManager.stopUpdatingLocation()
      self.locationManager.stopMonitoringSignificantLocationChanges()
    }

    RelayLogger.info("Location relay stopped.")
  }

  func isRunning() -> Bool {
    stateQueue.sync { running }
  }

  /// Resumes tracking after the app has been relaunched by the system (for
  /// example, following a significant-location-change event) without any
  /// JavaScript having run yet.
  @objc func resumeIfNeeded() {
    guard !isRunning() else {
      return
    }

    guard ConfigStore.wasRunning() else {
      return
    }

    loadPersistedConfigIfNeeded()

    guard stateQueue.sync(execute: { config }) != nil else {
      ConfigStore.setWasRunning(false)
      return
    }

    guard Self.hasAlwaysPermission(for: locationManager) else {
      return
    }

    RelayLogger.info("Relaunched by the system — resuming location relay.")
    try? start()
  }

  func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
    RelayLogger.info(
      "Location authorization changed: \(Self.describeAuthorizationStatus(manager.authorizationStatus))."
    )

    runOnMain {
      if Self.hasAlwaysPermission(for: manager) {
        manager.allowsBackgroundLocationUpdates = true
      } else {
        manager.allowsBackgroundLocationUpdates = false
      }
    }

    guard isRunning(), hasLocationPermission() else {
      return
    }

    runOnMain {
      manager.startUpdatingLocation()
      if Self.hasAlwaysPermission(for: manager) {
        manager.startMonitoringSignificantLocationChanges()
      }
    }
  }

  func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location = locations.last else {
      RelayLogger.info("Skipping delivery: location update did not contain coordinates.")
      return
    }

    RelayLogger.info(
      String(
        format: "Location update received (%.6f, %.6f, accuracy %.1f m).",
        location.coordinate.latitude,
        location.coordinate.longitude,
        location.horizontalAccuracy
      )
    )

    let shouldDeliverInitial = stateQueue.sync { () -> Bool in
      latestLocation = location
      guard running else {
        return false
      }
      if !hasDeliveredInitial {
        hasDeliveredInitial = true
        return true
      }
      return false
    }

    // Deliver the very first fix immediately so consumers do not have to wait a
    // full interval for the initial location; the timer drives every delivery
    // afterwards, including while the device is stationary.
    if shouldDeliverInitial {
      deliverLatest()
    }
  }

  func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    RelayLogger.error("Location manager failed: \(error.localizedDescription)")
  }

  private func startDeliveryTimer() {
    guard let currentConfig = stateQueue.sync(execute: { config }) else {
      return
    }

    let intervalMs = max(currentConfig.location.interval, Self.minIntervalMs)
    let interval = intervalMs / 1000

    stopDeliveryTimer()

    let timer = DispatchSource.makeTimerSource(queue: deliveryQueue)
    timer.schedule(deadline: .now() + interval, repeating: interval)
    timer.setEventHandler { [weak self] in
      guard let self = self, self.isRunning() else {
        return
      }
      self.deliverLatest()
    }
    deliveryTimer = timer
    timer.resume()
  }

  private func stopDeliveryTimer() {
    deliveryTimer?.cancel()
    deliveryTimer = nil
  }

  private func deliverLatest() {
    let snapshot = stateQueue.sync { () -> (CLLocation?, BackgroundLocationRelayConfig?) in
      (latestLocation, config)
    }

    guard let currentConfig = snapshot.1 else {
      return
    }

    guard let location = snapshot.0 else {
      RelayLogger.info("Skipping delivery: no location fix yet.")
      return
    }

    deliver(location: location, config: currentConfig)
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
          RelayLogger.info(
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
    locationManager.desiredAccuracy = Self.desiredAccuracy(for: locationConfig.accuracy)
    locationManager.distanceFilter = locationConfig.distanceFilter ?? kCLDistanceFilterNone
    locationManager.pausesLocationUpdatesAutomatically =
      locationConfig.pausesLocationUpdatesAutomatically ?? false

    if #available(iOS 11.0, *) {
      if let showsIndicator = locationConfig.showsBackgroundLocationIndicator {
        locationManager.showsBackgroundLocationIndicator = showsIndicator
      }
    }

    locationManager.allowsBackgroundLocationUpdates =
      Self.hasAlwaysPermission(for: locationManager)
  }

  private func hasLocationPermission() -> Bool {
    switch locationManager.authorizationStatus {
    case .authorizedAlways, .authorizedWhenInUse:
      return true
    default:
      return false
    }
  }

  private static func hasAlwaysPermission(for manager: CLLocationManager) -> Bool {
    manager.authorizationStatus == .authorizedAlways
  }

  private static func describeAuthorizationStatus(_ status: CLAuthorizationStatus) -> String {
    switch status {
    case .notDetermined:
      return "notDetermined"
    case .restricted:
      return "restricted"
    case .denied:
      return "denied"
    case .authorizedAlways:
      return "authorizedAlways"
    case .authorizedWhenInUse:
      return "authorizedWhenInUse"
    @unknown default:
      return "unknown"
    }
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

  private func runOnMain(_ block: @escaping () -> Void) {
    if Thread.isMainThread {
      block()
    } else {
      DispatchQueue.main.async(execute: block)
    }
  }

  private func runOnMainSync(_ block: () -> Void) {
    if Thread.isMainThread {
      block()
    } else {
      DispatchQueue.main.sync(execute: block)
    }
  }
}

@objc(RelayLaunchResumer)
final class RelayLaunchResumer: NSObject {
  @objc static func resume() {
    DispatchQueue.main.async {
      LocationRelayEngine.shared.resumeIfNeeded()
    }
  }
}
