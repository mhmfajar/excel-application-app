package id.mhmfajar.excelapplicationapp.data.database

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseHelper {

    private var connection: Connection? = null

    fun connect(dbPath: String): Connection {
        if (connection == null || connection?.isClosed == true) {
            File(dbPath).parentFile?.mkdirs()
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        }
        return connection!!
    }

    fun getConnection(): Connection? = connection

    fun createTables() {
        val conn = connection ?: return

        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS excel_data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    row_index INTEGER NOT NULL,
                    sales_store TEXT,
                    customer_name TEXT,
                    invoice_number TEXT,
                    customer_number TEXT,
                    unit_bisnis TEXT,
                    trx_date TEXT,
                    description TEXT,
                    amount TEXT,
                    tax_code TEXT,
                    ppn TEXT,
                    total_amount_ppn TEXT
                )
            """.trimIndent())

            // Indexes on frequently filtered/sorted columns
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_store ON excel_data(sales_store)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_customer_number ON excel_data(customer_number)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_invoice_number ON excel_data(invoice_number)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_row_index ON excel_data(row_index)")
        }
    }

    fun dropTables() {
        connection?.let { conn ->
            try {
                conn.createStatement().use { it.execute("DROP TABLE IF EXISTS excel_data") }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        try {
            connection?.close()
            connection = null
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}

