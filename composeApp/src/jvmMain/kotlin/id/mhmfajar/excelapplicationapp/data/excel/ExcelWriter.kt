package id.mhmfajar.excelapplicationapp.data.excel

import id.mhmfajar.excelapplicationapp.domain.model.PivotSummary
import id.mhmfajar.excelapplicationapp.domain.model.RowDataReport
import id.mhmfajar.excelapplicationapp.util.DateFormatter
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

class ExcelWriter {

    fun write(headers: List<String>, data: List<RowDataReport>, pivotData: List<PivotSummary>, file: File) {
        if (headers.isEmpty()) return

        val workbook = XSSFWorkbook()

        try {
            val styles = createStyles(workbook)

            writeReportSheet(workbook, headers, data, styles)
            writePivotSheet(workbook, pivotData, styles)

            file.outputStream().use { workbook.write(it) }
        } finally {
            workbook.close()
        }
    }

    // ─── Report Sheet (Sheet 1) ───────────────────────────────────────

    private fun writeReportSheet(
        workbook: XSSFWorkbook,
        headers: List<String>,
        data: List<RowDataReport>,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet("Report View")

        // Header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("NO")
        headers.forEachIndexed { i, h -> headerRow.createCell(i + 1).setCellValue(h) }

        // Data rows
        var currentRowIdx = 1
        var groupCounter = 1
        var lastInvoice: String? = ""
        var groupStartRowIdx = 1

        data.forEachIndexed { idx, rowData ->
            if (idx > 0 && rowData.invoiceNumber != lastInvoice) {
                if (currentRowIdx - 1 > groupStartRowIdx) {
                    sheet.addMergedRegion(CellRangeAddress(groupStartRowIdx, currentRowIdx - 1, 0, 0))
                }
                sheet.createRow(currentRowIdx)
                currentRowIdx++
                groupCounter++
                groupStartRowIdx = currentRowIdx
            }

            val row = sheet.createRow(currentRowIdx)
            if (idx == 0 || rowData.invoiceNumber != lastInvoice) {
                row.createCell(0).apply {
                    setCellValue(groupCounter.toDouble())
                    setCellStyle(styles.noCenter)
                }
            }

            headers.forEachIndexed { colIdx, key ->
                val value = if (key == "TRX_DATE") DateFormatter.format(rowData.getFieldByHeader(key))
                else rowData.getFieldByHeader(key)

                val cell = row.createCell(colIdx + 1)
                setCellValueByType(cell, key, value, styles)
            }

            lastInvoice = rowData.invoiceNumber
            currentRowIdx++
        }

        // Merge last group
        if (currentRowIdx - 1 > groupStartRowIdx) {
            sheet.addMergedRegion(CellRangeAddress(groupStartRowIdx, currentRowIdx - 1, 0, 0))
        }

        // Auto-size columns
        for (i in 0..headers.size) sheet.autoSizeColumn(i)
    }

    // ─── Pivot Sheet (Sheet 2) ────────────────────────────────────────

