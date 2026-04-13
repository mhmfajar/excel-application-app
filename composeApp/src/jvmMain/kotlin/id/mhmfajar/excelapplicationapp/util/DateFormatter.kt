package id.mhmfajar.excelapplicationapp.util

/**
 * Utility object for formatting date strings from Excel.
 * Converts date strings like "Mon Jan 15 00:00:00 WIB 2024" → "15/01/2024".
 */
object DateFormatter {

    private val MONTH_MAP = mapOf(
        "jan" to "01", "feb" to "02", "mar" to "03",
        "apr" to "04", "may" to "05", "jun" to "06",
        "jul" to "07", "aug" to "08", "sep" to "09",
        "oct" to "10", "nov" to "11", "dec" to "12"
    )

    /**
     * Formats a raw date string (e.g. Java Date.toString() format) to dd/MM/yyyy.
     * Returns null if input is null, returns the original string if parsing fails.
     */
    fun format(dateStr: String?): String? {
        if (dateStr == null) return null
        return try {
            val parts = dateStr.split(" ").filter { it.isNotEmpty() }
            if (parts.size == 6) {
                val month = MONTH_MAP[parts[1].lowercase()] ?: "01"
                val day = parts[2].padStart(2, '0')
                val year = parts[5]
                "$day/$month/$year"
            } else {
                dateStr
            }
        } catch (_: Exception) {
            dateStr
        }
    }
}
