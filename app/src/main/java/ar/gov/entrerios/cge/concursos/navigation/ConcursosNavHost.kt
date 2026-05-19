package ar.gov.entrerios.cge.concursos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ar.gov.entrerios.cge.concursos.feature.details.DetailsScreen
import ar.gov.entrerios.cge.concursos.feature.home.HomeScreen
import ar.gov.entrerios.cge.concursos.feature.keywords.KeywordsScreen
import ar.gov.entrerios.cge.concursos.feature.results.ResultsScreen
import ar.gov.entrerios.cge.concursos.feature.settings.SettingsScreen

@Composable
fun ConcursosNavHost(
    navController: NavHostController,
    startDestination: String = Routes.HOME,
    onOpenUrl: (String) -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.HOME) {
            HomeScreen(
                onConcursoClick = { id -> navController.navigate(Routes.details(id)) }
            )
        }

        composable(Routes.RESULTS) {
            ResultsScreen(
                onConcursoClick = { id -> navController.navigate(Routes.details(id)) }
            )
        }

        composable(Routes.KEYWORDS) {
            KeywordsScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onOpenUrl = onOpenUrl)
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument(Routes.DETAILS_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(Routes.DETAILS_ARG) ?: 0L
            DetailsScreen(
                concursoId = id,
                onBack = { navController.popBackStack() },
                onOpenUrl = onOpenUrl
            )
        }
    }
}
