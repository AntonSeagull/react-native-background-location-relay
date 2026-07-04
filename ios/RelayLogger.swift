import Foundation
import os.log

enum RelayLogger {
  private static let logger = Logger(
    subsystem: "com.margelo.nitro.backgroundlocationrelay",
    category: "BackgroundLocationRelay"
  )

  static func debug(_ message: String) {
    logger.debug("\(message, privacy: .public)")
  }

  static func info(_ message: String) {
    logger.info("\(message, privacy: .public)")
  }

  static func error(_ message: String) {
    logger.error("\(message, privacy: .public)")
  }
}
