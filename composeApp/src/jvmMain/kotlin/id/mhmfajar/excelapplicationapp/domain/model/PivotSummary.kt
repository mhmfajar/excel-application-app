package id.mhmfajar.excelapplicationapp.domain.model

data class PivotSummary(
    val salesStore: String,
    val invoiceNumber: String,
    val customerNumber: String,
    val customerName: String,
    val unitBisnis: String,
    val trxDate: String,
    val totalAmountPpn: Double
)
