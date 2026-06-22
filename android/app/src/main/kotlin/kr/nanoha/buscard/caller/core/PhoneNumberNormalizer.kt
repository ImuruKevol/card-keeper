package kr.nanoha.buscard.caller.core

object PhoneNumberNormalizer {
    private val telPrefix = Regex("^tel:\\s*", RegexOption.IGNORE_CASE)
    private val extensionMarker = Regex("ext\\.?|extension|내선|#", RegexOption.IGNORE_CASE)

    fun display(rawNumber: String?): String {
        val text = rawNumber.orEmpty().trim().replace(telPrefix, "")
        if (text.isBlank()) return ""
        if (text.any { it !in "0123456789+().- " }) return text

        val digits = text.filter { it.isDigit() }
        if (digits.length < 7) return text

        if (digits.startsWith("82") && digits.length in 11..12) {
            val national = digits.drop(2).let { if (it.startsWith("0")) it else "0$it" }
            val formatted = formatKoreanLocal(national)
            if (formatted.isNotBlank()) {
                val prefix = if (text.startsWith("+")) "+82" else "82"
                return "$prefix-${formatted.removePrefix("0")}"
            }
        }

        return formatKoreanLocal(digits).ifBlank { text }
    }

    fun aliases(rawNumber: String?): Set<String> {
        val mainNumber = rawNumber.orEmpty()
            .trim()
            .replace(telPrefix, "")
            .split(extensionMarker, limit = 2)
            .firstOrNull()
            .orEmpty()
        val digits = mainNumber.filter { it.isDigit() }
        if (digits.isBlank()) return emptySet()

        val values = linkedSetOf(digits)
        if (digits.startsWith("82") && digits.length > 2) {
            values.add("0${digits.drop(2)}")
        }
        if (digits.startsWith("0") && digits.length > 1) {
            values.add("82${digits.drop(1)}")
        }
        return values
    }

    private fun formatKoreanLocal(digits: String): String {
        return when {
            Regex("^02\\d{7,8}$").matches(digits) -> {
                if (digits.length == 9) join(digits.take(2), digits.substring(2, 5), digits.drop(5))
                else join(digits.take(2), digits.substring(2, 6), digits.drop(6))
            }
            Regex("^050\\d{9}$").matches(digits) -> join(digits.take(4), digits.substring(4, 8), digits.drop(8))
            Regex("^0\\d{10}$").matches(digits) -> join(digits.take(3), digits.substring(3, 7), digits.drop(7))
            Regex("^0\\d{9}$").matches(digits) -> join(digits.take(3), digits.substring(3, 6), digits.drop(6))
            Regex("^\\d{8}$").matches(digits) -> join(digits.take(4), digits.drop(4))
            Regex("^\\d{7}$").matches(digits) -> join(digits.take(3), digits.drop(3))
            Regex("^\\d{11}$").matches(digits) -> join(digits.take(3), digits.substring(3, 7), digits.drop(7))
            Regex("^\\d{10}$").matches(digits) -> join(digits.take(3), digits.substring(3, 6), digits.drop(6))
            else -> ""
        }
    }

    private fun join(vararg parts: String): String {
        return parts.filter { it.isNotBlank() }.joinToString("-")
    }
}
