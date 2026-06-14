package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PembayaranEntity
import com.example.ui.viewmodel.JimpitanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: JimpitanViewModel,
    onBack: () -> Unit
) {
    val allPembayaran by viewModel.allPembayaran.collectAsState()

    val GreenPrimary = Color(0xFF138A4A)
    val GreenDark = Color(0xFF0F6E3B)
    val BackgroundWhite = Color(0xFFF8F9FA)

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displaySdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val today = remember { sdf.format(Date()) }

    // Filter options
    var selectedFilter by remember { mutableStateOf("Semua") }
    val filterOptions = listOf("Semua", "Hari Ini", "Minggu Ini", "Bulan Ini")
    var showExportSnackbar by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current


    val filtered = remember(allPembayaran, selectedFilter) {
        when (selectedFilter) {
            "Hari Ini" -> allPembayaran.filter { it.tanggalBayar == today }
            "Minggu Ini" -> {
                val weekAgo = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -7)
                }.let { sdf.format(it.time) }
                allPembayaran.filter { it.tanggalBayar >= weekAgo }
            }
            "Bulan Ini" -> {
                val monthStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                }.let { sdf.format(it.time) }
                allPembayaran.filter { it.tanggalBayar >= monthStart }
            }
            else -> allPembayaran
        }
    }

    val totalNominal = filtered.sumOf { it.nominal }

    Scaffold(
        containerColor = BackgroundWhite,
        snackbarHost = {
            if (showExportSnackbar.isNotEmpty()) {
                Snackbar(
                    action = { TextButton(onClick = { showExportSnackbar = "" }) { Text("OK") } }
                ) { Text(showExportSnackbar) }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.exportLaporanPdf(
                            context = context,
                            onSuccess = { showExportSnackbar = "✓ PDF disimpan ke Downloads/Jimpitan" },
                            onError = { showExportSnackbar = "Gagal mengekspor PDF" }
                        )
                    }) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(GreenDark, GreenPrimary))
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Transaksi", color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Text("${filtered.size} Transaksi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Nominal", color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Text("Rp%,d".format(totalNominal), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { option ->
                    FilterChip(
                        selected = selectedFilter == option,
                        onClick = { selectedFilter = option },
                        label = { Text(option, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Transaction List
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Color.Gray.copy(0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada transaksi", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { item ->
                        PaymentHistoryItem(item = item, displaySdf = displaySdf, GreenPrimary = GreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryItem(
    item: PembayaranEntity,
    displaySdf: SimpleDateFormat,
    GreenPrimary: Color
) {
    val statusColor = when (item.syncStatus) {
        "SYNCED" -> Color(0xFF138A4A)
        "PENDING" -> Color(0xFFF4900C)
        "CONFLICT" -> Color(0xFFBA1A1A)
        "FAILED" -> Color(0xFFBA1A1A)
        else -> Color.Gray
    }
    val statusLabel = when (item.syncStatus) {
        "SYNCED" -> "Terkirim"
        "PENDING" -> "Menunggu Sync"
        "CONFLICT" -> "Konflik"
        "FAILED" -> "Gagal"
        else -> item.syncStatus
    }

    val parsedDate = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.tanggalBayar)
    } catch (e: Exception) { null }
    val displayDate = if (parsedDate != null) displaySdf.format(parsedDate) else item.tanggalBayar

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GreenPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Payments, contentDescription = null, tint = GreenPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Rp%,d".format(item.nominal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                Text("${item.coverageDays} hari coverage · $displayDate", fontSize = 12.sp, color = Color.Gray)
            }
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
