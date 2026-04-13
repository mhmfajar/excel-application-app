package id.mhmfajar.excelapplicationapp.data.excel

import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

class ExcelReader {

    fun read(file: File, onProgress: (Float) -> Unit = {}): List<RowDataReport> {
        val result = mutableListOf<RowDataReport>()

        file.inputStream().use { fis ->
            val workbook = XSSFWorkbook(fis)
            try {
                val sheet = workbook.getSheetAt(0)
                val totalRows = sheet.lastRowNum

                if (totalRows <= 0) return emptyList()

                val headerRow = sheet.getRow(0)
                val headers = headerRow.map { it.toString() }

                val headerIndices = resolveHeaderIndices(headers)

                for (i in 1..totalRows) {
                    val row = sheet.getRow(i) ?: continue

                    result.add(
                        RowDataReport(
                            salesStore = getCellValue(row, headerIndices["SALES_STORE"]),
                            customerName = getCellValue(row, headerIndices["CUSTOMER_NAME"]),
                            invoiceNumber = getCellValue(row, headerIndices["INVOICE_NUMBER"]),
                            customerNumber = getCellValue(row, headerIndices["CUSTOMER_NUMBER"]),
                            unitBisnis = getCellValue(row, headerIndices["UNIT_BISNIS"]),
                            trxDate = getCellValue(row, headerIndices["TRX_DATE"]),
                            description = getCellValue(row, headerIndices["DESCRIPTION"]),
                            amount = getCellValue(row, headerIndices["AMOUNT"]),
                            taxCode = getCellValue(row, headerIndices["TAX_CODE"]),
                            ppn = getCellValue(row, headerIndices["PPN"]),
                            totalAmountPpn = getCellValue(row, headerIndices["TOTAL_AMOUNT+PPN"])
                        )
                    )

                    val progress = (i.toFloat() / totalRows) * 100f
                    onProgress(progress)
                }
            } finally {
                workbook.close()
            }
        }

        return result
    }

    /**
     * Resolves header name → column index mapping.
     * Throws if required headers are missing.
     */
    private fun resolveHeaderIndices(headers: List<String>): Map<String, Int> {
        fun findIndex(name: String): Int {
            return headers.indexOfFirst { header ->
                header.trim().equals(name, ignoreCase = true) ||
                        header.trim().replace(" ", "_").equals(name, ignoreCase = true)
            }
        }

        val indices = REQUIRED_HEADERS.associateWith { findIndex(it) }
        val missing = indices.filter { it.value == -1 }.keys

        if (missing.isNotEmpty()) {
            throw IllegalArgumentException(
                "Invalid layout. Expected headers not found:\n${missing.joinToString("\n- ")}"
            )
        }

        return indices
    }

    private fun getCellValue(row: org.apache.poi.ss.usermodel.Row, idx: Int?): String? {
        if (idx == null || idx == -1) return null
        val cell = row.getCell(idx)
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    cell.numericCellValue.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> null
        }
    }

    companion object {
        private val REQUIRED_HEADERS = listOf(
            "SALES_STORE", "CUSTOMER_NAME", "INVOICE_NUMBER", "CUSTOMER_NUMBER",
            "UNIT_BISNIS", "TRX_DATE", "DESCRIPTION", "AMOUNT", "TAX_CODE",
            "PPN", "TOTAL_AMOUNT+PPN"
        )
    }
}