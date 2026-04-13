package id.mhmfajar.excelapplicationapp.presentation

import id.mhmfajar.excelapplicationapp.data.database.DatabaseHelper
import id.mhmfajar.excelapplicationapp.data.excel.ExcelReader
import id.mhmfajar.excelapplicationapp.data.excel.ExcelWriter
import id.mhmfajar.excelapplicationapp.data.repository.DataRepository
import id.mhmfajar.excelapplicationapp.domain.model.PivotSummary
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainViewModel {

    private val reader = ExcelReader()
    private val writer = ExcelWriter()
    private val dbHelper = DatabaseHelper()
    private val repository = DataRepository(dbHelper)

    var currentViewMode: ViewMode by mutableStateOf(ViewMode.DATA)
    var headers: List<String> by mutableStateOf(emptyList())
    var currentPageData: List<RowDataReport> by mutableStateOf(emptyList())
    var totalCount: Int by mutableStateOf(0)
    var currentPage: Int by mutableStateOf(0)
        private set

    var sortRules: List<Pair<String, Boolean>> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set
    var loadProgress: Float by mutableStateOf(0f)
        private set

    var currentFilters: List<Pair<String, String>> by mutableStateOf(emptyList())
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var uniqueSalesStores: List<String> by mutableStateOf(emptyList())
        private set
    var uniqueCustomerNumbers: List<String> by mutableStateOf(emptyList())
        private set

    var draftFilters: List<Pair<String, String>> by mutableStateOf(DEFAULT_FILTERS)
        private set

    var dynamicSuggestions: Map<Int, List<String>> by mutableStateOf(emptyMap())
        private set

    val pageSize = 100

    private var loadJob: Job? = null
    private var suggestionJob: Job? = null

    fun load(file: File, coroutineScope: CoroutineScope) {
        loadJob = coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            loadProgress = 0f
            errorMessage = null

            try {
                val data = reader.read(file) { progress ->
                    loadProgress = progress * 0.5f
                }

                loadProgress = 50f

                if (data.isNotEmpty()) {
                    headers = RowDataReport.STATIC_HEADERS

                    val dbPath = getDbPath()
                    File(dbPath).parentFile?.mkdirs()

                    dbHelper.connect(dbPath)
                    dbHelper.createTables()
                    repository.clearDatabase()
                    repository.saveData(data) { dbProgress ->
                        loadProgress = 50f + (dbProgress * 0.5f)
                    }

                    totalCount = repository.getDataCount()
                    uniqueSalesStores = repository.getUniqueValues("SALES_STORE")
                    uniqueCustomerNumbers = repository.getUniqueValues("CUSTOMER_NUMBER")
                    draftFilters = DEFAULT_FILTERS
                    updateDraftFilters(draftFilters, coroutineScope)
                    currentPage = 0
                    loadPage(0)
                }

                isLoading = false
                loadProgress = 100f
            } catch (e: Exception) {
                isLoading = false
                loadProgress = 0f
                errorMessage = e.message ?: "Unknown error while reading file"
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun loadPage(page: Int) {
        val offset = page * pageSize
        val newData = if (currentFilters.isNotEmpty()) {
            repository.filterDataPaginated(currentFilters, offset, pageSize, sortRules)
        } else {
            repository.getPaginatedData(offset, pageSize, sortRules)
        }

        currentPageData = if (page == 0) newData else currentPageData + newData
    }

    fun loadNextPage() {
        if (currentPageData.size < totalCount && !isLoading) {
            currentPage++
            loadPage(currentPage)
        }
    }

    fun filter(filters: List<Pair<String, String>>) {
        currentFilters = filters.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
        totalCount = repository.getFilteredCount(currentFilters)
        currentPage = 0
        loadPage(0)
    }

    fun export(file: File, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            loadProgress = 50f
            try {
                val data = if (currentFilters.isNotEmpty()) {
                    repository.filterData(currentFilters)
                } else {
                    repository.getAllData()
                }
                val pivotData = getPivotData()
                writer.write(headers, data, pivotData, file)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun clear() {
        repository.clearDatabase()
        headers = emptyList()
        currentPageData = emptyList()
        totalCount = 0
        currentPage = 0
        sortRules = emptyList()
        currentFilters = emptyList()
        uniqueSalesStores = emptyList()
        uniqueCustomerNumbers = emptyList()
        draftFilters = DEFAULT_FILTERS
        dynamicSuggestions = emptyMap()
    }

    /**
     * Updates draft filters and refreshes dynamic suggestions.
     * Uses the passed coroutineScope (or creates a supervised one) to avoid leaking.
     */
    fun updateDraftFilters(newFilters: List<Pair<String, String>>, scope: CoroutineScope? = null) {
        draftFilters = newFilters

        suggestionJob?.cancel()
        val effectiveScope = scope ?: CoroutineScope(Dispatchers.IO + Job())
        suggestionJob = effectiveScope.launch(Dispatchers.IO) {
            val newSuggestions = mutableMapOf<Int, List<String>>()
            newFilters.forEachIndexed { index, rule ->
                if (rule.first == "SALES_STORE" || rule.first == "CUSTOMER_NUMBER") {
                    val constraints = newFilters.filterIndexed { i, _ -> i != index }
                    newSuggestions[index] = repository.getUniqueValues(rule.first, constraints)
                }
            }
            dynamicSuggestions = newSuggestions
        }
    }

    fun sort(column: String) {
        val newSortRules = sortRules.toMutableList()
        val existingIndex = newSortRules.indexOfFirst { it.first == column }

        if (existingIndex != -1) {
            if (newSortRules[existingIndex].second) {
                newSortRules[existingIndex] = Pair(column, false)
            } else {
                newSortRules.removeAt(existingIndex)
            }
        } else {
            newSortRules.add(Pair(column, true))
        }

        sortRules = newSortRules
        currentPage = 0
        loadPage(0)
    }

    fun getPivotData(): List<PivotSummary> {
        val allData = if (currentFilters.isNotEmpty()) {
            repository.filterData(currentFilters)
        } else {
            repository.getAllData()
        }

        return allData.groupBy { it.invoiceNumber ?: "Unknown" }
            .map { (invoice, rows) ->
                val firstRow = rows.first()
                PivotSummary(
                    salesStore = firstRow.salesStore ?: "",
                    invoiceNumber = invoice,
                    customerNumber = firstRow.customerNumber ?: "",
                    customerName = firstRow.customerName ?: "",
                    unitBisnis = firstRow.unitBisnis ?: "",
                    trxDate = firstRow.trxDate ?: "",
                    totalAmountPpn = rows.sumOf { it.totalAmountPpn?.toDoubleOrNull() ?: 0.0 }
                )
            }
    }

    private fun getDbPath(): String {
        val userHome = System.getProperty("user.home")
        return "$userHome/.excelapplicationapp/data.db"
    }

    companion object {
        private val DEFAULT_FILTERS = listOf(
            Pair("SALES_STORE", ""),
            Pair("CUSTOMER_NUMBER", "")
        )
    }
}