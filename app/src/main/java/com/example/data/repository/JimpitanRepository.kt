package com.example.data.repository

import com.example.data.local.dao.JimpitanDao
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.remote.AuthRequest
import com.example.data.remote.CoverageDto
import com.example.data.remote.PembayaranDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.SupabaseService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WargaArrearsInfo(
    val warga: com.example.data.local.entity.WargaEntity,
    val arrearsdays: Int,
    val lastPaymentDate: String?,
    val lastCoverageDate: String?
)

data class WargaDetailData(
    val warga: com.example.data.local.entity.WargaEntity,
    val totalLunasDays: Int,
    val totalNominal: Int,
    val lastCoverageDate: String?,
    val coverageMap: Map<String, Boolean> // date -> lunas (true) or not (false, meaning arrears)
)

class JimpitanRepository(
    private val api: SupabaseService,
    private val dao: JimpitanDao,
    private val apiKey: String
) {
    var accessToken: String? = null
    var userId: String? = null
    var userRole: String? = null
    var userName: String? = null
    var userEmail: String? = null

    val allWarga = dao.getAllWarga()
    val allPembayaran = dao.getAllPembayaran()
    val totalPemasukan = dao.getTotalPemasukan()
    val allPengeluaran = dao.getAllPengeluaran()
    val totalPengeluaran = dao.getTotalPengeluaran()

    fun getRevenue(date: String): Flow<Int?> = dao.getRevenueByDate(date)
    fun getPaidCount(date: String): Flow<Int> = dao.getPaidCountByDate(date)
    fun getArrearsCount(date: String): Flow<Int> = dao.getArrearsCountByDate(date)
    fun getArrearsWarga(date: String): Flow<List<com.example.data.local.entity.WargaEntity>> = dao.getArrearsWargaByDate(date)
    fun getRevenueInRange(startDate: String, endDate: String): Flow<Int?> = dao.getRevenueInRange(startDate, endDate)

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun todayStr() = sdf.format(Date())

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * Login memerlukan internet. Setelah berhasil, data warga, pembayaran, dan coverage
     * diunduh ke Room sehingga session berikutnya bisa offline.
     * Return false jika gagal (termasuk jika tidak ada internet).
     */
    suspend fun login(email: String, pass: String): Boolean {
        val res = api.login(apiKey, AuthRequest(email, pass))
        accessToken = res.access_token
        userId = res.user.id
        userEmail = res.user.email ?: email

        val authHeader = "Bearer ${res.access_token}"
        val profiles = api.getProfile(apiKey, authHeader, "eq.${res.user.id}")
        val profile = profiles.firstOrNull()
        userRole = profile?.role ?: "PETUGAS"
        userName = profile?.nama ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
        return true
    }


    // ─── Petugas ──────────────────────────────────────────────────────────────

    suspend fun savePetugas(email: String, nama: String, pass: String): Boolean {
        if (accessToken != null && accessToken != "dummy_token") {
            return try {
                val req = com.example.data.remote.SignUpRequest(
                    email = email,
                    password = pass,
                    data = mapOf("nama" to nama, "role" to "PETUGAS")
                )
                api.signUp(apiKey, req)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return true
    }

    suspend fun fetchPetugas(): List<ProfileDto> {
        if (accessToken == null || accessToken == "dummy_token") return emptyList()
        return try {
            val authHeader = "Bearer $accessToken"
            api.getPetugas(apiKey, authHeader)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Warga ────────────────────────────────────────────────────────────────

    suspend fun fetchWarga() {
        val seedData = listOf(
            com.example.data.local.entity.WargaEntity("1", "WRG001", "Edi Subekti", "03", "01", "009", "Jl. Mawar No. 9", true, 0),
            com.example.data.local.entity.WargaEntity("2", "WRG002", "Samiri", "03", "01", "008", "Jl. Mawar No. 8", true, 0),
            com.example.data.local.entity.WargaEntity("3", "WRG003", "Duryono", "03", "01", "007", "Jl. Mawar No. 7", true, 0),
            com.example.data.local.entity.WargaEntity("4", "WRG004", "Seswa Hidayat", "03", "01", "001", "Jl. Melati No. 1", true, 0),
            com.example.data.local.entity.WargaEntity("5", "WRG005", "Casto", "03", "01", "002", "Jl. Melati No. 2", true, 0),
            com.example.data.local.entity.WargaEntity("6", "WRG006", "Turyanto", "03", "01", "004", "Jl. Melati No. 4", true, 0),
            com.example.data.local.entity.WargaEntity("7", "WRG007", "Pamuji", "03", "01", "005", "Jl. Melati No. 5", true, 0),
            com.example.data.local.entity.WargaEntity("8", "WRG008", "Datar", "03", "01", "006", "Jl. Melati No. 6", true, 0),
            com.example.data.local.entity.WargaEntity("9", "WRG009", "Anton", "03", "01", "010", "Jl. Mawar No. 10", true, 0),
            com.example.data.local.entity.WargaEntity("10", "WRG010", "Karyanto", "03", "01", "011", "Jl. Mawar No. 11", true, 0)
        )
        try {
            if (accessToken != null && accessToken != "dummy_token") {
                val authHeader = "Bearer $accessToken"
                val remoteWarga = api.getWarga(apiKey, authHeader)
                if (remoteWarga.isNotEmpty()) {
                    dao.clearWarga()
                    val entities = remoteWarga.map {
                        com.example.data.local.entity.WargaEntity(
                            id = it.id,
                            qrUuid = it.qr_uuid,
                            nama = it.nama,
                            rt = it.rt,
                            rw = it.rw,
                            nomorRumah = it.nomor_rumah,
                            alamat = it.alamat,
                            isActive = it.is_active,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    dao.insertWargaList(entities)
                    return
                }
            }
            dao.insertWargaList(seedData)
        } catch (e: Exception) {
            e.printStackTrace()
            dao.insertWargaList(seedData)
        }
    }

    suspend fun fetchPembayaranFromServer() {
        if (accessToken == null || accessToken == "dummy_token") return
        try {
            val authHeader = "Bearer $accessToken"
            val remotePembayaran = api.getPembayaran(apiKey, authHeader)
            if (remotePembayaran.isNotEmpty()) {
                val entities = remotePembayaran.map { dto ->
                    PembayaranEntity(
                        serverId = dto.id,
                        wargaId = dto.warga_id,
                        nominal = dto.nominal,
                        coverageDays = dto.coverage_days,
                        tanggalBayar = dto.tanggal_bayar,
                        createdBy = dto.created_by,
                        syncStatus = "SYNCED"
                    )
                }
                dao.clearSyncedPembayaran()
                dao.insertPembayaranList(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchCoverageHistoryFromServer() {
        if (accessToken == null || accessToken == "dummy_token") return
        try {
            val authHeader = "Bearer $accessToken"
            val remoteCoverage = api.getCoverageHistory(apiKey, authHeader)
            if (remoteCoverage.isNotEmpty()) {
                val entities = remoteCoverage.map { dto ->
                    CoverageHistoryEntity(
                        id = dto.id,
                        wargaId = dto.warga_id,
                        paymentId = dto.payment_id,
                        paymentLocalId = null,
                        tanggalKewajiban = dto.tanggal_kewajiban
                    )
                }
                dao.clearSyncedCoverageHistory()
                dao.insertCoverageHistoryList(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchPengeluaranFromServer() {
        if (accessToken == null || accessToken == "dummy_token") return
        try {
            val authHeader = "Bearer $accessToken"
            val remotePengeluaran = api.getPengeluaran(apiKey, authHeader)
            if (remotePengeluaran.isNotEmpty()) {
                val entities = remotePengeluaran.map { dto ->
                    com.example.data.local.entity.PengeluaranEntity(
                        serverId = dto.id,
                        nominal = dto.nominal,
                        tanggal = dto.tanggal,
                        keterangan = dto.keterangan,
                        createdBy = dto.created_by,
                        syncStatus = "SYNCED"
                    )
                }
                dao.clearSyncedPengeluaran()
                dao.insertPengeluaranList(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getWargaByQr(qrText: String) = dao.getWargaByQr(qrText)

    suspend fun saveWarga(nama: String, rt: String, rw: String, nomorRumah: String, alamat: String): Boolean {
        val count = dao.getWargaCount()
        val qrUuid = "WRG" + String.format(Locale.getDefault(), "%03d", count + 1)
        val newWarga = com.example.data.local.entity.WargaEntity(
            id = java.util.UUID.randomUUID().toString(),
            qrUuid = qrUuid,
            nama = nama,
            rt = rt,
            rw = rw,
            nomorRumah = nomorRumah,
            alamat = alamat,
            isActive = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertWargaList(listOf(newWarga))

        if (accessToken != null && accessToken != "dummy_token") {
            try {
                val authHeader = "Bearer $accessToken"
                val dto = com.example.data.remote.WargaDto(
                    id = newWarga.id,
                    qr_uuid = newWarga.qrUuid,
                    nama = newWarga.nama,
                    rt = newWarga.rt,
                    rw = newWarga.rw,
                    nomor_rumah = newWarga.nomorRumah,
                    alamat = newWarga.alamat,
                    is_active = newWarga.isActive,
                    updated_at = ""
                )
                api.insertWarga(apiKey, authHeader, req = dto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    suspend fun saveWargaList(wargaList: List<com.example.data.local.entity.WargaEntity>): Boolean {
        val startCount = dao.getWargaCount()
        val processedList = wargaList.mapIndexed { index, warga ->
            warga.copy(
                qrUuid = "WRG" + String.format(Locale.getDefault(), "%03d", startCount + index + 1)
            )
        }
        dao.insertWargaList(processedList)

        if (accessToken != null && accessToken != "dummy_token") {
            val authHeader = "Bearer $accessToken"
            for (w in processedList) {
                try {
                    val dto = com.example.data.remote.WargaDto(
                        id = w.id,
                        qr_uuid = w.qrUuid,
                        nama = w.nama,
                        rt = w.rt,
                        rw = w.rw,
                        nomor_rumah = w.nomorRumah,
                        alamat = w.alamat,
                        is_active = w.isActive,
                        updated_at = ""
                    )
                    api.insertWarga(apiKey, authHeader, req = dto)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    // ─── Pembayaran ───────────────────────────────────────────────────────────

    /**
     * Returns true if payment was saved, false if warga already has coverage today (double-pay prevention).
     */
    suspend fun savePembayaran(wargaId: String, nominal: Int): Boolean {
        val today = todayStr()

        // Anti double-bayar: cek apakah warga sudah punya coverage hari ini
        val existingToday = dao.hasCoverageOnDate(wargaId, today)
        if (existingToday > 0) {
            return false // Already paid today
        }

        val coverageDays = nominal / 500

        val idLocal = dao.insertPembayaran(
            PembayaranEntity(
                serverId = null,
                wargaId = wargaId,
                nominal = nominal,
                coverageDays = coverageDays,
                tanggalBayar = today,
                createdBy = userId ?: "unknown",
                syncStatus = "PENDING"
            )
        )

        // Hitung Coverage FIFO
        val lastCoverage = dao.getLastCoverageDate(wargaId)
        val cal = Calendar.getInstance()
        if (lastCoverage != null) {
            val lastDate = sdf.parse(lastCoverage)
            if (lastDate != null) {
                cal.time = lastDate
                // Check if last coverage is in the past — if so, check for gaps
                val todayCal = Calendar.getInstance()
                if (cal.before(todayCal)) {
                    // Last coverage is in the past; continue from next day to cover arrears
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                } else {
                    // Last coverage is today or future; continue from next day
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
            } else {
                cal.time = sdf.parse(today)!!
            }
        } else {
            cal.time = sdf.parse(today)!!
        }

        val coverageList = mutableListOf<CoverageHistoryEntity>()
        for (i in 0 until coverageDays) {
            coverageList.add(
                CoverageHistoryEntity(
                    id = null,
                    wargaId = wargaId,
                    paymentId = null,
                    paymentLocalId = idLocal,
                    tanggalKewajiban = sdf.format(cal.time)
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        dao.insertCoverageHistoryList(coverageList)
        return true
    }

    suspend fun savePengeluaran(nominal: Int, keterangan: String, userId: String): Boolean {
        val newPengeluaran = com.example.data.local.entity.PengeluaranEntity(
            serverId = null,
            nominal = nominal,
            tanggal = todayStr(),
            keterangan = keterangan,
            createdBy = userId,
            syncStatus = "PENDING"
        )
        dao.insertPengeluaran(newPengeluaran)
        return true
    }

    suspend fun getLastCoverageDate(wargaId: String): String? = dao.getLastCoverageDate(wargaId)

    suspend fun hasCoverageToday(wargaId: String): Boolean {
        return dao.hasCoverageOnDate(wargaId, todayStr()) > 0
    }

    // ─── Warga Detail ─────────────────────────────────────────────────────────

    suspend fun getWargaDetail(wargaId: String, year: Int, month: Int): WargaDetailData? {
        val warga = dao.getWargaById(wargaId) ?: return null

        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val startDate = sdf.format(cal.time)
        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = sdf.format(cal.time)

        val coverageInMonth = dao.getCoverageInRange(wargaId, startDate, endDate)
        val paidDates = coverageInMonth.map { it.tanggalKewajiban }.toSet()

        // Build coverage map for the whole month
        val coverageMap = mutableMapOf<String, Boolean>()
        val dayCal = Calendar.getInstance()
        dayCal.set(year, month - 1, 1)
        while (sdf.format(dayCal.time) <= endDate) {
            val dateStr = sdf.format(dayCal.time)
            if (sdf.format(dayCal.time) > endDate) break
            coverageMap[dateStr] = paidDates.contains(dateStr)
            dayCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val totalLunas = dao.getTotalLunasByWarga(wargaId)
        val totalNominal = dao.getTotalNominalByWarga(wargaId) ?: 0
        val lastCoverage = dao.getLastCoverageDate(wargaId)

        return WargaDetailData(
            warga = warga,
            totalLunasDays = totalLunas,
            totalNominal = totalNominal,
            lastCoverageDate = lastCoverage,
            coverageMap = coverageMap
        )
    }

    // ─── Arrears Detail ───────────────────────────────────────────────────────

    suspend fun buildArrearsInfo(warga: com.example.data.local.entity.WargaEntity): WargaArrearsInfo {
        val lastCoverage = dao.getLastCoverageDate(warga.id)
        val today = todayStr()

        val arrearsdays = if (lastCoverage == null) {
            // Never paid: count days from beginning of current month
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = sdf.format(cal.time)
            val diffMs = sdf.parse(today)!!.time - sdf.parse(monthStart)!!.time
            (diffMs / (1000 * 60 * 60 * 24)).toInt() + 1
        } else if (lastCoverage < today) {
            val diffMs = sdf.parse(today)!!.time - sdf.parse(lastCoverage)!!.time
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } else {
            0
        }

        // Get last payment date
        val lastPayment = dao.getPembayaranByWarga(warga.id)

        return WargaArrearsInfo(
            warga = warga,
            arrearsdays = arrearsdays,
            lastPaymentDate = null, // will be populated from flow in ViewModel
            lastCoverageDate = lastCoverage
        )
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────

    suspend fun syncPending() {
        if (accessToken == null || accessToken == "dummy_token") return
        val pending = dao.getPendingPembayaran()
        val authHeader = "Bearer $accessToken"

        for (p in pending) {
            try {
                val pReq = PembayaranDto(
                    warga_id = p.wargaId,
                    nominal = p.nominal,
                    coverage_days = p.coverageDays,
                    tanggal_bayar = p.tanggalBayar,
                    created_by = p.createdBy
                )
                val pRes = api.insertPembayaran(apiKey, authHeader, req = pReq)
                val sId = pRes.firstOrNull()?.id ?: continue

                dao.updatePembayaranStatus(p.idLocal, "SYNCED", sId)

                val localCoverages = dao.getCoverageByLocalPaymentId(p.idLocal)
                var coverageConflict = false
                for (c in localCoverages) {
                    try {
                        val cReq = CoverageDto(
                            warga_id = c.wargaId,
                            payment_id = sId,
                            tanggal_kewajiban = c.tanggalKewajiban
                        )
                        val cRes = api.insertCoverage(apiKey, authHeader, req = cReq)
                        val sCoverageId = cRes.firstOrNull()?.id
                        if (sCoverageId != null) {
                            dao.updateCoverageServerId(c.localId, sCoverageId, sId)
                        }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 409) {
                            coverageConflict = true
                            break
                        } else {
                            throw e
                        }
                    }
                }
                if (coverageConflict) {
                    dao.updatePembayaranStatus(p.idLocal, "CONFLICT", sId)
                }
            } catch (e: retrofit2.HttpException) {
                val status = if (e.code() == 409) "CONFLICT" else "FAILED"
                dao.updatePembayaranStatus(p.idLocal, status, null)
            } catch (e: Exception) {
                dao.updatePembayaranStatus(p.idLocal, "FAILED", null)
            }
        }

        // Sync Pengeluaran
        val pendingPengeluaran = dao.getPendingPengeluaran()
        for (pengeluaran in pendingPengeluaran) {
            try {
                val req = com.example.data.remote.PengeluaranDto(
                    nominal = pengeluaran.nominal,
                    tanggal = pengeluaran.tanggal,
                    keterangan = pengeluaran.keterangan,
                    created_by = pengeluaran.createdBy
                )
                val res = api.insertPengeluaran(apiKey, authHeader, req = req)
                val sId = res.firstOrNull()?.id
                if (sId != null) {
                    dao.updatePengeluaranStatus(pengeluaran.idLocal, "SYNCED", sId)
                }
            } catch (e: Exception) {
                dao.updatePengeluaranStatus(pengeluaran.idLocal, "FAILED", null)
            }
        }
    }

    // ─── OTA Update ───────────────────────────────────────────────────────────
    suspend fun checkAppUpdate(currentVersionCode: Int): com.example.data.remote.AppVersionDto? {
        return try {
            val versions = api.getLatestVersion(apiKey)
            val latest = versions.firstOrNull()
            if (latest != null && latest.versionCode > currentVersionCode) {
                latest
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
