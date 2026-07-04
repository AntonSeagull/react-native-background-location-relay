import Foundation
import NitroModules

class BackgroundLocationRelay: HybridBackgroundLocationRelaySpec {
  func initialize(config: BackgroundLocationRelayConfig) throws -> Promise<Void> {
    return Promise.async {
      LocationRelayEngine.shared.setConfig(config)
    }
  }

  func start() throws -> Promise<Void> {
    return Promise.async {
      try LocationRelayEngine.shared.start()
    }
  }

  func stop() throws -> Promise<Void> {
    return Promise.async {
      LocationRelayEngine.shared.stop()
    }
  }

  func isRunning() throws -> Promise<Bool> {
    return Promise.async {
      LocationRelayEngine.shared.isRunning()
    }
  }

  func getAndroidSetupStatus() throws -> Promise<AndroidSetupStatus> {
    return Promise.async {
      AndroidSetupStatus(
        location: true,
        backgroundLocation: true,
        notifications: true,
        batteryOptimizationIgnored: true,
        manufacturer: nil,
        manufacturerSettingsAvailable: false
      )
    }
  }

  func requestIgnoreBatteryOptimizations() throws -> Promise<Bool> {
    return Promise.async {
      true
    }
  }

  func openBatteryOptimizationSettings() throws -> Promise<Void> {
    return Promise.async {
      // No-op on iOS.
    }
  }

  func openManufacturerSettings() throws -> Promise<Bool> {
    return Promise.async {
      false
    }
  }
}
