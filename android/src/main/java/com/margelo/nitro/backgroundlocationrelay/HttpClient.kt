package com.margelo.nitro.backgroundlocationrelay

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

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
        val headers = JSONObject(headersJson)
        headers.keys().forEach { key ->
          connection.setRequestProperty(key, headers.getString(key))
        }
      }

      val requestHeaders = connection.requestProperties
      RelayLogger.info("POST $endpoint")
      RelayLogger.info("Request headers: ${formatHeaders(requestHeaders)}")
      RelayLogger.info("Request body: $body")

      connection.outputStream.use { stream ->
        stream.write(body.toByteArray(Charsets.UTF_8))
      }

      val statusCode = connection.responseCode
      val responseBody = readResponseBody(connection)
      connection.disconnect()

      val success = statusCode in 200..299
      if (success) {
        RelayLogger.info("Response status: $statusCode")
        RelayLogger.info("Response body: $responseBody")
      } else {
        RelayLogger.error("Response status: $statusCode")
        RelayLogger.error("Response body: $responseBody")
      }

      RelayDeliveryResult(
        success = success,
        statusCode = statusCode,
        errorMessage = if (success) null else "HTTP $statusCode",
      )
    } catch (error: Exception) {
      RelayLogger.error("Request failed: ${error.message ?: "unknown error"}")
      RelayDeliveryResult(
        success = false,
        statusCode = null,
        errorMessage = error.message ?: "Request failed.",
      )
    }
  }

  private fun readResponseBody(connection: HttpURLConnection): String {
    val stream =
      if (connection.responseCode in 200..299) {
        connection.inputStream
      } else {
        connection.errorStream
      }

    return stream?.bufferedReader()?.use { it.readText() }?.ifEmpty { "<empty>" } ?: "<empty>"
  }

  private fun formatHeaders(headers: Map<String, List<String>>): String {
    val json = JSONObject()
    headers.forEach { (key, values) ->
      if (key.isNotEmpty() && values.isNotEmpty()) {
        json.put(key, values.joinToString(", "))
      }
    }
    return json.toString()
  }
}
