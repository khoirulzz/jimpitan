package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.di.Injection

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = Injection.provideRepository(applicationContext)
        return try {
            repository.fetchWarga()
            repository.fetchPembayaranFromServer()
            repository.fetchCoverageHistoryFromServer()
            repository.fetchPengeluaranFromServer()
            repository.syncPending()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
