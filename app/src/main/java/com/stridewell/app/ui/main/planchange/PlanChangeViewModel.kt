package com.stridewell.app.ui.main.planchange

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class PlanChangeViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Empty : ScreenState
        data class Error(val message: String) : ScreenState
        data class Loaded(val record: DecisionRecord) : ScreenState
    }

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val preloadedRecord: DecisionRecord? = savedStateHandle
        .get<String>(Route.PlanChange.argRecord)
        ?.let(Uri::decode)
        ?.let { json ->
            runCatching {
                Json.decodeFromString(DecisionRecord.serializer(), json)
            }.getOrNull()
        }

    init {
        if (preloadedRecord != null) {
            _screenState.value = ScreenState.Loaded(preloadedRecord)
        } else {
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading
            when (val result = planRepository.latestDecision()) {
                is ApiResult.Success -> {
                    _screenState.value = ScreenState.Loaded(result.data.decision_record)
                }
                is ApiResult.Error -> {
                    _screenState.value = if (result.status == 404) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Error(result.message)
                    }
                }
            }
        }
    }

    fun markSeen(onDone: () -> Unit) {
        viewModelScope.launch {
            val versionId = (screenState.value as? ScreenState.Loaded)?.record?.to_plan_version_id
            planRepository.markPlanChangeSeen(versionId)
            onDone()
        }
    }
}
