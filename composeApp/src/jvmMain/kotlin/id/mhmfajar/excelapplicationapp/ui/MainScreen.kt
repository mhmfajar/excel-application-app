package id.mhmfajar.excelapplicationapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mhmfajar.excelapplicationapp.presentation.MainViewModel
import id.mhmfajar.excelapplicationapp.presentation.ViewMode
import id.mhmfajar.excelapplicationapp.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val data = vm.currentPageData
    val headers = vm.headers
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ReportFlux", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (vm.totalCount > 0) {
                            Spacer(Modifier.width(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "${vm.totalCount} rows",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val file = pickFile()
                            if (file != null) vm.load(file, scope)
                        },
                        enabled = !vm.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upload Excel")
                    }

                    Spacer(Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            val file = saveFile()
                            if (file != null) vm.export(file, scope)
                        },
                        enabled = headers.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Export")
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { vm.clear() },
                        enabled = headers.isNotEmpty() && !vm.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear")
                    }
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "@mhm.fajar",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Filter Card
                if (headers.isNotEmpty()) {
                    FilterCard(vm = vm, headers = headers)
                }

                Spacer(Modifier.height(16.dp))

                // Table Container
                if (data.isNotEmpty()) {
                    TableContainer(vm = vm, headers = headers, data = data)
                } else if (headers.isNotEmpty() && !vm.isLoading) {
                    EmptyStateComponent("No matching data found.", "🔍")
                } else if (headers.isEmpty() && !vm.isLoading) {
                    EmptyStateComponent("Upload an Excel file to get started.", "📁")
                }
            }

            // Error dialog
            if (vm.errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { vm.clearError() },
                    title = { Text("Validation Error") },
                    text = { Text(vm.errorMessage!!) },
                    confirmButton = {
                        Button(onClick = { vm.clearError() }) { Text("OK") }
                    }
                )
            }

            // Loading overlay
            if (vm.isLoading) {
                LoadingOverlay(vm.loadProgress)
            }
        }
    }
}

@Composable
private fun ColumnScope.TableContainer(
    vm: MainViewModel,
    headers: List<String>,
    data: List<id.mhmfajar.excelapplicationapp.domain.model.RowDataReport>
) {
    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when (vm.currentViewMode) {
                ViewMode.DATA -> DataTable(
                    headers = headers,
                    data = data,
                    onHeaderClick = { vm.sort(it) },
                    sortRules = vm.sortRules,
                    onEndReached = { vm.loadNextPage() }
                )
                ViewMode.REPORT -> ReportTable(
                    headers = headers,
                    data = data,
                    onHeaderClick = { vm.sort(it) },
                    sortRules = vm.sortRules,
                    onEndReached = { vm.loadNextPage() }
                )
                ViewMode.PIVOT -> PivotTable(data = vm.getPivotData())
            }
        }

        // Sheet tabs
        SheetTabs(
            currentMode = vm.currentViewMode,
            onModeSelected = { vm.currentViewMode = it }
        )
    }
}

@Composable
private fun SheetTabs(
    currentMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit
) {
    val modes = listOf(
        ViewMode.DATA to "Sheet1 (Data)",
        ViewMode.REPORT to "Sheet2 (Report)",
        ViewMode.PIVOT to "Sheet3 (Pivot)"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { (mode, label) ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSelected) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp).width(300.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Processing Excel...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "${progress.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
