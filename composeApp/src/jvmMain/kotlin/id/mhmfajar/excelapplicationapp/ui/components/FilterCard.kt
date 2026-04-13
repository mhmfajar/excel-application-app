package id.mhmfajar.excelapplicationapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.mhmfajar.excelapplicationapp.presentation.MainViewModel

@Composable
fun FilterCard(vm: MainViewModel, headers: List<String>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            // Top row: title + action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter By:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))

                OutlinedButton(
                    onClick = {
                        val newList = vm.draftFilters.toMutableList()
                        val availableHeader = headers.firstOrNull { h ->
                            vm.draftFilters.none { it.first == h }
                        } ?: ""
                        newList.add(Pair(availableHeader, ""))
                        vm.updateDraftFilters(newList)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("+ Add Filter", fontSize = 12.sp)
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { vm.filter(vm.draftFilters) },
                    enabled = !vm.isLoading,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Apply Filter", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Filter rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.draftFilters.chunked(4).forEachIndexed { rowIndex, rowFilters ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowFilters.forEachIndexed { indexInRow, rule ->
                            val globalIndex = rowIndex * 4 + indexInRow

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val availableHeaders = headers.filter { h ->
                                    h == rule.first || vm.draftFilters.none { it.first == h }
                                }

                                DropdownMenuBox(
                                    options = availableHeaders,
                                    selected = rule.first,
                                    modifier = Modifier.weight(1f)
                                ) { newValue ->
                                    val newList = vm.draftFilters.toMutableList()
                                    newList[globalIndex] = rule.copy(first = newValue, second = "")
                                    vm.updateDraftFilters(newList)
                                }

                                if (rule.first == "SALES_STORE" || rule.first == "CUSTOMER_NUMBER") {
                                    val suggestions = vm.dynamicSuggestions[globalIndex] ?: emptyList()
                                    DropdownMenuBox(
                                        options = suggestions,
                                        selected = rule.second,
                                        placeholder = "Select Values",
                                        modifier = Modifier.weight(1f)
                                    ) { newValue ->
                                        val newList = vm.draftFilters.toMutableList()
                                        newList[globalIndex] = rule.copy(second = newValue)
                                        vm.updateDraftFilters(newList)
                                    }
                                } else {
                                    CompactTextField(
                                        value = rule.second,
                                        onValueChange = { newValue ->
                                            val newList = vm.draftFilters.toMutableList()
                                            newList[globalIndex] = rule.copy(second = newValue)
                                            vm.updateDraftFilters(newList)
                                        },
                                        placeholder = "Search keyword...",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (vm.draftFilters.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val newList = vm.draftFilters.toMutableList()
                                            newList.removeAt(globalIndex)
                                            vm.updateDraftFilters(newList)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    Spacer(Modifier.width(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
