import Foundation

struct RelayDeliveryResult {
  let success: Bool
  let statusCode: Int?
  let errorMessage: String?
}

enum HttpClient {
  static func post(
    endpoint: String,
    headersJson: String?,
    timeoutMs: Double?,
    body: Data
  ) -> RelayDeliveryResult {
    let bodyString = String(data: body, encoding: .utf8) ?? "<binary body>"

    guard let url = URL(string: endpoint) else {
      RelayLogger.error("Invalid endpoint URL: \(endpoint)")
      return RelayDeliveryResult(
        success: false,
        statusCode: nil,
        errorMessage: "Invalid endpoint URL."
      )
    }

    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.httpBody = body
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    if let timeoutMs, timeoutMs > 0 {
      request.timeoutInterval = timeoutMs / 1000
    }

    if let headersJson, let headersData = headersJson.data(using: .utf8),
       let headersObject = try? JSONSerialization.jsonObject(with: headersData) as? [String: String]
    {
      for (key, value) in headersObject {
        request.setValue(value, forHTTPHeaderField: key)
      }
    }

    let requestHeaders = request.allHTTPHeaderFields ?? [:]
    RelayLogger.info("POST \(endpoint)")
    RelayLogger.info("Request headers: \(Self.formatHeaders(requestHeaders))")
    RelayLogger.info("Request body: \(bodyString)")

    let semaphore = DispatchSemaphore(value: 0)
    var result = RelayDeliveryResult(
      success: false,
      statusCode: nil,
      errorMessage: "Request did not complete."
    )

    let task = URLSession.shared.dataTask(with: request) { data, response, error in
      defer { semaphore.signal() }

      if let error {
        RelayLogger.error("Request failed: \(error.localizedDescription)")
        result = RelayDeliveryResult(
          success: false,
          statusCode: nil,
          errorMessage: error.localizedDescription
        )
        return
      }

      let statusCode = (response as? HTTPURLResponse)?.statusCode
      let responseBody = data.flatMap { String(data: $0, encoding: .utf8) } ?? "<empty>"
      let success = statusCode.map { (200 ... 299).contains($0) } ?? false

      if success {
        RelayLogger.info("Response status: \(statusCode ?? 0)")
        RelayLogger.info("Response body: \(responseBody)")
      } else {
        RelayLogger.error("Response status: \(statusCode ?? 0)")
        RelayLogger.error("Response body: \(responseBody)")
      }

      result = RelayDeliveryResult(
        success: success,
        statusCode: statusCode,
        errorMessage: success ? nil : "HTTP \(statusCode ?? 0)"
      )
    }

    task.resume()
    semaphore.wait()

    return result
  }

  private static func formatHeaders(_ headers: [String: String]) -> String {
    guard let data = try? JSONSerialization.data(withJSONObject: headers, options: [.sortedKeys]),
          let json = String(data: data, encoding: .utf8)
    else {
      return "{}"
    }
    return json
  }
}
