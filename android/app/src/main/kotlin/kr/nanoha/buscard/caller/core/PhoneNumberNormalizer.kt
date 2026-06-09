package kr.nanoha.buscard.caller.core

object PhoneNumberNormalizer {
    fun aliases(rawNumber: String?): Set<String> {
        val digits = rawNumber.orEmpty().filter { it.isDigit() }
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
}
