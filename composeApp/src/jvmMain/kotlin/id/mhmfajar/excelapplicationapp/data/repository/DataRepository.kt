package id.mhmfajar.excelapplicationapp.data.repository

import id.mhmfajar.excelapplicationapp.data.database.DatabaseHelper
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import java.sql.ResultSet

class DataRepository(private val dbHelper: DatabaseHelper) {

    fun clearDatabase() {
        dbHelper.dropTables()
        dbHelper.createTables()
    }

    fun saveData(data: List<RowDataReport>, onProgress: (Float) -> Unit = {}) {
        val conn = dbHelper.getConnection() ?: return

        conn.autoCommit = false

        try {
            // Clear existing data
            conn.createStatement().use { it.execute("DELETE FROM excel_data") }

            // Insert data rows
            val totalRows = data.size
            conn.prepareStatement(
                """
                INSERT INTO excel_data (
                    row_index, sales_store, customer_name, invoice_number, customer_number, 
                    unit_bisnis, trx_date, description, amount, tax_code, ppn, total_amount_ppn
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { insertStmt ->
                data.forEachIndexed { rowIndex, row ->
                    insertStmt.setInt(1, rowIndex)
                    insertStmt.setString(2, row.salesStore)
                    insertStmt.setString(3, row.customerName)
                    insertStmt.setString(4, row.invoiceNumber)
                    insertStmt.setString(5, row.customerNumber)
                    insertStmt.setString(6, row.unitBisnis)
                    insertStmt.setString(7, row.trxDate)
                    insertStmt.setString(8, row.description)
                    insertStmt.setString(9, row.amount)
                    insertStmt.setString(10, row.taxCode)
                    insertStmt.setString(11, row.ppn)
                    insertStmt.setString(12, row.totalAmountPpn)
                    insertStmt.addBatch()

                    // Report progress
                    if ((rowIndex + 1) % 100 == 0 || rowIndex == totalRows - 1) {
                        val progress = ((rowIndex + 1).toFloat() / totalRows) * 100f
                        onProgress(progress)
                    }
                }

                insertStmt.executeBatch()
            }
            conn.commit()
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    private fun mapRow(rs: ResultSet): RowDataReport {
        return RowDataReport(
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
    }

    private fun getColumnName(header: String): String {
        return when(header) {
            "SALES_STORE" -> "sales_store"
            "CUSTOMER_NAME" -> "customer_name"
            "INVOICE_NUMBER" -> "invoice_number"
            "CUSTOMER_NUMBER" -> "customer_number"
            "UNIT_BISNIS" -> "unit_bisnis"
            "TRX_DATE" -> "trx_date"
            "DESCRIPTION" -> "description"
            "AMOUNT" -> "amount"
            "TAX_CODE" -> "tax_code"
            "PPN" -> "ppn"
            "TOTAL_AMOUNT+PPN" -> "total_amount_ppn"
            else -> "id"
        }
    }

    fun getDataCount(): Int {
        val conn = dbHelper.getConnection() ?: return 0
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) as count FROM excel_data").use { rs ->
                return if (rs.next()) rs.getInt("count") else 0
            }
        }
    }

    fun getAllData(): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val result = mutableListOf<RowDataReport>()

        val query = "SELECT * FROM excel_data ORDER BY row_index"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(query).use { rs ->
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }

        return result
    }

    fun getPaginatedData(offset: Int, limit: Int, sorts: List<Pair<String, Boolean>> = emptyList()): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val result = mutableListOf<RowDataReport>()

        val orderClause = if (sorts.isNotEmpty()) {
            sorts.joinToString(", ") { "${getColumnName(it.first)} " + if (it.second) "ASC" else "DESC" }
        } else {
            "row_index"
        }

        val query = "SELECT * FROM excel_data ORDER BY $orderClause LIMIT $limit OFFSET $offset"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(query).use { rs ->
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }

        return result
    }

    fun filterData(filters: List<Pair<String, String>>): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val result = mutableListOf<RowDataReport>()

        val whereClause = if (filters.isNotEmpty()) "WHERE " + filters.joinToString(" AND ") { "${getColumnName(it.first)} LIKE ?" } else ""
        val query = "SELECT * FROM excel_data $whereClause ORDER BY row_index"
        conn.prepareStatement(query).use { pstmt ->
            filters.forEachIndexed { index, pair -> 
                pstmt.setString(index + 1, "%${pair.second}%")
            }
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }

        return result
    }

    fun filterDataPaginated(filters: List<Pair<String, String>>, offset: Int, limit: Int, sorts: List<Pair<String, Boolean>> = emptyList()): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val result = mutableListOf<RowDataReport>()

        val orderClause = if (sorts.isNotEmpty()) {
            sorts.joinToString(", ") { "${getColumnName(it.first)} " + if (it.second) "ASC" else "DESC" }
        } else {
            "row_index"
        }

        val whereClause = if (filters.isNotEmpty()) "WHERE " + filters.joinToString(" AND ") { "${getColumnName(it.first)} LIKE ?" } else ""
        val query = "SELECT * FROM excel_data $whereClause ORDER BY $orderClause LIMIT $limit OFFSET $offset"
        conn.prepareStatement(query).use { pstmt ->
            filters.forEachIndexed { index, pair -> 
                pstmt.setString(index + 1, "%${pair.second}%")
            }
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }

        return result
    }

    fun getFilteredCount(filters: List<Pair<String, String>>): Int {
        val conn = dbHelper.getConnection() ?: return 0
        
        val whereClause = if (filters.isNotEmpty()) "WHERE " + filters.joinToString(" AND ") { "${getColumnName(it.first)} LIKE ?" } else ""
        val query = "SELECT COUNT(*) as count FROM excel_data $whereClause"
        
        conn.prepareStatement(query).use { pstmt ->
            filters.forEachIndexed { index, pair -> 
                pstmt.setString(index + 1, "%${pair.second}%")
            }
            pstmt.executeQuery().use { rs ->
                return if (rs.next()) rs.getInt("count") else 0
            }
        }
    }

    fun sortData(column: String, ascending: Boolean): List<RowDataReport> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val result = mutableListOf<RowDataReport>()

        val order = if (ascending) "ASC" else "DESC"
        val dbColumn = getColumnName(column)
        val query = "SELECT * FROM excel_data ORDER BY $dbColumn $order"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(query).use { rs ->
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }

        return result
    }

    fun getUniqueValues(column: String, constraints: List<Pair<String, String>> = emptyList()): List<String> {
        val conn = dbHelper.getConnection() ?: return emptyList()
        val result = mutableListOf<String>()
        val dbCol = getColumnName(column)
        if (dbCol.isEmpty()) return emptyList()

        val activeConstraints = constraints.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
        val whereParts = mutableListOf("$dbCol IS NOT NULL", "$dbCol != ''")
        activeConstraints.forEach {
            val filterDbCol = getColumnName(it.first)
            if (filterDbCol.isNotEmpty()) {
                whereParts.add("$filterDbCol LIKE ?")
            }
        }

        val whereClause = "WHERE " + whereParts.joinToString(" AND ")
        val query = "SELECT DISTINCT $dbCol FROM excel_data $whereClause ORDER BY $dbCol ASC"
        
        conn.prepareStatement(query).use { stmt ->
            var indexOffset = 1
            activeConstraints.forEach {
                val filterDbCol = getColumnName(it.first)
                if (filterDbCol.isNotEmpty()) {
                    stmt.setString(indexOffset++, "%${it.second}%")
                }
            }
            
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    var v = rs.getString(1)
                    if (v != null) {
                        if (v.endsWith(".0")) {
                            v = v.removeSuffix(".0")
                        }
                        result.add(v)
                    }
                }
            }
        }
        return result
    }
}
