package id.mhmfajar.excelapplicationapp.ui.components

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Opens a file picker dialog filtered for Excel files (.xlsx, .xls).
 * Returns the selected File, or null if cancelled.
 */
fun pickFile(): File? {
    val chooser = JFileChooser().apply {
        fileFilter = FileNameExtensionFilter("Excel Files", "xlsx", "xls")
    }
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

/**
 * Opens a save-file dialog. Appends .xlsx extension if not present.
 * Returns the target File, or null if cancelled.
 */
fun saveFile(): File? {
    val chooser = JFileChooser()
    val result = chooser.showSaveDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        var file = chooser.selectedFile
        if (!file.name.endsWith(".xlsx")) {
            file = File(file.absolutePath + ".xlsx")
        }
        file
    } else null
}
