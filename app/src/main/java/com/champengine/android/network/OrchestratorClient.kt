package com.champengine.android.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class Job(
    val jobId: String,
    val projectType: String,
    val description: String,
    val status: String,
    val logs: List<String>,
    val deliverable: String?,
    val createdAt: Double,
    val complexityScore: String,
    val priceDisplay: String
)

class OrchestratorClient(
    private val baseUrl: String,
    private val authToken: String
) {
    suspend fun createProject(description: String, autonomy: String = "FULL_AUTO"): Result<Job> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/projects")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-Champ-Token", authToken)
                conn.doOutput = true
                conn.connectTimeout = 30000
                conn.readTimeout = 60000
                val body = JSONObject().apply {
                    put("description", description)
                    put("autonomy", autonomy)
                }.toString()
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                Result.success(parseJob(JSONObject(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getProject(jobId: String): Result<Job> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/projects/$jobId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("X-Champ-Token", authToken)
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                Result.success(parseJob(JSONObject(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listProjects(): Result<List<Job>> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/projects")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("X-Champ-Token", authToken)
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val arr = JSONObject(response).getJSONArray("jobs")
                val list = mutableListOf<Job>()
                for (i in 0 until arr.length()) list.add(parseJob(arr.getJSONObject(i)))
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseJob(j: JSONObject): Job {
        val logs = mutableListOf<String>()
        val logsArr = j.optJSONArray("logs") ?: JSONArray()
        for (i in 0 until logsArr.length()) logs.add(logsArr.getString(i))
        val complexity = j.optJSONObject("complexity")
        return Job(
            jobId = j.optString("job_id"),
            projectType = j.optString("project_type"),
            description = j.optString("description"),
            status = j.optString("status"),
            logs = logs,
            deliverable = j.optString("deliverable").takeIf { it != "null" && it.isNotEmpty() },
            createdAt = j.optDouble("created_at"),
            complexityScore = j.optInt("complexity_score", 0).toString(),
            priceDisplay = complexity?.optString("price_standard_display") ?: "?"
        )
    }
}
