package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.JimpitanDao
import com.example.data.local.entity.CoverageHistoryEntity
import com.example.data.local.entity.PembayaranEntity
import com.example.data.local.entity.WargaEntity

@Database(
    entities = [WargaEntity::class, PembayaranEntity::class, CoverageHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jimpitanDao(): JimpitanDao
}
