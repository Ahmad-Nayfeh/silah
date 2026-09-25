package io.github.ahmadnayfeh.silah.domain

object PhoneNumbers {
    const val DEFAULT_COUNTRY_CODE = "966"

    /**
     * Normalises what the user typed (or what the contacts app returned) to international
     * form, e.g. "050 123 4567" → "+966501234567". Returns null when it is not a usable number.
     */
    fun normalize(raw: String?, countryCode: String = DEFAULT_COUNTRY_CODE): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = toLatinDigits(raw).trim()
        val digits = trimmed.filter { it.isDigit() }
        val international = when {
            trimmed.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> countryCode + digits.drop(1)
            // A local Saudi mobile typed without its leading zero: 5XXXXXXXX
            digits.length == 9 && digits.startsWith("5") && countryCode == "966" -> countryCode + digits
            else -> digits
        }
        return if (international.length in 8..15) "+$international" else null
    }

    /** Digits only, as wa.me expects. */
    fun waDigits(phone: String): String = toLatinDigits(phone).filter { it.isDigit() }

    /** Contacts saved on an Arabic phone may use Arabic-Indic digits (٠١٢…). */
    private fun toLatinDigits(s: String): String = buildString {
        for (ch in s) {
            append(
                when (ch) {
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    else -> ch
                },
            )
        }
    }
}
