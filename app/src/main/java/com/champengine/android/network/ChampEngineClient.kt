package com.champengine.android.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChampEngineClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "ChampEngineClient"

    // ── Baked-in defaults — users never need to configure anything ──
    private val DEFAULT_ENDPOINT = "https://api.champengine.cloud"
    private val DEFAULT_TOKEN    = "eb0845f1d07fa481e941e4733b90de348e17ef95afa60b7c4f524905bbe1fd2a"
    private val DEFAULT_MODEL    = "llama3.2:3b"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("champengine_config", Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Config — users can override, but defaults work out of box ──
    fun getEndpoint(): String = prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
    fun getToken(): String    = prefs.getString("token", DEFAULT_TOKEN) ?: DEFAULT_TOKEN
    fun getModel(): String    = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun saveCustomConfig(endpoint: String, token: String, model: String) {
        prefs.edit()
            .putString("endpoint", endpoint)
            .putString("token", token)
            .putString("model", model)
            .apply()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    fun isUsingDefaults(): Boolean =
        getEndpoint() == DEFAULT_ENDPOINT && getToken() == DEFAULT_TOKEN

    // ── Health check ──────────────────────────────────────────────
    suspend fun ping(): Boolean {
        return try {
            val req = Request.Builder()
                .url("${getEndpoint()}/api/tags")
                .addHeader("X-Champ-Token", getToken())
                .get().build()
            val resp = http.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Ping failed: ${e.message}")
            false
        }
    }

    // ── List available models ─────────────────────────────────────
    suspend fun listModels(): List<String> {
        return try {
            val req = Request.Builder()
                .url("${getEndpoint()}/api/tags")
                .addHeader("X-Champ-Token", getToken())
                .get().build()
            val resp = http.newCall(req).execute()
            val body = resp.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val models = json.getJSONArray("models")
            (0 until models.length()).map { models.getJSONObject(it).getString("name") }
        } catch (e: Exception) {
            Log.e(TAG, "listModels failed", e)
            emptyList()
        }
    }

    // ── Streaming chat ────────────────────────────────────────────
    fun streamChat(
        messages: List<Pair<String, String>>,
        systemPrompt: String,
        model: String? = null,
    ): Flow<String> = flow {
        val selectedModel = model ?: getModel()
        val msgs = JSONArray()
        if (systemPrompt.isNotBlank()) {
            msgs.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        messages.forEach { (role, content) ->
            msgs.put(JSONObject().put("role", role).put("content", content))
        }

        val body = JSONObject()
            .put("model", selectedModel)
            .put("messages", msgs)
            .put("stream", true)
            .toString()

        val request = Request.Builder()
            .url("${getEndpoint()}/api/chat")
            .addHeader("X-Champ-Token", getToken())
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = http.newCall(request).execute()
            if (!response.isSuccessful) {
                emit("[ERROR ${response.code}] Server error. Please try again.")
                return@flow
            }

            val source = response.body?.source() ?: run {
                emit("[ERROR] Empty response from server.")
                return@flow
            }

            val buffer = okio.Buffer()
            while (!source.exhausted()) {
                source.read(buffer, 8192)
                val line = buffer.readUtf8()
                line.lines().filter { it.isNotBlank() }.forEach { jsonLine ->
                    try {
                        val obj = JSONObject(jsonLine)
                        val token = obj.optJSONObject("message")
                            ?.optString("content", "") ?: ""
                        if (token.isNotEmpty()) emit(token)
                        if (obj.optBoolean("done", false)) return@flow
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream error", e)
            emit("\n[CONNECTION ERROR] Could not reach ChampEngine servers. Check your internet connection.")
        }
    }.flowOn(Dispatchers.IO)
}
