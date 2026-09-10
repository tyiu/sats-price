package xyz.tyiu.satsprice.domain

import kotlinx.datetime.LocalDateTime

// Individual Int fields (rather than the LocalDateTime itself) cross the js() boundary, matching
// the rest of this file's web interop — see SystemCurrencies.web.kt for why.
private fun jsFormatDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): String = js(
    """(function() {
        var date = new Date(year, month - 1, day, hour, minute);
        var fmt = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });
        return fmt.format(date);
    })()""",
)

actual fun localizedDateTime(dateTime: LocalDateTime): String =
    jsFormatDateTime(dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth, dateTime.hour, dateTime.minute)
