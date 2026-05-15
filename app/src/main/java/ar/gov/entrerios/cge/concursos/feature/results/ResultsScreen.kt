package ar.gov.entrerios.cge.concursos.feature.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.gov.entrerios.cge.concursos.R
import ar.gov.entrerios.cge.concursos.ui.components.ConcursoCard
import ar.gov.entrerios.cge.concursos.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onConcursoClick: (Long) -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val concursos by viewModel.concursos.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var active by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_results)) })
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DockedSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = { active = false },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                colors = SearchBarDefaults.colors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { /* sin sugerencias por ahora */ }

            if (concursos.isEmpty()) {
                EmptyState(
                    title = "Sin resultados",
                    description = if (query.isBlank())
                        "Todavía no se descargaron concursos. Tirá para refrescar o esperá el próximo chequeo automático."
                    else "No se encontraron concursos que coincidan con \"$query\"."
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
