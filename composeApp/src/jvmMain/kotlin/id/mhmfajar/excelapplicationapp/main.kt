package id.mhmfajar.excelapplicationapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import reportflux.composeapp.generated.resources.Res
import reportflux.composeapp.generated.resources.report_flux_icon

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ReportFlux",
        icon = painterResource(Res.drawable.report_flux_icon)
    ) {
        App()
    }
}