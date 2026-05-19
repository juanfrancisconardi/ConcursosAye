package ar.gov.entrerios.cge.concursos.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ar.gov.entrerios.cge.concursos.core.model.SyncMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.gov.entrerios.cge.concursos.R
import ar.gov.entrerios.cge.concursos.ui.components.ConcursoCard
import ar.gov.entrerios.cge.concursos.ui.components.EmptyState
import ar.gov.entrerios.cge.concursos.ui.components.RefreshHintArrow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConcursoClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val concursos by viewModel.concursos.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val syncInProgress by viewModel.syncInProgress.collectAsStateWithLifecycle()
    val syncMode by viewModel.syncMode.collectAsStateWithLifecycle()
    var refreshHintDismissed by rememberSaveable { mutableStateOf(false) }

    val isSyncing = isRefreshing || syncInProgress
    val showRefreshHint = !refreshHintDismissed && concursos.isEmpty() && !isSyncing

    val emptyDescription = when {
        isSyncing -> stringResource(R.string.home_sync_in_progress)
        syncMode == SyncMode.ON_APP_OPEN -> stringResource(R.string.home_empty_auto_sync)
        showRefreshHint -> stringResource(R.string.home_empty_tap_refresh)
        else -> stringResource(R.string.home_empty_default)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_home)) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showRefreshHint) {
                            RefreshHintArrow(modifier = Modifier.size(22.dp))
                        }
                        IconButton(
                            onClick = {
                                refreshHintDismissed = true
                                viewModel.refresh()
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.action_refresh)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            if (concursos.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_concursos),
                    description = emptyDescription
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = concursos, key = { it.id }) { concurso ->
                        ConcursoCard(concurso = concurso, onClick = onConcursoClick)
                    }
                }
            }
        }
    }
}
