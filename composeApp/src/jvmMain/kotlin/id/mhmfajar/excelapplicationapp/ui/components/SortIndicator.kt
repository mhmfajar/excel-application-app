package id.mhmfajar.excelapplicationapp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SortIndicator(
    header: String,
    sortRules: List<Pair<String, Boolean>>,
    color: Color
) {
    val sortRule = sortRules.find { it.first == header }
    if (sortRule != null) {
        val index = sortRules.indexOf(sortRule)
        val sortIndicator = (if (sortRule.second) " ↑" else " ↓") +
                if (sortRules.size > 1) "${index + 1}" else ""
        Text(
            text = sortIndicator,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
