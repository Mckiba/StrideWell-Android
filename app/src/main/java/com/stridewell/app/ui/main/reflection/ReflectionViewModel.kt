package com.stridewell.app.ui.main.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.ReflectionRepository
import com.stridewell.app.model.ReflectionSubmission
import com.stridewell.app.model.SorenessEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SorenessFormEntry(
    val id: String = UUID.randomUUID().toString(),
    val location: String = "",
    val score: Float = 5f
) {
    fun toSubmission(): SorenessEntry? {
        val trimmed = location.trim()
        return if (trimmed.isBlank()) null else SorenessEntry(trimmed, score.toInt())
    }
}

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val reflectionRepository: ReflectionRepository
) : ViewModel() {

    data class UiState(
        val fatigue: Float = 5f,
        val sleepQuality: Float = 5f,
        val mood: com.stridewell.app.ui.components.MoodOption = com.stridewell.app.ui.components.MoodOption.Neutral,
        val sorenessEntries: List<SorenessFormEntry> = emptyList(),
        val freeText: String = "",
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val showSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setFatigue(value: Float) = _uiState.update { it.copy(fatigue = value) }
    fun setSleepQuality(value: Float) = _uiState.update { it.copy(sleepQuality = value) }
    fun setMood(value: com.stridewell.app.ui.components.MoodOption) = _uiState.update { it.copy(mood = value) }
    fun setFreeText(value: String) = _uiState.update { it.copy(freeText = value.take(2000)) }

    fun addSorenessEntry() = _uiState.update {
        it.copy(sorenessEntries = it.sorenessEntries + SorenessFormEntry())
    }

    fun removeSorenessEntry(id: String) = _uiState.update {
        it.copy(sorenessEntries = it.sorenessEntries.filterNot { entry -> entry.id == id })
    }

    fun updateSorenessLocation(id: String, value: String) = _uiState.update {
        it.copy(sorenessEntries = it.sorenessEntries.map { entry ->
            if (entry.id == id) entry.copy(location = value) else entry
        })
    }

    fun updateSorenessScore(id: String, value: Float) = _uiState.update {
        it.copy(sorenessEntries = it.sorenessEntries.map { entry ->
            if (entry.id == id) entry.copy(score = value) else entry
        })
    }

    fun submit(onSuccessFinished: () -> Unit) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val sorenessPayload = current.sorenessEntries.mapNotNull { it.toSubmission() }.ifEmpty { null }
            val trimmed = current.freeText.trim().ifBlank { null }
            val submission = ReflectionSubmission(
                fatigue = current.fatigue.toInt(),
                sleep_quality = current.sleepQuality.toInt(),
                mood = current.mood.score,
                soreness = sorenessPayload,
                free_text = trimmed,
                related_run_id = null
            )

            when (val result = reflectionRepository.submit(submission)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, showSuccess = true) }
                    delay(1500)
                    onSuccessFinished()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
            }
        }
    }
}
