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

  func checkBatteryOptimization() throws -> Promise<Bool> {
    return Promise.async {
      // Battery optimization is an Android-only concept.
      true
    }
  }

  func requestBatteryOptimization() throws -> Promise<Bool> {
    return Promise.async {
      true
    }
  }

  func openBatteryOptimizationSettings() throws -> Promise<Void> {
    return Promise.async {
      // No-op on iOS.
    }
  }

  func enableAutostartSettings() throws -> Promise<Bool> {
    return Promise.async {
      false
    }
  }

  func openAutostartSettings() throws -> Promise<Bool> {
    return Promise.async {
      false
    }
  }
}
