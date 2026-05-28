package com.example.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.ExtractedSubscription
import com.example.data.Category
import com.example.data.Subscription
import com.example.data.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExtractionState {
    object Idle : ExtractionState
    object Loading : ExtractionState
    data class Success(val extracted: ExtractedSubscription) : ExtractionState
    data class Error(val message: String) : ExtractionState
}

sealed interface OptimizationState {
    object Idle : OptimizationState
    object Loading : OptimizationState
    data class Success(val recommendations: String) : OptimizationState
    data class Error(val message: String) : OptimizationState
}

class SubscriptionViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    // Observe active subscriptions reactively
    val subscriptions: StateFlow<List<Subscription>> = repository.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe active custom categories reactively
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Prepopulate standard categories on first run
        viewModelScope.launch {
            try {
                val existing = repository.allCategories.first()
                if (existing.isEmpty()) {
                    val defaults = listOf("Entertainment", "Fitness", "Utilities", "SaaS", "Other")
                    defaults.forEach { name ->
                        repository.insertCategory(Category(name = name))
                    }
                }
            } catch (e: Exception) {
                // Ignore seed errors
            }
        }
    }

    // SMS/OCR Text & Image Extraction State
    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    // Spending Recommendations/Optimization State
    private val _optimizationState = MutableStateFlow<OptimizationState>(OptimizationState.Idle)
    val optimizationState: StateFlow<OptimizationState> = _optimizationState.asStateFlow()

    fun saveSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.insertSubscription(subscription)
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            repository.deleteSubscriptionById(id)
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
        }
    }

    fun extractSubscriptionFromText(rawText: String) {
        if (rawText.isBlank()) {
            _extractionState.value = ExtractionState.Error("Please paste or type some text first!")
            return
        }
        _extractionState.value = ExtractionState.Loading
        viewModelScope.launch {
            try {
                val extracted = repository.extractSubscriptionFromText(rawText)
                if (extracted != null) {
                    _extractionState.value = ExtractionState.Success(extracted)
                } else {
                    _extractionState.value = ExtractionState.Error("Could not extract any subscription details from this text.")
                }
            } catch (e: Exception) {
                _extractionState.value = ExtractionState.Error(e.localizedMessage ?: "Network/OCR Scan Error")
            }
        }
    }

    fun extractSubscriptionFromImage(bitmap: Bitmap, helperText: String) {
        _extractionState.value = ExtractionState.Loading
        viewModelScope.launch {
            try {
                val extracted = repository.extractSubscriptionFromImage(bitmap, helperText)
                if (extracted != null) {
                    _extractionState.value = ExtractionState.Success(extracted)
                } else {
                    _extractionState.value = ExtractionState.Error("Could not parse subscription details from this visual.")
                }
            } catch (e: Exception) {
                _extractionState.value = ExtractionState.Error(e.localizedMessage ?: "OCR visual parsing error")
            }
        }
    }

    fun clearExtractionState() {
        _extractionState.value = ExtractionState.Idle
    }

    fun generateBudgetRecommendations() {
        _optimizationState.value = OptimizationState.Loading
        viewModelScope.launch {
            try {
                val currentList = subscriptions.value
                val recommendations = repository.getOptimizationRecommendations(currentList)
                _optimizationState.value = OptimizationState.Success(recommendations)
            } catch (e: Exception) {
                _optimizationState.value = OptimizationState.Error(e.localizedMessage ?: "Failing to optimize balance.")
            }
        }
    }

    fun clearOptimizationState() {
        _optimizationState.value = OptimizationState.Idle
    }
}

class SubscriptionViewModelFactory(
    private val repository: SubscriptionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
