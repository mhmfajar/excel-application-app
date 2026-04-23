package id.mhmfajar.excelapplicationapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import id.mhmfajar.excelapplicationapp.presentation.MainViewModel
import id.mhmfajar.excelapplicationapp.presentation.ViewMode
import id.mhmfajar.excelapplicationapp.ui.components.DataTable
import id.mhmfajar.excelapplicationapp.ui.components.EmptyStateComponent
import id.mhmfajar.excelapplicationapp.ui.components.FilterCard
import id.mhmfajar.excelapplicationapp.ui.components.LoadingOverlay
import id.mhmfajar.excelapplicationapp.ui.components.PivotTable
import id.mhmfajar.excelapplicationapp.ui.components.ReportTable
import id.mhmfajar.excelapplicationapp.ui.components.SheetTabs
import id.mhmfajar.excelapplicationapp.ui.components.pickFile
import id.mhmfajar.excelapplicationapp.ui.components.saveFile

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
    data: List<RowDataReport>
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
