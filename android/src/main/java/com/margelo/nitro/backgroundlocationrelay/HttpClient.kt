package com.margelo.nitro.backgroundlocationrelay

import java.net.HttpURLConnection
import java.net.URL

data class RelayDeliveryResult(
  val success: Boolean,
  val statusCode: Int?,
  val errorMessage: String?,
)

object HttpClient {
  fun post(
    endpoint: String,
    headersJson: String?,
    timeoutMs: Double?,
    body: String,
  ): RelayDeliveryResult {
    return try {
      val connection = URL(endpoint).openConnection() as HttpURLConnection
      val timeout = timeoutMs?.toInt()?.takeIf { it > 0 } ?: 15_000

      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.connectTimeout = timeout
      connection.readTimeout = timeout
      connection.setRequestProperty("Content-Type", "application/json")

      if (!headersJson.isNullOrEmpty()) {
        val headers = org.json.JSONObject(headersJson)
        headers.keys().forEach { key ->
          connection.setRequestProperty(key, headers.getString(key))
        }
      }

      connection.outputStream.use { stream ->
        stream.write(body.toByteArray(Charsets.UTF_8))
      }

      val statusCode = connection.responseCode
      connection.disconnect()

      val success = statusCode in 200..299
      RelayDeliveryResult(
        success = success,
        statusCode = statusCode,
        errorMessage = if (success) null else "HTTP $statusCode",
      )
    } catch (error: Exception) {
      RelayDeliveryResult(
        success = false,
        statusCode = null,
        errorMessage = error.message ?: "Request failed.",
      )
    }
  }
}
