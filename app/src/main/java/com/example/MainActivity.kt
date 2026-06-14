package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.di.Injection
import com.example.ui.AppNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JimpitanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full edge-to-edge — content extends behind status bar & nav bar
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val repository = Injection.provideRepository(this)

        // Schedule periodic background sync via WorkManager
        val syncConstraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncWorkRequest =
            androidx.work.PeriodicWorkRequestBuilder<com.example.data.sync.SyncWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(syncConstraints)
                .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "JimpitanSyncWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val jimpitanViewModel: JimpitanViewModel = viewModel(
                    factory = JimpitanViewModel.Factory(repository, applicationContext)
                )
                // No Scaffold wrapper here — each screen handles its own insets
                AppNavGraph(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = jimpitanViewModel
                )
            }
        }
    }
}
