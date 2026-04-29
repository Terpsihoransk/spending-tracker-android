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

object HttpClientFactory {

    // adb reverse tcp:8081 tcp:8081 — туннелирует 127.0.0.1:8081 эмулятора → 127.0.0.1:8081 хоста
//    private const val BASE_URL = "http://127.0.0.1:8081/api/v1/"
    private const val BASE_URL = "http://192.168.1.101:8081/api/v1/"
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
            // Временно ALL для диагностики; после починки вернуть INFO.
            level = LogLevel.ALL
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
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 20_000
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
