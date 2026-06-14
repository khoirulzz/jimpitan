package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.WargaDetailData
import com.example.ui.viewmodel.JimpitanViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WargaDetailScreen(
    wargaId: String,
    viewModel: JimpitanViewModel,
    onBack: () -> Unit
) {
    val GreenPrimary = Color(0xFF138A4A)
    val GreenDark = Color(0xFF0F6E3B)
    val BackgroundWhite = Color(0xFFF8F9FA)

    val today = Calendar.getInstance()
    var displayYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(today.get(Calendar.MONTH) + 1) } // 1-based

    val wargaDetail by viewModel.wargaDetail.collectAsState()

    LaunchedEffect(wargaId, displayYear, displayMonth) {
        viewModel.loadWargaDetail(wargaId, displayYear, displayMonth)
    }

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Detail Warga", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (wargaDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else {
            val detail = wargaDetail!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Profile Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenPrimary)
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.White.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = detail.warga.nama.take(1).uppercase(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(detail.warga.nama, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "RT ${detail.warga.rt} / RW ${detail.warga.rw}  ·  No. ${detail.warga.nomorRumah}",
                                fontSize = 13.sp,
                                color = Color.White.copy(0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(detail.warga.alamat, fontSize = 12.sp, color = Color.White.copy(0.7f))
                        }
                    }
                }

                // Stats Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Lunas",
                            value = "${detail.totalLunasDays} Hari",
                            color = GreenPrimary,
                            icon = Icons.Outlined.CheckCircle
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Bayar",
                            value = "Rp%,d".format(detail.totalNominal),
                            color = Color(0xFF1565C0),
                            icon = Icons.Outlined.AccountBalanceWallet
                        )
                    }
                }

                // Coverage until
                item {
                    if (detail.lastCoverageDate != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val displaySdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                        val parsed = runCatching { sdf.parse(detail.lastCoverageDate) }.getOrNull()
                        val todayDate = sdf.format(Calendar.getInstance().time)
                        val isLunas = detail.lastCoverageDate >= todayDate
                        val statusColor = if (isLunas) GreenPrimary else Color(0xFFBA1A1A)
                        val statusText = if (isLunas)
                            "✓ Lunas sampai ${parsed?.let { displaySdf.format(it) } ?: detail.lastCoverageDate}"
                        else
                            "⚠ Coverage terakhir: ${parsed?.let { displaySdf.format(it) } ?: detail.lastCoverageDate}"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isLunas) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Calendar Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Month Navigation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (displayMonth == 1) {
                                        displayMonth = 12; displayYear--
                                    } else {
                                        displayMonth--
                                    }
                                }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan sebelumnya", tint = GreenPrimary)
                                }
                                Text(
                                    "${monthNames[displayMonth - 1]} $displayYear",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                                IconButton(onClick = {
                                    if (displayMonth == 12) {
                                        displayMonth = 1; displayYear++
                                    } else {
                                        displayMonth++
                                    }
                                }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Bulan berikutnya", tint = GreenPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Day headers
                            val dayHeaders = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                dayHeaders.forEach { day ->
                                    Text(
                                        text = day,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Calendar grid
                            PaymentCalendarGrid(
                                year = displayYear,
                                month = displayMonth,
                                coverageMap = detail.coverageMap,
                                GreenPrimary = GreenPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Legend
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                LegendItem(color = GreenPrimary, label = "Lunas")
                                LegendItem(color = Color(0xFFBA1A1A), label = "Tunggak")
                                LegendItem(color = Color.LightGray, label = "Belum tiba")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentCalendarGrid(
    year: Int,
    month: Int,
    coverageMap: Map<String, Boolean>,
    GreenPrimary: Color
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = sdf.format(Calendar.getInstance().time)

    // Day of week of first day (Monday=1)
    var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2 // convert to Mon=0
    if (firstDayOfWeek < 0) firstDayOfWeek += 7

    val cells = mutableListOf<Int?>()
    repeat(firstDayOfWeek) { cells.add(null) }
    for (d in 1..daysInMonth) cells.add(d)
    while (cells.size % 7 != 0) cells.add(null)

    val rows = cells.chunked(7)
    rows.forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (day != null) {
                        val dateStr = sdf.format(Calendar.getInstance().apply {
                            set(year, month - 1, day)
                        }.time)
                        val isToday = dateStr == today
                        val isPast = dateStr <= today
                        val isCovered = coverageMap[dateStr]

                        val bgColor = when {
                            isCovered == true -> GreenPrimary
                            isCovered == false && isPast -> Color(0xFFBA1A1A)
                            else -> Color.Transparent
                        }
                        val textColor = when {
                            isCovered != null -> Color.White
                            isToday -> GreenPrimary
                            else -> Color.DarkGray.copy(alpha = 0.5f)
                        }
                        val borderColor = if (isToday && isCovered == null) GreenPrimary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(bgColor, CircleShape)
                                .border(1.5.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
