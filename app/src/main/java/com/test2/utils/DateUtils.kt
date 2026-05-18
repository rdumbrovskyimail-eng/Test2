package com.test2.utils

import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Утилиты для работы с датами и временем.
 * Использует java.time где возможно, для совместимости держим и старый Date.
 */
object DateUtils {

    private const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val DATE_ONLY_FORMAT = "yyyy-MM-dd"
    private const val DATE_RU_FORMAT = "dd.MM.yyyy"
    private const val TIME_ONLY_FORMAT = "HH:mm:ss"
    private const val DATETIME_RU_FORMAT = "dd.MM.yyyy HH:mm"

    private val isoFormatter: SimpleDateFormat
        get() = SimpleDateFormat(ISO_FORMAT, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val ruDateFormatter: SimpleDateFormat
        get() = SimpleDateFormat(DATE_RU_FORMAT, Locale("ru"))

    private val ruDateTimeFormatter: SimpleDateFormat
        get() = SimpleDateFormat(DATETIME_RU_FORMAT, Locale("ru"))

    fun nowEpochMillis(): Long = System.currentTimeMillis()

    fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000

    fun nowInstant(): Instant = Instant.now()

    fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))

    fun nowLocal(): LocalDateTime = LocalDateTime.now()

    fun today(): LocalDate = LocalDate.now()

    fun tomorrow(): LocalDate = LocalDate.now().plusDays(1)

    fun yesterday(): LocalDate = LocalDate.now().minusDays(1)

    fun formatIso(epochMillis: Long): String {
        return isoFormatter.format(Date(epochMillis))
    }

    fun parseIso(iso: String): Long? {
        return try {
            isoFormatter.parse(iso)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun formatRuDate(epochMillis: Long): String {
        return ruDateFormatter.format(Date(epochMillis))
    }

    fun formatRuDateTime(epochMillis: Long): String {
        return ruDateTimeFormatter.format(Date(epochMillis))
    }

    fun formatRelative(epochMillis: Long, locale: String = "ru"): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMillis
        val isPast = diff >= 0
        val absDiff = Math.abs(diff)

        val seconds = absDiff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 5 -> if (locale == "ru") "только что" else "just now"
            minutes < 1 -> formatTimeAgo(seconds, "сек", "sec", isPast, locale)
            hours < 1 -> formatTimeAgo(minutes, "мин", "min", isPast, locale)
            days < 1 -> formatTimeAgo(hours, "ч", "h", isPast, locale)
            months < 1 -> formatTimeAgo(days, "д", "d", isPast, locale)
            years < 1 -> formatTimeAgo(months, "мес", "mo", isPast, locale)
            else -> formatTimeAgo(years, "г", "y", isPast, locale)
        }
    }

    private fun formatTimeAgo(value: Long, unitRu: String, unitEn: String, past: Boolean, locale: String): String {
        val unit = if (locale == "ru") unitRu else unitEn
        val template = if (past) {
            if (locale == "ru") "$value $unit назад" else "$value $unit ago"
        } else {
            if (locale == "ru") "через $value $unit" else "in $value $unit"
        }
        return template
    }

    fun daysBetween(start: Long, end: Long): Long {
        return Duration.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)).toDays()
    }

    fun hoursBetween(start: Long, end: Long): Long {
        return Duration.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)).toHours()
    }

    fun minutesBetween(start: Long, end: Long): Long {
        return Duration.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)).toMinutes()
    }

    fun secondsBetween(start: Long, end: Long): Long {
        return Duration.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)).seconds
    }

    fun addDays(epochMillis: Long, days: Long): Long {
        return Instant.ofEpochMilli(epochMillis).plus(days, ChronoUnit.DAYS).toEpochMilli()
    }

    fun addHours(epochMillis: Long, hours: Long): Long {
        return Instant.ofEpochMilli(epochMillis).plus(hours, ChronoUnit.HOURS).toEpochMilli()
    }

    fun addMinutes(epochMillis: Long, minutes: Long): Long {
        return Instant.ofEpochMilli(epochMillis).plus(minutes, ChronoUnit.MINUTES).toEpochMilli()
    }

    fun startOfDay(epochMillis: Long): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun endOfDay(epochMillis: Long): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun isSameDay(a: Long, b: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val dateA = Instant.ofEpochMilli(a).atZone(zone).toLocalDate()
        val dateB = Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
        return dateA == dateB
    }

    fun isToday(epochMillis: Long): Boolean = isSameDay(epochMillis, System.currentTimeMillis())

    fun isYesterday(epochMillis: Long): Boolean {
        val yesterdayMs = System.currentTimeMillis() - 86_400_000L
        return isSameDay(epochMillis, yesterdayMs)
    }

    fun isInPast(epochMillis: Long): Boolean = epochMillis < System.currentTimeMillis()

    fun isInFuture(epochMillis: Long): Boolean = epochMillis > System.currentTimeMillis()

    fun getDayOfWeek(epochMillis: Long, locale: String = "ru"): String {
        val day = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .dayOfWeek
        return if (locale == "ru") {
            when (day.value) {
                1 -> "Понедельник"; 2 -> "Вторник"; 3 -> "Среда"; 4 -> "Четверг"
                5 -> "Пятница"; 6 -> "Суббота"; 7 -> "Воскресенье"
                else -> ""
            }
        } else day.name
    }

    fun getMonthName(epochMillis: Long, locale: String = "ru"): String {
        val month = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .month
        return if (locale == "ru") {
            when (month.value) {
                1 -> "Январь"; 2 -> "Февраль"; 3 -> "Март"; 4 -> "Апрель"
                5 -> "Май"; 6 -> "Июнь"; 7 -> "Июль"; 8 -> "Август"
                9 -> "Сентябрь"; 10 -> "Октябрь"; 11 -> "Ноябрь"; 12 -> "Декабрь"
                else -> ""
            }
        } else month.name
    }
}