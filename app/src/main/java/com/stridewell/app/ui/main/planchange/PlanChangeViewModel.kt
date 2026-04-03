package com.stridewell.app.ui.main.planchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.model.DecisionRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlanChangeViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Empty : ScreenState
        data class Error(val message: String) : ScreenState
        data class Loaded(val record: DecisionRecord) : ScreenState
    }

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    init {
        load()
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
            planRepository.markPlanChangeSeen()
            onDone()
        }
    }
}
