package io.github.yizhu.flint.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * WebDAV sync: silent push on every config change; pull on manual request or
 * when the last pull is older than 24h. No UI, no notifications — invisible.
 */
class WebDavRepo(
    private val url: String,      // full URL to the backup file, incl. filename
    private val username: String,
    private val password: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun Request.Builder.auth() = header(
        "Authorization",
        okhttp3.Credentials.basic(username, password),
    )

    /** PUT the config JSON. */
    suspend fun push(content: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .auth()
                .put(content.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    /** GET the config JSON; null when missing or unreachable. */
    suspend fun pull(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).auth().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body.string() else null
            }
        }.getOrNull()
    }
}
