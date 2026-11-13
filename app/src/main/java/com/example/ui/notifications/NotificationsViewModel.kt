package com.example.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PushHistoryItem
import com.example.data.model.TargetScreen
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

data class NotificationFormState(
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val selectedScreen: TargetScreen = TargetScreen.NONE,
    val isSending: Boolean = false,
    val showConfirmAllDialog: Boolean = false,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false
) {
    val isTitleValid: Boolean get() = title.trim().isNotEmpty() && title.length <= MAX_TITLE_LENGTH
    val isBodyValid: Boolean get() = body.trim().isNotEmpty() && body.length <= MAX_BODY_LENGTH
    val isImageUrlValid: Boolean
        get() {
            val trimmed = imageUrl.trim()
            return trimmed.isEmpty() || trimmed.startsWith("https://")
        }
    val canSend: Boolean
        get() = !isSending && isTitleValid && isBodyValid && isImageUrlValid

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val MAX_BODY_LENGTH = 200
    }
}

class NotificationsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _formState = MutableStateFlow(NotificationFormState())
    val formState: StateFlow<NotificationFormState> = _formState.asStateFlow()

    private val _historyState = MutableStateFlow<UiState<List<PushHistoryItem>>>(UiState.Loading)
    val historyState: StateFlow<UiState<List<PushHistoryItem>>> = _historyState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var historyJob: Job? = null

    init {
        observeHistory()
    }

    fun observeHistory() {
        historyJob?.cancel()
        _historyState.value = UiState.Loading
        historyJob = viewModelScope.launch {
            repository.observePushHistory()
                .map<List<PushHistoryItem>, UiState<List<PushHistoryItem>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    emit(UiState.Error(root.localizedMessage ?: "ইতিহাস লোড করা যায়নি"))
                }
                .collect { state ->
                    _historyState.value = state
                }
        }
    }

    fun onTitleChange(value: String) {
        if (value.length <= NotificationFormState.MAX_TITLE_LENGTH) {
            _formState.update { it.copy(title = value, statusMessage = null) }
        }
    }

    fun onBodyChange(value: String) {
        if (value.length <= NotificationFormState.MAX_BODY_LENGTH) {
            _formState.update { it.copy(body = value, statusMessage = null) }
        }
    }

    fun onImageUrlChange(value: String) {
        _formState.update { it.copy(imageUrl = value, statusMessage = null) }
    }

    fun onScreenSelected(screen: TargetScreen) {
        _formState.update { it.copy(selectedScreen = screen) }
    }

    fun requestSendAllConfirmation() {
        if (!_formState.value.canSend) return
        _formState.update { it.copy(showConfirmAllDialog = true) }
    }

    fun dismissSendAllConfirmation() {
        _formState.update { it.copy(showConfirmAllDialog = false) }
    }

    fun sendTest() {
        sendPush(target = "tester")
    }

    fun confirmSendAll() {
        _formState.update { it.copy(showConfirmAllDialog = false) }
        sendPush(target = "all")
    }

    private fun sendPush(target: String) {
        val current = _formState.value
        if (!current.canSend) return

        _formState.update {
            it.copy(
                isSending = true,
                statusMessage = null,
                statusIsError = false
            )
        }

        viewModelScope.launch {
            try {
                repository.sendPushNotification(
                    title = current.title.trim(),
                    body = current.body.trim(),
                    imageUrl = current.imageUrl.trim(),
                    screen = current.selectedScreen.routeKey,
                    target = target
                )
                val successMsg = if (target == "tester") {
                    "টেস্ট নোটিফিকেশন সফলভাবে পাঠানো হয়েছে"
                } else {
                    "সবার কাছে নোটিফিকেশন পাঠানো হয়েছে"
                }
                _formState.update {
                    it.copy(
                        isSending = false,
                        statusMessage = successMsg,
                        statusIsError = false
                    )
                }
                _snackbarEvents.tryEmit(successMsg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "নোটিফিকেশন পাঠানো যায়নি"
                _formState.update {
                    it.copy(
                        isSending = false,
                        statusMessage = errMsg,
                        statusIsError = true
                    )
                }
                _snackbarEvents.tryEmit("ব্যর্থ: $errMsg")
            }
        }
    }
}
