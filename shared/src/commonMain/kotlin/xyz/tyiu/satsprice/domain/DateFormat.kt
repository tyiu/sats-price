package xyz.tyiu.satsprice.domain

import kotlinx.datetime.LocalDateTime

/**
 * A locale-aware, human-readable rendering of [dateTime] (e.g. "Sep 10, 2026, 10:03 AM" in
 * en-US), using each platform's own date/time formatter — mirroring how [localizedGroupedInteger]
 * and [localizedDecimalSeparator] format numbers. Deliberately drops seconds: a "last updated"
 * label doesn't need second-level precision.
 */
expect fun localizedDateTime(dateTime: LocalDateTime): String
