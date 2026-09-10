package xyz.tyiu.satsprice.domain

import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun localizedDateTime(dateTime: LocalDateTime): String {
    val components = NSDateComponents().apply {
        year = dateTime.year.toLong()
        month = dateTime.monthNumber.toLong()
        day = dateTime.dayOfMonth.toLong()
        hour = dateTime.hour.toLong()
        minute = dateTime.minute.toLong()
    }
    val date = NSCalendar.currentCalendar.dateFromComponents(components) ?: return dateTime.toString()
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
        locale = NSLocale.currentLocale
    }
    return formatter.stringFromDate(date)
}
