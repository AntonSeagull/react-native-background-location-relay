import Foundation
import os.log

enum RelayLogger {
  private static let prefix = "⏫ BackgroundLocationRelay: "
  private static let logger = Logger(
    subsystem: "com.margelo.nitro.backgroundlocationrelay",
    category: "BackgroundLocationRelay"
  )

  static func debug(_ message: String) {
    logger.debug("\(prefix)\(message, privacy: .public)")
  }

  static func info(_ message: String) {
    logger.info("\(prefix)\(message, privacy: .public)")
  }

  static func error(_ message: String) {
    logger.error("\(prefix)\(message, privacy: .public)")
  }
}
