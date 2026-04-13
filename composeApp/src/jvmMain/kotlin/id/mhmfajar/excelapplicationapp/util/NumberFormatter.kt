package id.mhmfajar.excelapplicationapp.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Utility object for formatting numeric display values.
 */
object NumberFormatter {

    private val DECIMAL_SYMBOLS = DecimalFormatSymbols().apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val NUMBER_FORMAT = DecimalFormat("#,##0.####", DECIMAL_SYMBOLS)

    private val ID_COLUMNS = setOf("CUSTOMER_NUMBER", "SALES_STORE")

    /**
     * Formats a value for display based on column context.
     * - Date columns are reformatted via [DateFormatter].
     * - ID columns (CUSTOMER_NUMBER, SALES_STORE) have decimals stripped.
     * - Numeric values are formatted with Indonesian locale separators.
     */
    fun formatDisplayValue(key: String, value: Any?): String {
        if (value == null) return "-"

        val str = value.toString()

        // Date formatting
        if (key.equals("TRX_DATE", ignoreCase = true)) {
            return DateFormatter.format(str) ?: str
        }

        // Numeric formatting
        val doubleVal = str.toDoubleOrNull()
        if (doubleVal != null) {
            if (key.uppercase() in ID_COLUMNS) {
                return BigDecimal(str).toBigInteger().toString()
            }
            return NUMBER_FORMAT.format(doubleVal)
        }

        return str
    }
}
