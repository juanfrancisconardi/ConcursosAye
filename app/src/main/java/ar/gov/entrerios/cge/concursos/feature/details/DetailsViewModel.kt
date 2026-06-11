package ar.gov.entrerios.cge.concursos.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.domain.usecase.DeepScanConcursoUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.EnsureConcursoDetailUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.MarkConcursoAsReadUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveConcursoDetailUseCase
import ar.gov.entrerios.cge.concursos.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveConcursoDetailUseCase,
    private val ensureDetail: EnsureConcursoDetailUseCase,
    private val deepScanConcurso: DeepScanConcursoUseCase,
    private val markAsRead: MarkConcursoAsReadUseCase
) : ViewModel() {

    private val concursoId: Long = savedStateHandle.get<Long>(Routes.DETAILS_ARG) ?: 0L
    private val deepScanTriggered = AtomicBoolean(false)

    private val _isDeepScanning = MutableStateFlow(false)
    val isDeepScanning: StateFlow<Boolean> = _isDeepScanning.asStateFlow()

    init {
        viewModelScope.launch { ensureDetail(concursoId) }
    }

    val concurso: StateFlow<Concurso?> = observeDetail(concursoId)
        .onEach { c ->
            if (c == null) return@onEach
            if (!c.isRead) viewModelScope.launch { markAsRead(c.id) }
            if (c.deepScannedAt == null && deepScanTriggered.compareAndSet(false, true)) {
                viewModelScope.launch {
                    _isDeepScanning.value = true
                    try {
                        deepScanConcurso(c.id)
                    } catch (t: Throwable) {
                        Timber.w(t, "Deep scan al abrir detalle falló")
                    } finally {
                        _isDeepScanning.value = false
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
