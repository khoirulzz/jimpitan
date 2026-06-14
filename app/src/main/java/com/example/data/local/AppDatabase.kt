package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.JimpitanDao
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.local.entity.WargaEntity

/**
 * Room Database definition.
 * version = 2: bumped for multi-RT query support and new DAO queries.
 * fallbackToDestructiveMigration() is set in Injection.kt so no manual Migration class needed.
 */
@Database(
    entities = [WargaEntity::class, PembayaranEntity::class, CoverageHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jimpitanDao(): JimpitanDao
}
