package io.github.yizhu.flint.translate

import android.content.Context
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation as MlTranslation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface TrResult {
    data class Ok(val text: String) : TrResult
    data class Err(val message: String) : TrResult
}

data class Translation(
    val engineId: String,
    val source: String,
    val result: TrResult,
)

/** Language auto-detection: non-Chinese -> zh, Chinese -> en. */
suspend fun detectTarget(text: String): String {
    val tag = suspendCancellableCoroutine { cont ->
        val id = LanguageIdentification.getClient()
        id.identifyLanguage(text)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
        cont.invokeOnCancellation { id.close() }
    }
    return if (tag == "zh" || tag.startsWith("zh-")) "en" else "zh"
}

class TranslateRepo {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** ML Kit on-device. Caller passes target ("zh" or "en"); source is the other. */
    suspend fun mlkit(context: Context, text: String, target: String): Translation {
        val src = if (target == "zh") TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
        val out = suspendCancellableCoroutine { cont ->
            val translator = MlTranslation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(src)
                    .setTargetLanguage(target)
                    .build()
            )
            // First use: download model, then translate.
            translator.downloadModelIfNeeded()
                .addOnSuccessListener { translator.translate(text)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) } }
                .addOnFailureListener { cont.resumeWithException(it) }
            cont.invokeOnCancellation { translator.close() }
        }
        return Translation("mlkit", text, TrResult.Ok(out))
    }

    /** MyMemory free API (no key). */
    suspend fun mymemory(text: String, target: String): Translation {
        val pair = if (target == "zh") "en|zh-CN" else "zh-CN|en"
        val out = withContext(Dispatchers.IO) {
            val url = "https://api.mymemory.translated.net/get?q=" +
                java.net.URLEncoder.encode(text, "UTF-8") + "&langpair=$pair"
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                json.parseToJsonElement(resp.body.string())
                    .jsonObject["responseData"]!!
                    .jsonObject["translatedText"]!!
                    .jsonPrimitive.content
            }
        }
        return Translation("mymemory", text, TrResult.Ok(out))
    }

    /** Google unofficial free endpoint. */
    suspend fun google(text: String, target: String): Translation {
        val from = if (target == "zh") "en" else "zh-CN"
        val to = if (target == "zh") "zh-CN" else "en"
        val out = withContext(Dispatchers.IO) {
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$from&tl=$to&dt=t&q=" +
                java.net.URLEncoder.encode(text, "UTF-8")
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                val arr = json.parseToJsonElement(resp.body.string()).jsonArray
                buildString {
                    for (seg in arr[0].jsonArray) {
                        append(seg.jsonArray[0].jsonPrimitive.content)
                    }
                }
            }
        }
        return Translation("google", text, TrResult.Ok(out))
    }

    /** Run all enabled engines in parallel; failures become Err entries. */
    suspend fun translateAll(
        context: Context,
        text: String,
        engines: List<String>,
    ): List<Translation> = coroutineScope {
        val target = detectTarget(text)
        engines.map { id ->
            async {
                when (id) {
                    "mlkit" -> runCatching { mlkit(context, text, target) }
                    "mymemory" -> runCatching { mymemory(text, target) }
                    "google" -> runCatching { google(text, target) }
                    else -> Result.failure(IllegalArgumentException("unknown engine $id"))
                }.getOrElse { e ->
                    Translation(id, text, TrResult.Err(e.message ?: "error"))
                }
            }
        }.awaitAll()
    }
}
