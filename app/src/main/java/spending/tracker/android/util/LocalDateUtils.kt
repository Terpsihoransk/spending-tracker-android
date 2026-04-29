package spending.tracker.android.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Единый формат дат между API, БД и Domain: ISO-8601 (YYYY-MM-DD). */
object LocalDateFormats {
    val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
}

fun LocalDate.toIsoString(): String = format(LocalDateFormats.ISO)

fun String.toLocalDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(this, LocalDateFormats.ISO)
}.getOrNull()

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, LocalDateFormats.ISO)

/**
 * Сериализатор [LocalDate] для kotlinx.serialization (используется в DTO).
 * Формат: ISO-8601 строка.
 */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toIsoString())
    }

    override fun deserialize(decoder: Decoder): LocalDate =
        decoder.decodeString().toLocalDate()
}
