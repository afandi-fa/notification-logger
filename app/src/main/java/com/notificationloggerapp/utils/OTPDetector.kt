package com.notificationloggerapp.utils

class OTPDetector {
    private val otpPatterns = listOf(
        Regex("""\b\d{4}\b"""),           // 4 digit OTP
        Regex("""\b\d{6}\b"""),           // 6 digit OTP
        Regex("""\b\d{8}\b"""),           // 8 digit OTP
        Regex("""(?:OTP|otp|code|Code).*?(\d{4,8})"""),
        Regex("""(\d{4,8}).*?(?:OTP|otp|code|Code)""")
    )

    fun detectOTP(text: String?): Pair<Boolean, String?> {
        if (text.isNullOrEmpty()) return Pair(false, null)

        for (pattern in otpPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val code = match.groups[1]?.value ?: match.value
                // Filter out common false positives
                if (code.length in 4..8 && !isFalsePositive(code, text)) {
                    return Pair(true, code)
                }
            }
        }
        return Pair(false, null)
    }

    private fun isFalsePositive(code: String, text: String): Boolean {
        // Avoid phone numbers, dates, etc.
        val lowerText = text.lowercase()
        return lowerText.contains("phone") ||
                lowerText.contains("call") ||
                code.all { it == code[0] } // All same digit
    }
}
