package ar.gov.entrerios.cge.concursos

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ar.gov.entrerios.cge.concursos.navigation.ConcursosNavHost
import ar.gov.entrerios.cge.concursos.navigation.Routes
import ar.gov.entrerios.cge.concursos.navigation.TopLevelDestination
import ar.gov.entrerios.cge.concursos.ads.AdBanner
import ar.gov.entrerios.cge.concursos.ui.MainViewModel
import ar.gov.entrerios.cge.concursos.ui.components.SyncReportHost
import ar.gov.entrerios.cge.concursos.ui.theme.ConcursosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        maybeRequestNotificationPermission()

        setContent {
            val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()

            ConcursosTheme(darkMode = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        viewModel = viewModel,
                        onOpenUrl = ::openExternal,
                        initialRoute = intent?.dataString?.toDeepLinkRoute()
                    )
                }
            }
        }
    }

    private fun openExternal(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* aceptado o no: la app sigue funcionando, solo afecta notificaciones */ }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun String?.toDeepLinkRoute(): String? {
    val uri = this?.let { Uri.parse(it) } ?: return null
    if (uri.scheme == "concursoscge" && uri.host == "detalle") {
        val id = uri.lastPathSegment?.toLongOrNull() ?: return null
        return Routes.details(id)
    }
    return null
}

@Composable
private fun AppRoot(
    viewModel: MainViewModel,
    onOpenUrl: (String) -> Unit,
    initialRoute: String?
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    AdBanner()
                    BottomBar(navController = navController, currentRoute = currentRoute)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ConcursosNavHost(
                navController = navController,
                startDestination = Routes.HOME,
                onOpenUrl = onOpenUrl
            )
        }

        LaunchedEffect(initialRoute) {
            initialRoute?.let { navController.navigate(it) }
        }
    }

    // Popup global de reporte de sincronización (foreground).
    SyncReportHost(
        events = viewModel.syncEvents,
        syncInProgress = viewModel.syncInProgress,
        onOpenConcurso = { id -> navController.navigate(Routes.details(id)) }
    )

    // Sync al abrir: solo después de que la UI escucha el bus (evita perder el evento Started).
    LaunchedEffect(Unit) {
        viewModel.runSyncOnAppOpenIfNeeded()
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        TopLevelDestination.entries.forEach { dest ->
            val selected = navController.currentBackStackEntry?.destination?.hierarchy
                ?.any { it.route == dest.route } == true || currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}
