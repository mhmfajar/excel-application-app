package id.mhmfajar.excelapplicationapp.data.repository

import id.mhmfajar.excelapplicationapp.data.database.DatabaseHelper
import id.mhmfajar.excelapplicationapp.domain.model.PivotSummary
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import java.sql.PreparedStatement
import java.sql.ResultSet

class DataRepository(private val dbHelper: DatabaseHelper) {

    // ─── Write Operations ────────────────────────────────────────────

    fun clearDatabase() {
        dbHelper.dropTables()
        dbHelper.createTables()
    }

    fun saveData(data: List<RowDataReport>, onProgress: (Float) -> Unit = {}) {
        val conn = dbHelper.getConnection() ?: return
        val totalRows = data.size
        if (totalRows == 0) return

        conn.autoCommit = false
        try {
            conn.createStatement().use { it.execute("DELETE FROM excel_data") }

            conn.prepareStatement(
                """
                INSERT INTO excel_data (
                    row_index, sales_store, customer_name, invoice_number, customer_number,
                    unit_bisnis, trx_date, description, amount, tax_code, ppn, total_amount_ppn
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                data.forEachIndexed { rowIndex, row ->
                    stmt.setInt(1, rowIndex)
                    stmt.setString(2, row.salesStore)
                    stmt.setString(3, row.customerName)
                    stmt.setString(4, row.invoiceNumber)
                    stmt.setString(5, row.customerNumber)
                    stmt.setString(6, row.unitBisnis)
                    stmt.setString(7, row.trxDate)
                    stmt.setString(8, row.description)
                    stmt.setString(9, row.amount)
                    stmt.setString(10, row.taxCode)
                    stmt.setString(11, row.ppn)
                    stmt.setString(12, row.totalAmountPpn)
                    stmt.addBatch()

                    // Flush batch every 500 rows to avoid excessive memory usage
                    if ((rowIndex + 1) % BATCH_SIZE == 0) {
                        stmt.executeBatch()
                        conn.commit()
                        onProgress(((rowIndex + 1).toFloat() / totalRows) * 100f)
                    }
                }

                // Flush remaining rows
                stmt.executeBatch()
                conn.commit()
                onProgress(100f)
            }
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    // ─── Read Operations ─────────────────────────────────────────────

    fun getDataCount(): Int {
        val conn = dbHelper.getConnection() ?: return 0
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) AS cnt FROM excel_data").use { rs ->
                return if (rs.next()) rs.getInt("cnt") else 0
            }
        }
    }

    fun getAllData(): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        return conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT * FROM excel_data ORDER BY row_index").use { rs ->
                rs.toRowList()
            }
        }
    }

    fun getPaginatedData(
        offset: Int,
        limit: Int,
        sorts: List<Pair<String, Boolean>> = emptyList()
    ): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val orderClause = buildOrderClause(sorts)

        val query = "SELECT * FROM excel_data ORDER BY $orderClause LIMIT $limit OFFSET $offset"
        return conn.createStatement().use { stmt ->
            stmt.executeQuery(query).use { rs -> rs.toRowList() }
        }
    }

    // ─── Filter Operations ───────────────────────────────────────────

    fun filterData(filters: List<Pair<String, String>>): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val (whereClause, params) = buildWhereClause(filters)

        val query = "SELECT * FROM excel_data $whereClause ORDER BY row_index"
        return conn.prepareStatement(query).use { pstmt ->
            pstmt.bindParams(params)
            pstmt.executeQuery().use { rs -> rs.toRowList() }
        }
    }

    fun filterDataPaginated(
        filters: List<Pair<String, String>>,
        offset: Int,
        limit: Int,
        sorts: List<Pair<String, Boolean>> = emptyList()
    ): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val (whereClause, params) = buildWhereClause(filters)
        val orderClause = buildOrderClause(sorts)

        val query = "SELECT * FROM excel_data $whereClause ORDER BY $orderClause LIMIT $limit OFFSET $offset"
        return conn.prepareStatement(query).use { pstmt ->
            pstmt.bindParams(params)
            pstmt.executeQuery().use { rs -> rs.toRowList() }
        }
    }

    fun getFilteredCount(filters: List<Pair<String, String>>): Int {
        val conn = dbHelper.getConnection() ?: return 0
        val (whereClause, params) = buildWhereClause(filters)

        val query = "SELECT COUNT(*) AS cnt FROM excel_data $whereClause"
        return conn.prepareStatement(query).use { pstmt ->
            pstmt.bindParams(params)
            pstmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt("cnt") else 0
            }
        }
    }

    fun getUniqueValues(
        column: String,
        constraints: List<Pair<String, String>> = emptyList()
    ): List<String> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val dbCol = getColumnName(column)

        val activeConstraints = constraints.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
        val whereParts = mutableListOf("$dbCol IS NOT NULL", "$dbCol != ''")
        val params = mutableListOf<String>()

        activeConstraints.forEach { (header, value) ->
            val filterDbCol = getColumnName(header)
            whereParts.add("$filterDbCol LIKE ?")
            params.add("%$value%")
        }

        val whereClause = "WHERE " + whereParts.joinToString(" AND ")
        val query = "SELECT DISTINCT $dbCol FROM excel_data $whereClause ORDER BY $dbCol ASC"

        return conn.prepareStatement(query).use { stmt ->
            stmt.bindParams(params)
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val v = rs.getString(1) ?: continue
                        add(if (v.endsWith(".0")) v.removeSuffix(".0") else v)
                    }
                }
            }
        }
    }

    // ─── Pivot Aggregation (SQL-level) ───────────────────────────────

    fun getPivotSummaries(filters: List<Pair<String, String>> = emptyList()): List<PivotSummary> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val (whereClause, params) = buildWhereClause(filters)

        val query = """
            SELECT
                sales_store,
                invoice_number,
                customer_number,
                customer_name,
                unit_bisnis,
                trx_date,
                SUM(CAST(COALESCE(NULLIF(total_amount_ppn, ''), '0') AS REAL)) AS total
            FROM excel_data
            $whereClause
            GROUP BY invoice_number
            ORDER BY invoice_number
        """.trimIndent()

        return conn.prepareStatement(query).use { pstmt ->
            pstmt.bindParams(params)
            pstmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            PivotSummary(
                                salesStore = rs.getString("sales_store") ?: "",
                                invoiceNumber = rs.getString("invoice_number") ?: "Unknown",
                                customerNumber = rs.getString("customer_number") ?: "",
                                customerName = rs.getString("customer_name") ?: "",
                                unitBisnis = rs.getString("unit_bisnis") ?: "",
                                trxDate = rs.getString("trx_date") ?: "",
                                totalAmountPpn = rs.getDouble("total")
                            )
                        )
                    }
                }
            }
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────

    private fun getColumnName(header: String): String =
        RowDataReport.HEADER_TO_DB_COLUMN[header] ?: "id"

    private fun buildOrderClause(sorts: List<Pair<String, Boolean>>): String =
        if (sorts.isNotEmpty()) {
            sorts.joinToString(", ") { "${getColumnName(it.first)} ${if (it.second) "ASC" else "DESC"}" }
        } else {
            "row_index"
        }

    private fun buildWhereClause(filters: List<Pair<String, String>>): Pair<String, List<String>> {
        if (filters.isEmpty()) return "" to emptyList()

        val clause = "WHERE " + filters.joinToString(" AND ") { "${getColumnName(it.first)} LIKE ?" }
        val params = filters.map { "%${it.second}%" }
        return clause to params
    }

    private fun PreparedStatement.bindParams(params: List<String>) {
        params.forEachIndexed { index, value ->
            setString(index + 1, value)
        }
    }

    private fun ResultSet.toRowList(): List<RowDataReport> = buildList {
        while (this@toRowList.next()) {
            add(mapRow(this@toRowList))
        }
    }

    private fun mapRow(rs: ResultSet) = RowDataReport(
        salesStore = rs.getString("sales_store"),
        customerName = rs.getString("customer_name"),
        invoiceNumber = rs.getString("invoice_number"),
        customerNumber = rs.getString("customer_number"),
        unitBisnis = rs.getString("unit_bisnis"),
        trxDate = rs.getString("trx_date"),
        description = rs.getString("description"),
        amount = rs.getString("amount"),
        taxCode = rs.getString("tax_code"),
        ppn = rs.getString("ppn"),
        totalAmountPpn = rs.getString("total_amount_ppn")
    )

    companion object {
        private const val BATCH_SIZE = 500
    }
}
