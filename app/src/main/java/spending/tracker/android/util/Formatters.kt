package spending.tracker.android.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Локаль по умолчанию для всех форматтеров в приложении (ru). */
private val appLocale: Locale = Locale("ru", "RU")

/**
 * Форматирование суммы расхода: `12 345 ₽` / `12 345,50 ₽`.
 *
 * Если у суммы нет копеек — показываем без дробной части, чтобы не засорять UI.
 */
fun formatMoney(amount: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(appLocale).apply {
        maximumFractionDigits = if (amount.scale() <= 0 || amount.stripTrailingZeros().scale() <= 0) 0 else 2
        minimumFractionDigits = if (amount.scale() <= 0 || amount.stripTrailingZeros().scale() <= 0) 0 else 2
    }
    return format.format(amount)
}

/** Перегрузка для Double (для обратной совместимости). */
fun formatMoney(amount: Double): String = formatMoney(amount.toBigDecimal())

private val dayMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", appLocale)

private val dayMonthShortFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", appLocale)

/** Полная дата: «15 марта». */
fun formatDayMonth(date: LocalDate): String = date.format(dayMonthFormatter)

/** Короткая дата: «15 мар». */
fun formatDayMonthShort(date: LocalDate): String = date.format(dayMonthShortFormatter)

/** Относительная дата для секций списка: «Сегодня» / «Вчера» / `formatDayMonth`. */
fun formatRelativeDate(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Сегодня"
    today.minusDays(1) -> "Вчера"
    else -> formatDayMonth(date)
}

/** Название месяца с заглавной буквы: «Март 2025». */
fun formatMonthYear(yearMonth: java.time.YearMonth): String {
    val month = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, appLocale)
        .replaceFirstChar { it.uppercase(appLocale) }
    return "$month ${yearMonth.year}"
}

/** Короткое название месяца: «Мар». */
fun formatMonthShort(yearMonth: java.time.YearMonth): String {
    return yearMonth.month.getDisplayName(TextStyle.SHORT_STANDALONE, appLocale)
        .replaceFirstChar { it.uppercase(appLocale) }
}
