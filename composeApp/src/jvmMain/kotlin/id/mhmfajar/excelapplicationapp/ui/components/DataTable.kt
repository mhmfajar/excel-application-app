package id.mhmfajar.excelapplicationapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import id.mhmfajar.excelapplicationapp.util.NumberFormatter
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

@Composable
fun DataTable(
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
    val columnWidth = 170.dp
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    InfiniteScrollEffect(listState = listState, onEndReached = onEndReached)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        // Header row
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.primary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(checkboxColumnWidth).padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { onToggleAllSelection(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        checkmarkColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            headers.forEach { header ->
                Row(
                    modifier = Modifier
                        .width(columnWidth)
                        .clickable { onHeaderClick(header) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = header,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    SortIndicator(header, sortRules, MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Data rows
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            itemsIndexed(data) { index, row ->
                val rowBackground = if (index % 2 == 0)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

                Row(
                    modifier = Modifier.background(rowBackground),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(checkboxColumnWidth).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(row.id),
                            onCheckedChange = { row.id?.let { onToggleSelection(it) } }
                        )
                    }

                    headers.forEach { key ->
                        val value = row.getFieldByHeader(key)
                        Text(
                            text = NumberFormatter.formatDisplayValue(key, value),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(columnWidth).padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

