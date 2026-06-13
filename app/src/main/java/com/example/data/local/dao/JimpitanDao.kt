package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.local.entity.WargaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JimpitanDao {

    @Query("SELECT * FROM warga ORDER BY nama ASC")
    fun getAllWarga(): Flow<List<WargaEntity>>

    @Query("SELECT * FROM warga WHERE qrUuid = :qrUuid LIMIT 1")
    suspend fun getWargaByQr(qrUuid: String): WargaEntity?

    @Query("SELECT * FROM warga WHERE id = :id LIMIT 1")
    suspend fun getWargaById(id: String): WargaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWargaList(warga: List<WargaEntity>)

    @Query("SELECT * FROM pembayaran ORDER BY createdAt DESC")
    fun getAllPembayaran(): Flow<List<PembayaranEntity>>

    @Query("SELECT * FROM pembayaran WHERE syncStatus = 'PENDING'")
    suspend fun getPendingPembayaran(): List<PembayaranEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPembayaran(pembayaran: PembayaranEntity): Long

    @Query("UPDATE pembayaran SET syncStatus = :status, serverId = :serverId WHERE idLocal = :idLocal")
    suspend fun updatePembayaranStatus(idLocal: Long, status: String, serverId: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverageHistory(coverage: CoverageHistoryEntity)

    @Query("SELECT * FROM coverage_history WHERE wargaId = :wargaId ORDER BY tanggalKewajiban DESC")
    suspend fun getCoverageByWarga(wargaId: String): List<CoverageHistoryEntity>
    
    @Query("SELECT SUM(nominal) FROM pembayaran WHERE tanggalBayar = :date")
    fun getRevenueByDate(date: String): Flow<Int?>

    @Query("SELECT COUNT(DISTINCT wargaId) FROM coverage_history WHERE tanggalKewajiban = :date")
    fun getPaidCountByDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM warga WHERE isActive = 1 AND id NOT IN (SELECT wargaId FROM coverage_history WHERE tanggalKewajiban = :date)")
    fun getArrearsCountByDate(date: String): Flow<Int>

    @Query("SELECT * FROM warga WHERE isActive = 1 AND id NOT IN (SELECT wargaId FROM coverage_history WHERE tanggalKewajiban = :date)")
    fun getArrearsWargaByDate(date: String): Flow<List<WargaEntity>>

    @Query("SELECT * FROM coverage_history WHERE paymentLocalId = :paymentLocalId")
    suspend fun getCoverageByLocalPaymentId(paymentLocalId: Long): List<CoverageHistoryEntity>

    @Query("UPDATE coverage_history SET id = :serverId, paymentId = :paymentServerId WHERE localId = :localId")
    suspend fun updateCoverageServerId(localId: Long, serverId: String, paymentServerId: String)

    @Query("SELECT COUNT(*) FROM warga")
    suspend fun getWargaCount(): Int

    @Query("DELETE FROM warga")
    suspend fun clearWarga()
}
