package app.sole.tracker.ai

import app.sole.tracker.data.PrefsManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ActivityEstimate(
    val steps: Int,
    val distanceKm: Double,
    val kcal: Int,
    val activeMinutes: Int
)

object GeminiActivityEstimator {

    fun estimate(
        activity: String,
        durationMinutes: Int,
        prefs: PrefsManager
    ): Result<ActivityEstimate> {
        val apiKey = prefs.googleApiKey.trim().replace("\n", "").replace("\r", "")
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Missing API Key in Settings"))
        }

        return try {
            val bodyContext = if (prefs.useBodyMetrics) {
                "User height: ${prefs.heightCm} cm, weight: ${prefs.weightKg} kg."
            } else {
                "Assume average adult build (70 kg, 175 cm)."
            }

            val prompt = """
                Estimate realistic fitness metrics for this logged physical activity:
                Activity: "$activity"
                Duration: $durationMinutes minutes
                $bodyContext
                Return ONLY a valid JSON object with these exact keys and numeric values (no markdown, no extra text):
                {"steps": integer_equivalent_steps, "distance_km": number, "kcal": integer, "active_minutes": $durationMinutes}
            """.trimIndent()

            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 12000
                readTimeout = 15000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("model", "openai/gpt-oss-20b")
                put("temperature", 0.2)
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    )
                )
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

            if (code !in 200..299) {
                val errMsg = try {
                    JSONObject(responseText).getJSONObject("error").optString("message", responseText)
                } catch (_: Exception) {
                    responseText
                }
                return Result.failure(RuntimeException("API error ($code): $errMsg"))
            }

            val root = JSONObject(responseText)
            val rawText = root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val jsonStart = rawText.indexOf('{')
            val jsonEnd = rawText.lastIndexOf('}')
            val cleanJson = if (jsonStart != -1 && jsonEnd > jsonStart) {
                rawText.substring(jsonStart, jsonEnd + 1)
            } else {
                rawText
            }

            val parsed = JSONObject(cleanJson)
            Result.success(
                ActivityEstimate(
                    steps = parsed.optInt("steps", 0).coerceAtLeast(0),
                    distanceKm = parsed.optDouble("distance_km", 0.0).coerceAtLeast(0.0),
                    kcal = parsed.optInt("kcal", 0).coerceAtLeast(0),
                    activeMinutes = parsed.optInt("active_minutes", durationMinutes).coerceAtLeast(1)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
