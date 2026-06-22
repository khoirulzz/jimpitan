package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pembayaran")
data class PembayaranEntity(
    @PrimaryKey(autoGenerate = true) val idLocal: Long = 0,
    val serverId: String?,
    val wargaId: String,
    val nominal: Int,
    val coverageDays: Int,
    val tanggalBayar: String, // format YYYY-MM-DD
    val createdBy: String,
    val createdByName: String = "", // Nama petugas untuk tampilan offline
    val syncStatus: String, // PENDING, SYNCED, FAILED, CONFLICT
    val createdAt: Long = System.currentTimeMillis()
)
