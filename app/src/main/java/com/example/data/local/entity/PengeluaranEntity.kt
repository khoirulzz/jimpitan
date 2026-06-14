package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pengeluaran")
data class PengeluaranEntity(
    @PrimaryKey(autoGenerate = true) val idLocal: Long = 0,
    val serverId: String?, // UUID dari server, null jika belum sync
    val nominal: Int,
    val tanggal: String, // format YYYY-MM-DD
    val keterangan: String,
    val createdBy: String,
    val syncStatus: String // "SYNCED" atau "PENDING"
)
