package xyz.tyiu.satsprice.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

actual fun localizedDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
    return dateTime.toJavaLocalDateTime().format(formatter)
}
