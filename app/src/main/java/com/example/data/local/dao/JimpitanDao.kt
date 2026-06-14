package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.local.entity.WargaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JimpitanDao {

    // ─── Warga ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM warga ORDER BY nama ASC")
    fun getAllWarga(): Flow<List<WargaEntity>>

    @Query("SELECT * FROM warga WHERE qrUuid = :qrUuid LIMIT 1")
    suspend fun getWargaByQr(qrUuid: String): WargaEntity?

    @Query("SELECT * FROM warga WHERE id = :id LIMIT 1")
    suspend fun getWargaById(id: String): WargaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWargaList(warga: List<WargaEntity>)

    @Query("SELECT COUNT(*) FROM warga")
    suspend fun getWargaCount(): Int

    @Query("DELETE FROM warga")
    suspend fun clearWarga()

    // ─── Pembayaran ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM pembayaran ORDER BY createdAt DESC")
    fun getAllPembayaran(): Flow<List<PembayaranEntity>>

    @Query("SELECT * FROM pembayaran WHERE syncStatus = 'PENDING'")
    suspend fun getPendingPembayaran(): List<PembayaranEntity>

    @Query("SELECT * FROM pembayaran WHERE wargaId = :wargaId ORDER BY createdAt DESC")
    fun getPembayaranByWarga(wargaId: String): Flow<List<PembayaranEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPembayaran(pembayaran: PembayaranEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPembayaranList(list: List<PembayaranEntity>)

    @Query("UPDATE pembayaran SET syncStatus = :status, serverId = :serverId WHERE idLocal = :idLocal")
    suspend fun updatePembayaranStatus(idLocal: Long, status: String, serverId: String?)

    @Query("DELETE FROM pembayaran WHERE syncStatus = 'SYNCED'")
    suspend fun clearSyncedPembayaran()

    @Query("DELETE FROM pembayaran")
    suspend fun clearPembayaran()

    // ─── Coverage History ─────────────────────────────────────────────────────

    @Query("SELECT * FROM coverage_history WHERE wargaId = :wargaId ORDER BY tanggalKewajiban DESC")
    suspend fun getCoverageByWarga(wargaId: String): List<CoverageHistoryEntity>

    @Query("SELECT * FROM coverage_history WHERE wargaId = :wargaId AND tanggalKewajiban BETWEEN :startDate AND :endDate ORDER BY tanggalKewajiban ASC")
    suspend fun getCoverageInRange(wargaId: String, startDate: String, endDate: String): List<CoverageHistoryEntity>

    @Query("SELECT MAX(tanggalKewajiban) FROM coverage_history WHERE wargaId = :wargaId")
    suspend fun getLastCoverageDate(wargaId: String): String?

    @Query("SELECT COUNT(*) FROM coverage_history WHERE wargaId = :wargaId")
    suspend fun getTotalLunasByWarga(wargaId: String): Int

    @Query("SELECT * FROM coverage_history WHERE paymentLocalId = :paymentLocalId")
    suspend fun getCoverageByLocalPaymentId(paymentLocalId: Long): List<CoverageHistoryEntity>

    @Query("UPDATE coverage_history SET id = :serverId, paymentId = :paymentServerId WHERE localId = :localId")
    suspend fun updateCoverageServerId(localId: Long, serverId: String, paymentServerId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCoverageHistory(coverage: CoverageHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCoverageHistoryList(list: List<CoverageHistoryEntity>)

    @Query("DELETE FROM coverage_history WHERE id IS NOT NULL")
    suspend fun clearSyncedCoverageHistory()

    @Query("DELETE FROM coverage_history")
    suspend fun clearCoverageHistory()

    // Cek apakah warga sudah punya coverage untuk hari ini (anti double bayar)
    @Query("SELECT COUNT(*) FROM coverage_history WHERE wargaId = :wargaId AND tanggalKewajiban = :date")
    suspend fun hasCoverageOnDate(wargaId: String, date: String): Int

    // ─── Statistik & Metrics ──────────────────────────────────────────────────

    @Query("SELECT SUM(nominal) FROM pembayaran WHERE tanggalBayar = :date")
    fun getRevenueByDate(date: String): Flow<Int?>

    @Query("SELECT SUM(nominal) FROM pembayaran WHERE tanggalBayar BETWEEN :startDate AND :endDate")
    fun getRevenueInRange(startDate: String, endDate: String): Flow<Int?>

    @Query("SELECT COUNT(DISTINCT wargaId) FROM coverage_history WHERE tanggalKewajiban = :date")
    fun getPaidCountByDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM warga WHERE isActive = 1 AND id NOT IN (SELECT wargaId FROM coverage_history WHERE tanggalKewajiban = :date)")
    fun getArrearsCountByDate(date: String): Flow<Int>

    @Query("SELECT * FROM warga WHERE isActive = 1 AND id NOT IN (SELECT wargaId FROM coverage_history WHERE tanggalKewajiban = :date) ORDER BY nama ASC")
    fun getArrearsWargaByDate(date: String): Flow<List<WargaEntity>>

    @Query("SELECT SUM(nominal) FROM pembayaran WHERE wargaId = :wargaId")
    suspend fun getTotalNominalByWarga(wargaId: String): Int?
}
