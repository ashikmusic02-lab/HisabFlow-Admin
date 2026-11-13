package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DashboardData
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardData>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var listenerJob: Job? = null

    init {
        startListening()
    }

    fun startListening() {
        listenerJob?.cancel()
        if (_uiState.value !is UiState.Success) {
            _uiState.value = UiState.Loading
        }
        listenerJob = viewModelScope.launch {
            repository.observeDashboardStats()
                .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    emit(UiState.Error(root.localizedMessage ?: "পরিসংখ্যান লোড করা যায়নি"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val freshData = repository.refreshDashboardFromServer()
                _uiState.value = UiState.Success(freshData)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value !is UiState.Success) {
                    _uiState.value = UiState.Error(e.localizedMessage ?: "রিফ্রেশ ব্যর্থ হয়েছে")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