    private fun writePivotSheet(
        workbook: XSSFWorkbook,
        pivotData: List<PivotSummary>,
        styles: ExcelStyles
    ) {
        val sheet = workbook.createSheet("Pivot View")
        val pivotCols = listOf(
            "SALES_STORE", "INVOICE_NUMBER", "CUSTOMER_NUMBER",
            "CUSTOMER_NAME", "UNIT_BISNIS", "TRX_DATE", "Total"
        )

        // Header
        val headerRow = sheet.createRow(0)
        pivotCols.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                setCellStyle(styles.pivotHeader)
            }
        }

        // Data rows
        pivotData.forEachIndexed { i, summary ->
            val row = sheet.createRow(i + 1)
            val values = listOf(
                summary.salesStore, summary.invoiceNumber, summary.customerNumber,
                summary.customerName, summary.unitBisnis, DateFormatter.format(summary.trxDate),
                summary.totalAmountPpn
            )

            values.forEachIndexed { colIdx, value ->
                val cell = row.createCell(colIdx)
                when {
                    colIdx == 6 -> { // Total column
                        cell.setCellValue(value as Double)
                        cell.setCellStyle(styles.pivotAccounting)
                    }
                    colIdx == 0 || colIdx == 2 -> { // SALES_STORE, CUSTOMER_NUMBER
                        val d = value.toString().toDoubleOrNull()
                        if (d != null) cell.setCellValue(d.toLong().toDouble()) else cell.setCellValue(value.toString())
                        cell.setCellStyle(styles.pivotData)
                    }
                    else -> {
                        when (value) {
                            is Double -> cell.setCellValue(value)
                            is String? -> cell.setCellValue(value ?: "")
                        }
                        cell.setCellStyle(styles.pivotData)
                    }
                }
            }
        }

        // Grand Total
        val totalRowIdx = pivotData.size + 1
        val totalRow = sheet.createRow(totalRowIdx)

        for (i in 0..5) {
            totalRow.createCell(i).setCellStyle(styles.totalLabel)
        }
        totalRow.getCell(0).setCellValue("Grand Total")
        sheet.addMergedRegion(CellRangeAddress(totalRowIdx, totalRowIdx, 0, 5))

        totalRow.createCell(6).apply {
            setCellFormula("SUM(G2:G${totalRowIdx})")
            setCellStyle(styles.totalValue)
        }

        for (i in 0..pivotCols.size) sheet.autoSizeColumn(i)
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private fun setCellValueByType(cell: Cell, key: String, value: String?, styles: ExcelStyles) {
        val str = value ?: ""
        val doubleVal = str.toDoubleOrNull()

        when {
            key in ACCOUNTING_COLUMNS -> {
                cell.setCellValue(doubleVal ?: 0.0)
                cell.setCellStyle(styles.accounting)
            }
            key in ID_COLUMNS -> {
                if (doubleVal != null) cell.setCellValue(doubleVal.toLong().toDouble())
                else cell.setCellValue(str)
            }
            else -> {
                if (doubleVal != null) cell.setCellValue(doubleVal)
                else cell.setCellValue(str)
            }
        }
    }

    // ─── Styles ───────────────────────────────────────────────────────

    private fun createStyles(workbook: XSSFWorkbook): ExcelStyles {
        val accountingFormat = workbook.createDataFormat()
            .getFormat("_(* #,##0_);_(* (#,##0);_(* \"-\"??_);_(@_)")

        val noCenter = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

        val accounting = workbook.createCellStyle().apply {
            setDataFormat(accountingFormat)
        }

        val thinBorder: (CellStyle) -> Unit = { style ->
            style.borderTop = BorderStyle.THIN
            style.borderBottom = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
        }

        val boldFont = workbook.createFont().apply { bold = true }

        val pivotHeader = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            thinBorder(this)
            setFont(boldFont)
        }

        val pivotData = workbook.createCellStyle().apply {
            thinBorder(this)
        }

        val pivotAccounting = workbook.createCellStyle().apply {
            cloneStyleFrom(pivotData)
            setDataFormat(accountingFormat)
        }

        val totalLabel = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            thinBorder(this)
            setFont(boldFont)
        }

        val totalValue = workbook.createCellStyle().apply {
            cloneStyleFrom(totalLabel)
            alignment = HorizontalAlignment.RIGHT
            setDataFormat(accountingFormat)
        }

        return ExcelStyles(
            noCenter = noCenter,
            accounting = accounting,
            pivotHeader = pivotHeader,
            pivotData = pivotData,
            pivotAccounting = pivotAccounting,
            totalLabel = totalLabel,
            totalValue = totalValue
        )
    }

    companion object {
        private val ACCOUNTING_COLUMNS = setOf("AMOUNT", "PPN", "TOTAL_AMOUNT+PPN")
        private val ID_COLUMNS = setOf("SALES_STORE", "CUSTOMER_NUMBER")
    }
}

/**
 * Groups all pre-created cell styles used during Excel writing.
 */
private data class ExcelStyles(
    val noCenter: CellStyle,
    val accounting: CellStyle,
    val pivotHeader: CellStyle,
    val pivotData: CellStyle,
    val pivotAccounting: CellStyle,
    val totalLabel: CellStyle,
    val totalValue: CellStyle
)