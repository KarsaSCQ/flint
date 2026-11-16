package io.github.yizhu.flint.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String)

/**
 * OpenAI-compatible chat client with SSE streaming.
 * Works with DeepSeek, OpenAI, or any compatible endpoint.
 */
class AiRepo(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Streams answer tokens; completes when done. Emits partial accumulations. */
    fun chatStream(history: List<ChatMessage>): Flow<String> = callbackFlow {
        val messages = history.joinToString(",") { m ->
            val obj = kotlinx.serialization.json.buildJsonObject {
                put("role", kotlinx.serialization.json.JsonPrimitive(m.role))
                put("content", kotlinx.serialization.json.JsonPrimitive(m.content))
            }
            json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), obj)
        }
        val body = """{"model":"$model","messages":[$messages],"stream":true}"""
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val call = client.newCall(req)
        val acc = StringBuilder()
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                trySend(e.message ?: "network error")
                close()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        trySend("HTTP ${resp.code}: ${resp.body.string().take(200)}")
                        close()
                        return
                    }
                    val source = resp.body.source()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data: ")) continue
                        val payload = line.removePrefix("data: ").trim()
                        if (payload == "[DONE]") break
                        val delta = runCatching {
                            json.parseToJsonElement(payload)
                                .jsonObject["choices"]!!.jsonArray[0]
                                .jsonObject["delta"]!!
                                .jsonObject["content"]?.jsonPrimitive?.content
                        }.getOrNull() ?: continue
                        if (delta != null) {
                            acc.append(delta)
                            trySend(acc.toString())
                        }
                    }
                    close()
                }
            }
        })
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)
}
