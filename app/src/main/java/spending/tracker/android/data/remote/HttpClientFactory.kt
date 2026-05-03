package spending.tracker.android.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import spending.tracker.android.BuildConfig

object HttpClientFactory {

    // URL из config.properties через BuildConfig
    private const val BASE_URL = BuildConfig.BASE_URL
    private const val TAG = "KtorHttpClient"

    fun create(): HttpClient = HttpClient(OkHttp) {
        // Бросать исключение на 4xx/5xx — удобно для Result.runCatching в репозиториях.
        expectSuccess = true

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }

        install(Logging) {
            level = if (BuildConfig.HTTP_LOGGING_ENABLED) LogLevel.ALL else LogLevel.NONE
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
        }

        install(HttpTimeout) {
            // Разделяем таймауты для точной диагностики:
            // connect — TCP handshake (быстро падает, если порт недоступен).
            // socket — ожидание данных после установления соединения.
            // request — общий таймаут на весь round-trip.
            connectTimeoutMillis = BuildConfig.HTTP_TIMEOUT_MS
            socketTimeoutMillis = BuildConfig.HTTP_TIMEOUT_MS * 3
            requestTimeoutMillis = BuildConfig.HTTP_TIMEOUT_MS * 5
        }

        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }

        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
