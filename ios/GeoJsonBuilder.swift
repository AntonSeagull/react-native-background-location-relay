import CoreLocation
import Foundation

enum GeoJsonBuilder {
  private static let placeholder = "$GEO$"

  static func geoDictionary(from location: CLLocation) -> [String: Any?] {
    [
      "latitude": location.coordinate.latitude,
      "longitude": location.coordinate.longitude,
      "accuracy": validAccuracy(location.horizontalAccuracy),
      "altitude": validAltitude(location.altitude),
      "altitudeAccuracy": validAccuracy(location.verticalAccuracy),
      "speed": validSpeed(location.speed),
      "speedAccuracy": validSpeedAccuracy(location),
      "heading": validHeading(location.course),
      "headingAccuracy": validHeadingAccuracy(location),
      "timestamp": Int64(location.timestamp.timeIntervalSince1970 * 1000),
    ]
  }

  static func buildRequestBody(
    bodyTemplate: String,
    location: CLLocation
  ) throws -> Data {
    guard let templateData = bodyTemplate.data(using: .utf8) else {
      throw RelayError.invalidBodyTemplate
    }

    let jsonObject = try JSONSerialization.jsonObject(with: templateData)
    let geo = geoDictionary(from: location)
    let injected = injectGeo(into: jsonObject, geo: geo)

    return try JSONSerialization.data(withJSONObject: injected)
  }

  private static func injectGeo(into value: Any, geo: [String: Any?]) -> Any {
    if let stringValue = value as? String, stringValue == placeholder {
      return geo.compactMapValues { $0 }
    }

    if var dictionary = value as? [String: Any] {
      for (key, nestedValue) in dictionary {
        dictionary[key] = injectGeo(into: nestedValue, geo: geo)
      }
      return dictionary
    }

    if let array = value as? [Any] {
      return array.map { injectGeo(into: $0, geo: geo) }
    }

    return value
  }

  private static func validAccuracy(_ value: CLLocationAccuracy) -> Double? {
    value >= 0 ? value : nil
  }

  private static func validAltitude(_ value: CLLocationDistance) -> Double? {
    value.isFinite ? value : nil
  }

  private static func validSpeed(_ value: CLLocationSpeed) -> Double? {
    value >= 0 ? value : nil
  }

  private static func validSpeedAccuracy(_ location: CLLocation) -> Double? {
    guard location.speedAccuracy >= 0 else {
      return nil
    }
    return location.speedAccuracy
  }

  private static func validHeading(_ value: CLLocationDirection) -> Double? {
    value >= 0 ? value : nil
  }

  private static func validHeadingAccuracy(_ location: CLLocation) -> Double? {
    guard location.courseAccuracy >= 0 else {
      return nil
    }
    return location.courseAccuracy
  }
}

enum RelayError: LocalizedError {
  case configurationMissing
  case locationPermissionDenied
  case invalidBodyTemplate
  case invalidEndpoint

  var errorDescription: String? {
    switch self {
    case .configurationMissing:
      return "BackgroundLocationRelay is not configured. Call initialize() first."
    case .locationPermissionDenied:
      return "Location permission has not been granted."
    case .invalidBodyTemplate:
      return "request.bodyTemplate must be valid JSON."
    case .invalidEndpoint:
      return "request.endpoint must be a valid URL."
    }
  }
}
