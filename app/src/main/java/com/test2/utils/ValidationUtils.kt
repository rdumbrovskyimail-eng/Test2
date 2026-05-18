package com.test2.utils

object ValidationUtils {

    fun isPositiveInt(s: String): Boolean = s.toIntOrNull()?.let { it > 0 } ?: false
    fun isNonNegativeInt(s: String): Boolean = s.toIntOrNull()?.let { it >= 0 } ?: false
    fun isPositiveDouble(s: String): Boolean = s.toDoubleOrNull()?.let { it > 0.0 } ?: false

    fun isInRange(value: Int, min: Int, max: Int): Boolean = value in min..max
    fun isInRange(value: Double, min: Double, max: Double): Boolean = value in min..max

    fun isStrongPassword(p: String): Boolean {
        if (p.length < 8) return false
        val hasUpper = p.any { it.isUpperCase() }
        val hasLower = p.any { it.isLowerCase() }
        val hasDigit = p.any { it.isDigit() }
        val hasSpecial = p.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    fun isWeakPassword(p: String): Boolean = !isStrongPassword(p)

    fun sanitize(input: String, maxLength: Int = 1000): String =
        input.trim().take(maxLength).replace(Regex("[\\x00-\\x1F]"), "")

    fun requireNotBlank(s: String, name: String): String {
        require(s.isNotBlank()) { "$name cannot be blank" }
        return s.trim()
    }

    fun requireInRange(value: Int, min: Int, max: Int, name: String): Int {
        require(value in min..max) { "$name must be in [$min..$max], got $value" }
        return value
    }
}