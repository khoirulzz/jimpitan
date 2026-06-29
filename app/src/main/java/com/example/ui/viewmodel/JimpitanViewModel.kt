package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.WargaEntity
import com.example.data.remote.ProfileDto
import com.example.data.repository.JimpitanRepository
import com.example.data.repository.WargaArrearsInfo
import com.example.data.repository.WargaDetailData
import com.example.util.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JimpitanViewModel(
    private val repository: JimpitanRepository,
    context: android.content.Context
) : ViewModel() {

    private val networkMonitor = com.example.util.NetworkMonitor(context)
    private val sessionManager = SessionManager(context)

    val isConnected = networkMonitor.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _scannedWarga = MutableStateFlow<WargaEntity?>(null)
    val scannedWarga = _scannedWarga.asStateFlow()

    private val _scannedCoverageStatus = MutableStateFlow<CoverageStatus>(CoverageStatus.Unknown)
    val scannedCoverageStatus = _scannedCoverageStatus.asStateFlow()

    val userRole = MutableStateFlow<String?>(null)
    val userName = MutableStateFlow<String?>(null)
    val userEmail = MutableStateFlow<String?>(null)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _petugasList = MutableStateFlow<List<ProfileDto>>(emptyList())
    val petugasList = _petugasList.asStateFlow()

    private val _petugasLoading = MutableStateFlow(false)
    val petugasLoading = _petugasLoading.asStateFlow()

    private val _wargaDetail = MutableStateFlow<WargaDetailData?>(null)
    val wargaDetail = _wargaDetail.asStateFlow()

    private val _paymentDoubleBayar = MutableStateFlow(false)
    val paymentDoubleBayar = _paymentDoubleBayar.asStateFlow()

    // ─── Date Helpers ─────────────────────────────────────────────────────────

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getTodayDateStr(): String = sdf.format(Date())

    private fun getWeekStartStr(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return sdf.format(cal.time)
    }

    private fun getMonthStartStr(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return sdf.format(cal.time)
    }

    // ─── Flows ────────────────────────────────────────────────────────────────

    val todayRevenue = repository.getRevenue(getTodayDateStr()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val weekRevenue = repository.getRevenueInRange(getWeekStartStr(), getTodayDateStr()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val monthRevenue = repository.getRevenueInRange(getMonthStartStr(), getTodayDateStr()).stateIn(
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

    val allPembayaran = repository.allPembayaran.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPengeluaran = repository.allPengeluaran.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalPengeluaran = repository.totalPengeluaran.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalPemasukan = repository.totalPemasukan.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // ─── OTA Updater ──────────────────────────────────────────────────────────

    private val _updateAvailable = MutableStateFlow<com.example.data.remote.AppVersionDto?>(null)
    val updateAvailable: StateFlow<com.example.data.remote.AppVersionDto?> = _updateAvailable.asStateFlow()

    fun checkForUpdate(currentVersionCode: Int) {
        viewModelScope.launch {
            val update = repository.checkAppUpdate(currentVersionCode)
            _updateAvailable.value = update
        }
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    fun checkSavedSession(): Boolean {
        val session = sessionManager.getSession() ?: return false
        repository.accessToken = session.accessToken
        repository.userId = session.userId
        repository.userRole = session.userRole
        repository.userName = session.userName
        repository.userEmail = session.userEmail
        userRole.value = session.userRole
        userName.value = session.userName
        userEmail.value = session.userEmail
        _loginState.value = LoginState.Success
        return true
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    fun login(email: String, pass: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val success = repository.login(email, pass)
                if (success) {
                    userRole.value = repository.userRole
                    userName.value = repository.userName
                    userEmail.value = repository.userEmail
                    // Save session for offline access
                    sessionManager.saveSession(
                        token = repository.accessToken ?: "",
                        userId = repository.userId ?: "",
                        role = repository.userRole ?: "PETUGAS",
                        name = repository.userName ?: "",
                        email = repository.userEmail ?: email
                    )
                    _loginState.value = LoginState.Success
                    // Unduh data master warga, pembayaran, coverage ke Room lokal
                    // agar fitur offline (scan) bisa berjalan setelah ini
                    _isSyncing.value = true
                    try {
                        repository.fetchWarga()
                        repository.fetchPembayaranFromServer()
                        repository.fetchCoverageHistoryFromServer()
                    } finally {
                        _isSyncing.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(
                    "Login gagal: ${e.message ?: "Unknown Error"}"
                )
            }
        }
    }


    fun logout() {
        sessionManager.clearSession()
        _loginState.value = LoginState.Idle
        userRole.value = null
        userName.value = null
        userEmail.value = null
        repository.accessToken = null
        repository.userId = null
        repository.userRole = null
        repository.userName = null
        repository.userEmail = null
    }

    // ─── Scan & Payment ───────────────────────────────────────────────────────

    fun scanQr(qrText: String) {
        viewModelScope.launch {
            val uuid = if (qrText.startsWith("JMP|")) qrText.substring(4) else qrText
            val w = repository.getWargaByQr(uuid)
            _scannedWarga.value = w
            if (w != null) {
                updateCoverageStatus(w.id)
            }
        }
    }

    private suspend fun updateCoverageStatus(wargaId: String) {
        val lastCoverage = repository.getLastCoverageDate(wargaId)
        val today = getTodayDateStr()
        val status = when {
            lastCoverage == null -> CoverageStatus.Menunggak(0, null)
            lastCoverage >= today -> CoverageStatus.Lunas(lastCoverage)
            else -> {
                val diffMs = sdf.parse(today)!!.time - sdf.parse(lastCoverage)!!.time
                val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                CoverageStatus.Menunggak(days, lastCoverage)
            }
        }
        _scannedCoverageStatus.value = status
    }

    fun clearScanned() {
        _scannedWarga.value = null
        _scannedCoverageStatus.value = CoverageStatus.Unknown
        _paymentDoubleBayar.value = false
    }

    fun savePembayaran(nominal: Int) {
        val w = _scannedWarga.value ?: return
        _paymentDoubleBayar.value = false
        viewModelScope.launch {
            val saved = repository.savePembayaran(w.id, nominal)
            if (!saved) {
                _paymentDoubleBayar.value = true
                return@launch
            }
            repository.syncPending()
            updateCoverageStatus(w.id)
        }
    }

    // ─── Cash Book (Buku Kas) ─────────────────────────────────────────────────

    fun savePengeluaran(nominal: Int, keterangan: String, onSuccess: () -> Unit) {
        val uid = repository.userId ?: return
        viewModelScope.launch {
            repository.savePengeluaran(nominal, keterangan, uid)
            repository.syncPending()
            onSuccess()
        }
    }

    // ─── Warga Management ─────────────────────────────────────────────────────

    fun addWarga(nama: String, rt: String, rw: String, nomorRumah: String, alamat: String, noWa: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveWarga(nama, rt, rw, nomorRumah, alamat, noWa)
            onSuccess()
        }
    }

    fun addPetugas(email: String, nama: String, pass: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.savePetugas(email, nama, pass)
            if (success) onSuccess() else onError()
        }
    }

    fun loadPetugas() {
        _petugasLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.fetchPetugas()
                _petugasList.value = result
                // Retry once if empty (timing issue with Supabase trigger)
                if (result.isEmpty()) {
                    delay(1500)
                    _petugasList.value = repository.fetchPetugas()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _petugasLoading.value = false
            }
        }
    }

    fun importWargaFromExcel(
        uri: android.net.Uri,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isSyncing.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val parsedList = com.example.util.ExcelParser.parseWargaExcel(inputStream)
                    if (parsedList.isNotEmpty()) {
                        repository.saveWargaList(parsedList)
                        repository.fetchWarga()
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) { onSuccess() }
                    } else {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onError("Format Excel tidak cocok atau kosong.")
                        }
                    }
                } else {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        onError("Gagal membuka berkas Excel.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Terjadi kesalahan: ${e.localizedMessage}")
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // ─── Warga Detail ─────────────────────────────────────────────────────────

    fun loadWargaDetail(wargaId: String, year: Int, month: Int) {
        viewModelScope.launch {
            _wargaDetail.value = repository.getWargaDetail(wargaId, year, month)
        }
    }

    // ─── Arrears Info ─────────────────────────────────────────────────────────

    private val _arrearsInfoList = MutableStateFlow<List<WargaArrearsInfo>>(emptyList())
    val arrearsInfoList: StateFlow<List<WargaArrearsInfo>> = _arrearsInfoList.asStateFlow()

    fun buildArrearsInfoList(wargaList: List<WargaEntity>) {
        viewModelScope.launch {
            val result = wargaList.map { repository.buildArrearsInfo(it) }
                .sortedByDescending { it.arrearsdays }
            _arrearsInfoList.value = result
        }
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────

    fun syncNow() {
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                repository.fetchWarga()
                repository.fetchPembayaranFromServer()
                repository.fetchCoverageHistoryFromServer()
                repository.syncPending()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Pull-to-refresh: syncs all data and reloads petugas list.
     */
    fun pullRefresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                repository.fetchWarga()
                repository.fetchPembayaranFromServer()
                repository.fetchCoverageHistoryFromServer()
                repository.syncPending()
                // Also reload petugas for admin
                val result = repository.fetchPetugas()
                _petugasList.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    fun exportLaporanPdf(
        context: android.content.Context,
        startDate: String? = null,
        endDate: String? = null,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val wargaList = allWarga.value
            val wargaMap = wargaList.associateBy { it.id }
            val list = allPembayaran.value.let { all ->
                if (startDate != null && endDate != null) {
                    all.filter { it.tanggalBayar in startDate..endDate }
                } else all
            }
            val judul = if (startDate != null && endDate != null)
                "Laporan Jimpitan: $startDate s.d. $endDate"
            else "Laporan Jimpitan Digital"
            val ok = com.example.util.PdfExporter.exportLaporanPdf(context, list, wargaMap, judul)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                if (ok) onSuccess() else onError()
            }
        }
    }

    fun exportQrSheetPdf(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ok = com.example.util.PdfExporter.exportQrSheetPdf(context, allWarga.value)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                if (ok) onSuccess() else onError()
            }
        }
    }

    fun exportBukuKasPdf(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pengeluaranList = allPengeluaran.value
            val totalIn = totalPemasukan.value ?: 0
            val totalOut = totalPengeluaran.value ?: 0
            val ok = com.example.util.PdfExporter.exportBukuKasPdf(context, pengeluaranList, totalIn, totalOut)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                if (ok) onSuccess() else onError()
            }
        }
    }


    // ─── Factory ─────────────────────────────────────────────────────────────

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

// ─── State Classes ────────────────────────────────────────────────────────────

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class CoverageStatus {
    object Unknown : CoverageStatus()
    data class Lunas(val until: String) : CoverageStatus()
    data class Menunggak(val days: Int, val lastCoverageDate: String?) : CoverageStatus()
}
