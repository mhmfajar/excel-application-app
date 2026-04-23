package id.mhmfajar.excelapplicationapp.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*

@Composable
fun InfiniteScrollEffect(
    listState: LazyListState,
    buffer: Int = 2,
    onEndReached: () -> Unit
) {
    val isEndReached by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0 || visibleItemsInfo.isEmpty()) false
            else visibleItemsInfo.last().index >= layoutInfo.totalItemsCount - buffer
        }
    }

    LaunchedEffect(isEndReached) {
        if (isEndReached) onEndReached()
    }
}
