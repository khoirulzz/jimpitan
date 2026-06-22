package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.JimpitanDao
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.local.entity.PengeluaranEntity
import com.example.data.local.entity.WargaEntity

/**
 * Room Database definition.
 * version = 3: added Pengeluaran table for Buku Kas.
 * version = 4: added createdByName to PembayaranEntity for offline petugas name display.
 * fallbackToDestructiveMigration() is set in Injection.kt so no manual Migration class needed.
 */
@Database(
    entities = [WargaEntity::class, PembayaranEntity::class, CoverageHistoryEntity::class, PengeluaranEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jimpitanDao(): JimpitanDao
}
