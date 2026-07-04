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
    guard let url = URL(string: endpoint) else {
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

    let semaphore = DispatchSemaphore(value: 0)
    var result = RelayDeliveryResult(
      success: false,
      statusCode: nil,
      errorMessage: "Request did not complete."
    )

    let task = URLSession.shared.dataTask(with: request) { _, response, error in
      defer { semaphore.signal() }

      if let error {
        result = RelayDeliveryResult(
          success: false,
          statusCode: nil,
          errorMessage: error.localizedDescription
        )
        return
      }

      let statusCode = (response as? HTTPURLResponse)?.statusCode
      let success = statusCode.map { (200 ... 299).contains($0) } ?? false
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
}
