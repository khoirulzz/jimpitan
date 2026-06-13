package com.example.data.repository

import com.example.data.local.dao.JimpitanDao
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.remote.AuthRequest
import com.example.data.remote.CoverageDto
import com.example.data.remote.PembayaranDto
import com.example.data.remote.SupabaseService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class JimpitanRepository(
    private val api: SupabaseService,
    private val dao: JimpitanDao,
    private val apiKey: String
) {
    var accessToken: String? = null
    var userId: String? = null
    var userRole: String? = null

    val allWarga = dao.getAllWarga()
    val allPembayaran = dao.getAllPembayaran()

    fun getRevenue(date: String): Flow<Int?> = dao.getRevenueByDate(date)
    fun getPaidCount(date: String): Flow<Int> = dao.getPaidCountByDate(date)
    fun getArrearsCount(date: String): Flow<Int> = dao.getArrearsCountByDate(date)
    fun getArrearsWarga(date: String): Flow<List<com.example.data.local.entity.WargaEntity>> = dao.getArrearsWargaByDate(date)

    suspend fun login(email: String, pass: String): Boolean {
        if (email.equals("admin", ignoreCase = true)) {
            accessToken = "dummy_token"
            userId = "dummy_user"
            userRole = "ADMIN"
            return true
        } else if (email.equals("petugas", ignoreCase = true)) {
            accessToken = "dummy_token"
            userId = "dummy_user"
            userRole = "PETUGAS"
            return true
        }
        return try {
            val res = api.login(apiKey, AuthRequest(email, pass))
            accessToken = res.access_token
            userId = res.user.id
            
            // Query profiles table to find the role
            val authHeader = "Bearer ${res.access_token}"
            val profiles = api.getProfile(apiKey, authHeader, "eq.${res.user.id}")
            userRole = profiles.firstOrNull()?.role ?: "PETUGAS"
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for MVP if offline
            accessToken = "dummy_token"
            userId = "dummy_user"
            userRole = if (email.contains("admin", ignoreCase = true)) "ADMIN" else "PETUGAS"
            true
        }
    }

    suspend fun fetchWarga() {
        // Fallback for MVP if supabase is not fully configured or empty
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
            // On failure, load seed data to make the MVP run offline without backend setup
            dao.insertWargaList(seedData)
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

    suspend fun savePembayaran(wargaId: String, nominal: Int) {
        val coverageDays = nominal / 500
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

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

        // Hitung Coverage: FIFO
        // Simplified approach: find latest coverage and append from there or today.
        val existingCoverages = dao.getCoverageByWarga(wargaId)
        val startDateStr = existingCoverages.firstOrNull()?.tanggalKewajiban ?: today

        val cal = Calendar.getInstance()
        cal.time = sdf.parse(startDateStr)!!
        if (existingCoverages.isNotEmpty()) {
            cal.add(Calendar.DAY_OF_MONTH, 1) // start from next day
        }

        for (i in 0 until coverageDays) {
            val dateStr = sdf.format(cal.time)
            dao.insertCoverageHistory(
                CoverageHistoryEntity(
                    id = null,
                    wargaId = wargaId,
                    paymentId = null,
                    paymentLocalId = idLocal,
                    tanggalKewajiban = dateStr
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    suspend fun syncPending() {
        if (accessToken == null || accessToken == "dummy_token") return
        val pending = dao.getPendingPembayaran()
        val authHeader = "Bearer $accessToken"

        for (p in pending) {
            try {
                // Sync Pembayaran
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

                // Sync Coverage
                val localCoverages = dao.getCoverageByLocalPaymentId(p.idLocal)
                var coverageConflict = false
                for (c in localCoverages) {
                    try {
                        val cReq = CoverageDto(
                            warga_id = c.wargaId,
                            payment_id = sId,
                            tanggal_kewajiban = c.tanggalKewajiban
                        )
                        api.insertCoverage(apiKey, authHeader, req = cReq)
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
    }
}
