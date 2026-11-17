package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KMobileRepository
import com.example.model.CallRecord
import com.example.model.SubscriberContract
import com.example.model.TariffPlan
import com.example.model.UiState
import com.example.model.generateVirtualNumbers7990
import com.example.model.normalizeDialablePhone
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallBridgeState(
    val callerVirtualNumber: String,
    val recipientNumber: String,
    val recipientName: String,
    val networkType: String,
    val phaseLabel: String,
    val progress: Float
)

class KMobileViewModel(
    private val repository: KMobileRepository,
    private val currentUserId: String
) : ViewModel() {

    val contractState: StateFlow<UiState<SubscriberContract?>> =
        repository.observePrimaryContract(currentUserId)
            .map<SubscriberContract?, UiState<SubscriberContract?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing contract", rootError)
                emit(UiState.Error(rootError.message ?: "Не удалось загрузить договор"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val callHistoryState: StateFlow<UiState<List<CallRecord>>> =
        repository.observeCallHistory(currentUserId)
            .map<List<CallRecord>, UiState<List<CallRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing call history", rootError)
                emit(UiState.Error(rootError.message ?: "Не удалось загрузить историю звонков"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _dialInput = MutableStateFlow("")
    val dialInput: StateFlow<String> = _dialInput.asStateFlow()

    private val _selectedContactName = MutableStateFlow("")
    val selectedContactName: StateFlow<String> = _selectedContactName.asStateFlow()

    private val _availableNumbers = MutableStateFlow(generateVirtualNumbers7990())
    val availableNumbers: StateFlow<List<String>> = _availableNumbers.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _activeBridge = MutableStateFlow<CallBridgeState?>(null)
    val activeBridge: StateFlow<CallBridgeState?> = _activeBridge.asStateFlow()

    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    private var bridgeJob: Job? = null

    fun appendDigit(digit: String) {
        if (_dialInput.value.length < 18) {
            _dialInput.value += digit
            _selectedContactName.value = ""
        }
    }

    fun backspaceDigit() {
        if (_dialInput.value.isNotEmpty()) {
            _dialInput.value = _dialInput.value.dropLast(1)
            if (_dialInput.value.isEmpty()) _selectedContactName.value = ""
        }
    }

    fun clearDialInput() {
        _dialInput.value = ""
        _selectedContactName.value = ""
    }

    fun selectRecipient(phone: String, name: String = "") {
        _dialInput.value = normalizeDialablePhone(phone).ifBlank { phone }
        _selectedContactName.value = name
    }

    fun refreshVirtualNumbers() {
        _availableNumbers.value = generateVirtualNumbers7990()
    }

    fun signContract(
        fullName: String,
        documentIdNumber: String,
        contactEmail: String,
        virtualNumber: String,
        tariff: TariffPlan,
        signatureStrokeCount: Int,
        signaturePointsJson: String,
        onSuccess: () -> Unit
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.signOrRenewContract(
                userId = currentUserId,
                fullName = fullName,
                documentIdNumber = documentIdNumber,
                contactEmail = contactEmail,
                virtualNumber = virtualNumber,
                tariff = tariff,
                signatureStrokeCount = signatureStrokeCount,
                signaturePointsJson = signaturePointsJson
            )
            _isSubmitting.value = false
            result.onSuccess { contractNum ->
                _uiMessages.tryEmit("Договор $contractNum подписан! Номер $virtualNumber активен.")
                onSuccess()
            }.onFailure { err ->
                _uiMessages.tryEmit(err.localizedMessage ?: "Не удалось оформить договор")
            }
        }
    }

    fun switchTariff(tariff: TariffPlan) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.switchTariff(currentUserId, tariff)
            _isSubmitting.value = false
            result.onSuccess {
                _uiMessages.tryEmit("Тариф изменён на «${tariff.title}» (${tariff.priceRub} ₽/мес)")
            }.onFailure { err ->
                _uiMessages.tryEmit(err.localizedMessage ?: "Ошибка смены тарифа")
            }
        }
    }

    fun terminateContract() {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.terminateContract(currentUserId)
            _isSubmitting.value = false
            result.onSuccess {
                _uiMessages.tryEmit("Договор К mobile расторгнут. Для звонков оформите новый договор.")
            }.onFailure { err ->
                _uiMessages.tryEmit(err.localizedMessage ?: "Не удалось расторгнуть договор")
            }
        }
    }

    /**
     * Connects through К mobile's internet bridge and invokes [onLaunchRealCall]
     * so the user enters a real Android system call.
     */
    fun startInternetToRealCall(
        contract: SubscriberContract,
        rawRecipient: String,
        recipientName: String,
        networkType: String,
        isInternetAvailable: Boolean,
        onLaunchRealCall: (normalizedNumber: String) -> Unit
    ) {
        if (!contract.activeContract()) {
            _uiMessages.tryEmit("Для звонков нужен действующий договор К mobile.")
            return
        }
        if (!isInternetAvailable) {
            _uiMessages.tryEmit("Подключите интернет (Wi-Fi или LTE/5G) для звонка через К mobile.")
            return
        }
        val dialable = normalizeDialablePhone(rawRecipient)
        if (dialable.length < 4) {
            _uiMessages.tryEmit("Введите корректный номер телефона.")
            return
        }

        bridgeJob?.cancel()
        bridgeJob = viewModelScope.launch {
            _activeBridge.value = CallBridgeState(
                callerVirtualNumber = contract.virtualNumber,
                recipientNumber = dialable,
                recipientName = recipientName,
                networkType = networkType,
                phaseLabel = "Шифрование и шлюз К mobile ($networkType)…",
                progress = 0.35f
            )
            delay(500L)

            _activeBridge.value = CallBridgeState(
                callerVirtualNumber = contract.virtualNumber,
                recipientNumber = dialable,
                recipientName = recipientName,
                networkType = networkType,
                phaseLabel = "Выделение линии ${contract.virtualNumber} → перевод в звонок…",
                progress = 0.85f
            )

            repository.logCall(
                userId = currentUserId,
                callerVirtualNumber = contract.virtualNumber,
                recipientNumber = dialable,
                recipientName = recipientName,
                networkType = networkType,
                tariffId = contract.tariffId,
                callMode = CallRecord.CALL_MODE_INTERNET_TO_REAL,
                status = CallRecord.CALL_STATUS_INITIATED
            )

            delay(450L)
            _activeBridge.value = null
            onLaunchRealCall(dialable)
        }
    }

    fun cancelActiveBridge() {
        bridgeJob?.cancel()
        _activeBridge.value = null
    }

    fun deleteCall(callId: String) {
        viewModelScope.launch {
            repository.deleteCallRecord(currentUserId, callId)
                .onFailure { err ->
                    _uiMessages.tryEmit(err.localizedMessage ?: "Не удалось удалить запись")
                }
        }
    }

    fun notifyMessage(message: String) {
        _uiMessages.tryEmit(message)
    }

    private companion object {
        const val TAG = "KMobileVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
