package id.mhmfajar.excelapplicationapp.domain.model

data class RowDataReport(
    val salesStore: String?,
    val customerName: String?,
    val invoiceNumber: String?,
    val customerNumber: String?,
    val unitBisnis: String?,
    val trxDate: String?,
    val description: String?,
    val amount: String?,
    val taxCode: String?,
    val ppn: String?,
    val totalAmountPpn: String?
) {
    /**
     * Returns the field value corresponding to the given header key.
     * Centralizes the header→field mapping that was previously duplicated
     * across ExcelWriter, MainScreen (getRowValue), etc.
     */
    fun getFieldByHeader(header: String): String? {
        return when (header) {
            "SALES_STORE" -> salesStore
            "CUSTOMER_NAME" -> customerName
            "INVOICE_NUMBER" -> invoiceNumber
            "CUSTOMER_NUMBER" -> customerNumber
            "UNIT_BISNIS" -> unitBisnis
            "TRX_DATE" -> trxDate
            "DESCRIPTION" -> description
            "AMOUNT" -> amount
            "TAX_CODE" -> taxCode
            "PPN" -> ppn
            "TOTAL_AMOUNT+PPN" -> totalAmountPpn
            else -> null
        }
    }

    companion object {
        val STATIC_HEADERS = listOf(
            "SALES_STORE",
            "CUSTOMER_NAME",
            "INVOICE_NUMBER",
            "CUSTOMER_NUMBER",
            "UNIT_BISNIS",
            "TRX_DATE",
            "DESCRIPTION",
            "AMOUNT",
            "TAX_CODE",
            "PPN",
            "TOTAL_AMOUNT+PPN"
        )
    }
}
