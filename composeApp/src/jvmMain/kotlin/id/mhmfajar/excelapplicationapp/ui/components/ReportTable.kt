package id.mhmfajar.excelapplicationapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import id.mhmfajar.excelapplicationapp.util.NumberFormatter
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

/**
 * Represents an item in the report spreadsheet view.
 */
sealed class SpreadsheetItem {
    data class Content(
        val row: RowDataReport,
        val groupNo: Int,
        val isFirstInGroup: Boolean
    ) : SpreadsheetItem()

    data object Separator : SpreadsheetItem()
}

@Composable
fun ReportTable(
    headers: List<String>,
    data: List<RowDataReport>,
    onHeaderClick: (String) -> Unit,
    sortRules: List<Pair<String, Boolean>>,
    onEndReached: () -> Unit,
    selectedIds: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onToggleAllSelection: (Boolean) -> Unit,
    isAllSelected: Boolean
) {
    val checkboxColumnWidth = 50.dp
    val noColumnWidth = 60.dp
    val columnWidth = 170.dp

    val spreadsheetItems = remember(data) {
        buildSpreadsheetItems(data)
    }

    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    InfiniteScrollEffect(listState = listState, onEndReached = onEndReached)

    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(checkboxColumnWidth).border(0.5.dp, borderColor).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { onToggleAllSelection(it) }
                )
            }

            Box(
                modifier = Modifier.width(noColumnWidth).border(0.5.dp, borderColor).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "NO",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            headers.forEach { header ->
                Row(
                    modifier = Modifier
                        .width(columnWidth)
                        .clickable { onHeaderClick(header) }
                        .border(0.5.dp, borderColor)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = header,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    SortIndicator(header, sortRules, MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Rows
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            items(spreadsheetItems) { item ->
                when (item) {
                    is SpreadsheetItem.Separator -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(checkboxColumnWidth).fillMaxHeight().border(0.5.dp, borderColor))
                            Box(modifier = Modifier.width(noColumnWidth).fillMaxHeight().border(0.5.dp, borderColor))
                            headers.forEach { _ ->
                                Box(modifier = Modifier.width(columnWidth).fillMaxHeight().border(0.5.dp, borderColor))
                            }
                        }
                    }
                    is SpreadsheetItem.Content -> {
                        val row = item.row
                        Row(
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.width(checkboxColumnWidth).border(0.5.dp, borderColor).padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(row.id),
                                    onCheckedChange = { row.id?.let { onToggleSelection(it) } }
                                )
                            }

                            Box(
                                modifier = Modifier.width(noColumnWidth).border(0.5.dp, borderColor).padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.isFirstInGroup) {
                                    Text(
                                        text = item.groupNo.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            headers.forEach { key ->
                                val value = row.getFieldByHeader(key)
                                Box(
                                    modifier = Modifier.width(columnWidth).border(0.5.dp, borderColor).padding(8.dp)
                                ) {
                                    Text(
                                        text = NumberFormatter.formatDisplayValue(key, value),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Builds the spreadsheet items list by grouping rows by invoice number.
 */
private fun buildSpreadsheetItems(data: List<RowDataReport>): List<SpreadsheetItem> {
    val list = mutableListOf<SpreadsheetItem>()
    var groupCounter = 1
    var lastInvoice: String? = ""

    data.forEachIndexed { idx, row ->
        if (idx > 0 && row.invoiceNumber != lastInvoice) {
            list.add(SpreadsheetItem.Separator)
            groupCounter++
        }
        list.add(
            SpreadsheetItem.Content(
                row = row,
                groupNo = groupCounter,
                isFirstInGroup = idx == 0 || row.invoiceNumber != lastInvoice
            )
        )
        lastInvoice = row.invoiceNumber
    }
    return list
}
