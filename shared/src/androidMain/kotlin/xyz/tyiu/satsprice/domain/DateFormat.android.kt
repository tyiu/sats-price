package xyz.tyiu.satsprice.domain

import kotlinx.datetime.LocalDateTime
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

// java.time requires API 26+ (this app's minSdk is 24) without core library desugaring, so this
// uses the older Calendar/DateFormat APIs instead, same as pre-java.time Android code always did.
actual fun localizedDateTime(dateTime: LocalDateTime): String {
    val calendar = Calendar.getInstance().apply {
        clear()
        set(dateTime.year, dateTime.monthNumber - 1, dateTime.dayOfMonth, dateTime.hour, dateTime.minute)
    }
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    return formatter.format(calendar.time)
}
