package com.test2.utils

import java.text.Normalizer
import java.util.Locale

/**
 * Утилитарные функции для работы со строками.
 * Чистые функции без состояния, всё статично, потокобезопасно.
 */
object StringUtils {

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val NON_ALPHANUMERIC_REGEX = Regex("[^a-zA-Z0-9]")
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val URL_REGEX = Regex("^https?://[\\w.-]+(:\\d+)?(/.*)?$")
    private val PHONE_REGEX = Regex("^\\+?[0-9\\s\\-()]{7,20}$")

    fun isBlankOrEmpty(s: String?): Boolean = s.isNullOrBlank()

    fun normalizeWhitespace(s: String): String {
        if (s.isEmpty()) return s
        return s.trim().replace(WHITESPACE_REGEX, " ")
    }

    fun removeDiacritics(s: String): String {
        if (s.isEmpty()) return s
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(normalized, "")
    }

    fun slugify(s: String): String {
        if (s.isEmpty()) return ""
        val noDiacritics = removeDiacritics(s.lowercase(Locale.ROOT))
        return NON_ALPHANUMERIC_REGEX.replace(noDiacritics, "-")
            .trim('-')
            .replace(Regex("-+"), "-")
    }

    fun camelCase(s: String): String {
        if (s.isEmpty()) return ""
        val parts = s.split(WHITESPACE_REGEX, Regex("[_\\-]"))
            .filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        return parts.first().lowercase(Locale.ROOT) +
            parts.drop(1).joinToString("") {
                it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }
            }
    }

    fun snakeCase(s: String): String {
        if (s.isEmpty()) return ""
        return s.replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(WHITESPACE_REGEX, "_")
            .replace(Regex("-+"), "_")
            .lowercase(Locale.ROOT)
    }

    fun kebabCase(s: String): String {
        return snakeCase(s).replace('_', '-')
    }

    fun titleCase(s: String): String {
        if (s.isEmpty()) return s
        return s.split(WHITESPACE_REGEX)
            .joinToString(" ") { word ->
                if (word.isEmpty()) ""
                else word.lowercase(Locale.ROOT).replaceFirstChar { c -> c.titlecase(Locale.ROOT) }
            }
    }

    fun truncate(s: String, maxLength: Int, suffix: String = "..."): String {
        if (maxLength <= 0) return ""
        if (s.length <= maxLength) return s
        if (suffix.length >= maxLength) return s.take(maxLength)
        return s.take(maxLength - suffix.length) + suffix
    }

    fun reverse(s: String): String = s.reversed()

    fun isPalindrome(s: String): Boolean {
        if (s.isEmpty()) return true
        val cleaned = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        return cleaned == cleaned.reversed()
    }

    fun countWords(s: String): Int {
        if (s.isBlank()) return 0
        return s.trim().split(WHITESPACE_REGEX).count { it.isNotEmpty() }
    }

    fun countCharacters(s: String, includeSpaces: Boolean = true): Int {
        return if (includeSpaces) s.length else s.count { !it.isWhitespace() }
    }

    fun countOccurrences(s: String, substring: String): Int {
        if (substring.isEmpty() || s.isEmpty()) return 0
        var count = 0
        var index = 0
        while (index < s.length) {
            val found = s.indexOf(substring, index)
            if (found < 0) break
            count++
            index = found + substring.length
        }
        return count
    }

    fun isValidEmail(s: String): Boolean = EMAIL_REGEX.matches(s.trim())

    fun isValidUrl(s: String): Boolean = URL_REGEX.matches(s.trim())

    fun isValidPhone(s: String): Boolean = PHONE_REGEX.matches(s.trim())

    fun maskEmail(email: String): String {
        if (!isValidEmail(email)) return email
        val atIndex = email.indexOf('@')
        val local = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        val masked = if (local.length <= 2) "*".repeat(local.length)
                     else local.first() + "*".repeat(local.length - 2) + local.last()
        return masked + domain
    }

    fun maskPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 4) return phone
        return "*".repeat(digits.length - 4) + digits.takeLast(4)
    }

    fun extractDigits(s: String): String = s.filter { it.isDigit() }

    fun extractLetters(s: String): String = s.filter { it.isLetter() }

    fun pad(s: String, length: Int, char: Char = ' ', alignment: Alignment = Alignment.LEFT): String {
        if (s.length >= length) return s
        val padding = length - s.length
        return when (alignment) {
            Alignment.LEFT -> s + char.toString().repeat(padding)
            Alignment.RIGHT -> char.toString().repeat(padding) + s
            Alignment.CENTER -> {
                val left = padding / 2
                val right = padding - left
                char.toString().repeat(left) + s + char.toString().repeat(right)
            }
        }
    }

    enum class Alignment { LEFT, RIGHT, CENTER }

    fun repeat(s: String, times: Int): String {
        if (times <= 0) return ""
        return s.repeat(times)
    }

    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val longer = if (a.length >= b.length) a else b
        val shorter = if (a.length >= b.length) b else a
        return (longer.length - editDistance(longer, shorter)).toDouble() / longer.length
    }

    private fun editDistance(a: String, b: String): Int {
        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = costs[0]
            costs[0] = i
            for (j in 1..b.length) {
                val cur = costs[j]
                costs[j] = if (a[i - 1] == b[j - 1]) prev
                           else 1 + minOf(prev, costs[j], costs[j - 1])
                prev = cur
            }
        }
        return costs[b.length]
    }
}