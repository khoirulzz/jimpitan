package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coverage_history")
data class CoverageHistoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val id: String?, // UUID from server, null if not synced
    val wargaId: String,
    val paymentId: String?, // string because it usually links to UUID string on server
    val paymentLocalId: Long?,
    val tanggalKewajiban: String, // format YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)
