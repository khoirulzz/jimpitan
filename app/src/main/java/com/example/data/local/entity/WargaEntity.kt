package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warga")
data class WargaEntity(
    @PrimaryKey val id: String,
    val qrUuid: String,
    val nama: String,
    val rt: String,
    val rw: String,
    val nomorRumah: String,
    val alamat: String,
    val isActive: Boolean,
    val updatedAt: Long,
    val noWa: String? = null
)
