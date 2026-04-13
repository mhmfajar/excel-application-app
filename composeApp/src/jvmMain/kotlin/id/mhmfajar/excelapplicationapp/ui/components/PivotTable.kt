package id.mhmfajar.excelapplicationapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mhmfajar.excelapplicationapp.domain.model.PivotSummary
import id.mhmfajar.excelapplicationapp.util.NumberFormatter

private val PIVOT_COLUMNS = listOf(
    "SALES_STORE", "INVOICE_NUMBER", "CUSTOMER_NUMBER",
    "CUSTOMER_NAME", "UNIT_BISNIS", "TRX_DATE", "Total"
)

private val WIDE_COLUMNS = setOf("CUSTOMER_NAME", "UNIT_BISNIS", "INVOICE_NUMBER")
private val STANDARD_WIDTH = 120.dp
private val WIDE_WIDTH = 250.dp

@Composable
fun PivotTable(data: List<PivotSummary>) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        // Header
        Row(modifier = Modifier.background(Color(0xFFCCCCCC))) {
            PIVOT_COLUMNS.forEach { header ->
                val width = if (header in WIDE_COLUMNS) WIDE_WIDTH else STANDARD_WIDTH
                Box(
                    modifier = Modifier.width(width).border(0.5.dp, borderColor).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = header,
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(data) { summary ->
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    val cells = listOf(
                        summary.salesStore to STANDARD_WIDTH,
                        summary.invoiceNumber to WIDE_WIDTH,
                        summary.customerNumber to STANDARD_WIDTH,
                        summary.customerName to WIDE_WIDTH,
                        summary.unitBisnis to WIDE_WIDTH,
                        NumberFormatter.formatDisplayValue("TRX_DATE", summary.trxDate) to STANDARD_WIDTH,
                        NumberFormatter.formatDisplayValue("Total", summary.totalAmountPpn.toString()) to STANDARD_WIDTH
                    )

                    cells.forEachIndexed { index, (value, width) ->
                        Box(
                            modifier = Modifier.width(width).border(0.5.dp, borderColor).padding(12.dp),
                            contentAlignment = if (index == 6) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Text(
                                text = value,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Grand Total row
            item {
                val totalAmountPpn = data.sumOf { it.totalAmountPpn }
                val mergedWidth = STANDARD_WIDTH * 3 + WIDE_WIDTH * 3

                Row(modifier = Modifier.background(Color.White)) {
                    Box(
                        modifier = Modifier.width(mergedWidth).border(0.5.dp, borderColor).padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Grand Total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Box(
                        modifier = Modifier.width(STANDARD_WIDTH).border(0.5.dp, borderColor).padding(12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = NumberFormatter.formatDisplayValue("Total", totalAmountPpn.toString()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
