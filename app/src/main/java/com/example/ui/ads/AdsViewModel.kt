package com.example.ui.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AdminConfig
import com.example.data.model.AdField
import com.example.data.model.AdsConfig
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdsScreenState(
    val config: AdsConfig = AdsConfig(),
    val intervalInput: String = AdminConfig.DEFAULT_INTERSTITIAL_INTERVAL_SEC.toString(),
    val intervalError: Boolean = false,
    val savingFields: Set<AdField> = emptySet(),
    val isSavingInterval: Boolean = false
)

class AdsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AdsScreenState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AdsScreenState>> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var listenerJob: Job? = null
    private var intervalInitialized = false

    init {
        startListening()
    }

    fun startListening() {
        listenerJob?.cancel()
        if (_uiState.value !is UiState.Success) {
            _uiState.value = UiState.Loading
        }
        listenerJob = viewModelScope.launch {
            repository.observeAdsConfig()
                .map<AdsConfig, UiState<AdsConfig>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    emit(UiState.Error(root.localizedMessage ?: "বিজ্ঞাপন কনফিগ লোড করা যায়নি"))
                }
                .collect { state ->
                    when (state) {
                        is UiState.Loading -> _uiState.value = UiState.Loading
                        is UiState.Error -> _uiState.value = state
                        is UiState.Success -> {
                            val current = (_uiState.value as? UiState.Success)?.data
                            val config = state.data
                            val nextInput = if (!intervalInitialized || current == null) {
                                intervalInitialized = true
                                config.interstitialMinIntervalSec.toString()
                            } else {
                                current.intervalInput
                            }
                            _uiState.value = UiState.Success(
                                AdsScreenState(
                                    config = config,
                                    intervalInput = nextInput,
                                    intervalError = current?.intervalError ?: false,
                                    savingFields = current?.savingFields ?: emptySet(),
                                    isSavingInterval = current?.isSavingInterval ?: false
                                )
                            )
                        }
                    }
                }
        }
    }

    fun toggleAdSwitch(field: AdField, targetEnabled: Boolean) {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        if (field in currentState.savingFields) return

        val previousConfig = currentState.config
        val optimisticConfig = when (field) {
            AdField.BANNER -> previousConfig.copy(bannerEnabled = targetEnabled)
            AdField.NATIVE -> previousConfig.copy(nativeEnabled = targetEnabled)
            AdField.INTERSTITIAL -> previousConfig.copy(interstitialEnabled = targetEnabled)
            AdField.REWARDED -> previousConfig.copy(rewardedEnabled = targetEnabled)
        }

        _uiState.update { state ->
            if (state is UiState.Success) {
                state.copy(
                    data = state.data.copy(
                        config = optimisticConfig,
                        savingFields = state.data.savingFields + field
                    )
                )
            } else state
        }

        viewModelScope.launch {
            try {
                repository.updateAdSwitch(field, targetEnabled, previousConfig)
                _snackbarEvents.tryEmit("বিজ্ঞাপন সেটিংস সংরক্ষিত হয়েছে")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Revert optimistic change on failure
                _uiState.update { state ->
                    if (state is UiState.Success) {
                        state.copy(
                            data = state.data.copy(
                                config = previousConfig,
                                savingFields = state.data.savingFields - field
                            )
                        )
                    } else state
                }
                _snackbarEvents.tryEmit("আপডেট ব্যর্থ: ${e.localizedMessage ?: "আগের মান ফিরিয়ে আনা হয়েছে"}")
                return@launch
            }

            _uiState.update { state ->
                if (state is UiState.Success) {
                    state.copy(data = state.data.copy(savingFields = state.data.savingFields - field))
                } else state
            }
        }
    }

    fun onIntervalInputChanged(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(4)
        val parsed = digitsOnly.toIntOrNull()
        val isValid = parsed != null &&
            parsed in AdminConfig.MIN_INTERSTITIAL_INTERVAL_SEC..AdminConfig.MAX_INTERSTITIAL_INTERVAL_SEC

        _uiState.update { state ->
            if (state is UiState.Success) {
                state.copy(
                    data = state.data.copy(
                        intervalInput = digitsOnly,
                        intervalError = digitsOnly.isNotEmpty() && !isValid
                    )
                )
            } else state
        }
    }

    fun saveInterstitialInterval() {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        if (currentState.isSavingInterval) return

        val parsed = currentState.intervalInput.toIntOrNull()
        if (parsed == null || parsed !in AdminConfig.MIN_INTERSTITIAL_INTERVAL_SEC..AdminConfig.MAX_INTERSTITIAL_INTERVAL_SEC) {
            _uiState.update { state ->
                if (state is UiState.Success) {
                    state.copy(data = state.data.copy(intervalError = true))
                } else state
            }
            return
        }

        _uiState.update { state ->
            if (state is UiState.Success) {
                state.copy(data = state.data.copy(isSavingInterval = true, intervalError = false))
            } else state
        }

        viewModelScope.launch {
            try {
                repository.updateInterstitialInterval(parsed, currentState.config)
                _snackbarEvents.tryEmit("ইন্টারস্টিশিয়াল বিরতি ($parsed সেকেন্ড) সংরক্ষিত হয়েছে")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarEvents.tryEmit("সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি ঘটেছে"}")
            } finally {
                _uiState.update { state ->
                    if (state is UiState.Success) {
                        state.copy(data = state.data.copy(isSavingInterval = false))
                    } else state
                }
            }
        }
    }
}
