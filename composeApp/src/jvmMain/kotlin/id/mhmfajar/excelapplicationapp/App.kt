package id.mhmfajar.excelapplicationapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import id.mhmfajar.excelapplicationapp.presentation.MainViewModel
import id.mhmfajar.excelapplicationapp.ui.MainScreen
import id.mhmfajar.excelapplicationapp.ui.theme.AppTheme

@Composable
fun App() {
    val vm = remember { MainViewModel() }

    AppTheme {
        MainScreen(vm)
    }
}