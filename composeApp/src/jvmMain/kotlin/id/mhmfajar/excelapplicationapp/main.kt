package id.mhmfajar.excelapplicationapp

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ReportFlux",
        icon = painterResource("drawable/report_flux_icon.png")
    ) {
        App()
    }
}