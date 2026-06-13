package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.WargaEntity
import com.example.data.repository.JimpitanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JimpitanViewModel(
    private val repository: JimpitanRepository,
    context: android.content.Context
) : ViewModel() {

    private val networkMonitor = com.example.util.NetworkMonitor(context)

    val isConnected = networkMonitor.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _scannedWarga = MutableStateFlow<WargaEntity?>(null)
    val scannedWarga = _scannedWarga.asStateFlow()

    val userRole = MutableStateFlow<String?>(null)

    private fun getTodayDateStr(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    val todayRevenue = repository.getRevenue(getTodayDateStr()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val todayPaidCount = repository.getPaidCount(getTodayDateStr()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val todayArrearsCount = repository.getArrearsCount(getTodayDateStr()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val arrearsWargaList = repository.getArrearsWarga(getTodayDateStr()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val allWarga = repository.allWarga.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun login(email: String, pass: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val success = repository.login(email, pass)
            if (success) {
                userRole.value = repository.userRole
                _loginState.value = LoginState.Success
                repository.fetchWarga()
            } else {
                _loginState.value = LoginState.Error("Login gagal")
            }
        }
    }

    fun logout() {
        _loginState.value = LoginState.Idle
        userRole.value = null
        repository.accessToken = null
        repository.userId = null
        repository.userRole = null
    }

    fun scanQr(qrText: String) {
        viewModelScope.launch {
            val uuid = if(qrText.startsWith("JMP|")) qrText.substring(4) else qrText
            val w = repository.getWargaByQr(uuid)
            _scannedWarga.value = w
        }
    }

    fun clearScanned() {
        _scannedWarga.value = null
    }

    fun savePembayaran(nominal: Int) {
        val w = _scannedWarga.value ?: return
        viewModelScope.launch {
            repository.savePembayaran(w.id, nominal)
            repository.syncPending()
        }
    }

    fun addWarga(nama: String, rt: String, rw: String, nomorRumah: String, alamat: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveWarga(nama, rt, rw, nomorRumah, alamat)
            onSuccess()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.fetchWarga()
            repository.syncPending()
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repository: JimpitanRepository,
        private val context: android.content.Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JimpitanViewModel(repository, context) as T
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
