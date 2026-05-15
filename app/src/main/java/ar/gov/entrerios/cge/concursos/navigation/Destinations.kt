package ar.gov.entrerios.cge.concursos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val RESULTS = "results"
    const val KEYWORDS = "keywords"
    const val SETTINGS = "settings"

    const val DETAILS_BASE = "details"
    const val DETAILS_ARG = "concursoId"
    const val DETAILS = "$DETAILS_BASE/{$DETAILS_ARG}"

    fun details(id: Long) = "$DETAILS_BASE/$id"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home(Routes.HOME, "Relevantes", Icons.Outlined.Home),
    Results(Routes.RESULTS, "Todos", Icons.Outlined.Article),
    Keywords(Routes.KEYWORDS, "Keywords", Icons.Outlined.Tag),
    Settings(Routes.SETTINGS, "Ajustes", Icons.Outlined.Settings)
}
