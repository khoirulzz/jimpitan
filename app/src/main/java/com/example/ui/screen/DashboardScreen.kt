package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JimpitanViewModel
import java.util.Calendar

// ─── Color tokens ─────────────────────────────────────────────────────────────
private val GreenPrimary = Color(0xFF138A4A)
private val GreenDark = Color(0xFF0F6E3B)
private val GreenMid = Color(0xFF0D7A40)
private val BackgroundWhite = Color(0xFFF6F8F7)

// ─── Time-based greeting ──────────────────────────────────────────────────────
private fun getGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 4..10 -> "Selamat Pagi,"
        in 11..14 -> "Selamat Siang,"
        in 15..17 -> "Selamat Sore,"
        else -> "Selamat Malam,"
    }
}

@Composable
fun DashboardScreen(
    viewModel: JimpitanViewModel,
    onNavigateScan: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val wargaList by viewModel.allWarga.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val todayPaidCount by viewModel.todayPaidCount.collectAsState()
    val todayArrearsCount by viewModel.todayArrearsCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val allPembayaran by viewModel.allPembayaran.collectAsState()

    val pendingCount = allPembayaran.count { it.syncStatus == "PENDING" }
    val greeting = remember { getGreeting() }
    val displayName = userName ?: "Petugas"

    Box(modifier = Modifier.fillMaxSize().background(BackgroundWhite)) {
        // ── Full-screen scrollable content ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Green Header — extends to status bar ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GreenDark, GreenMid, GreenPrimary),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        ),
                        shape = RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                        .padding(top = 20.dp, bottom = 80.dp) // bottom leaves room for scan button overlap
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                greeting,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                displayName,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.18f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Petugas Jimpitan",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        // Notification bell
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notifikasi",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ── SCAN QR Button — overlaps header curve ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-64).dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(24.dp, CircleShape, spotColor = GreenDark.copy(alpha = 0.4f))
                        .background(Color.White, CircleShape)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(listOf(GreenPrimary, GreenDark)),
                            shape = CircleShape
                        )
                        .clickable { onNavigateScan() }
                        .testTag("scan_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "SCAN QR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Tap untuk scan",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ── Content Cards ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-48).dp)
                    .padding(horizontal = 20.dp)
            ) {
                // Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.HowToReg,
                        iconBg = GreenPrimary.copy(alpha = 0.1f),
                        iconTint = GreenPrimary,
                        label = "Sudah Bayar",
                        value = "$todayPaidCount",
                        unit = "Orang"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PersonOff,
                        iconBg = Color(0xFFBA1A1A).copy(alpha = 0.1f),
                        iconTint = Color(0xFFBA1A1A),
                        label = "Menunggak",
                        value = "$todayArrearsCount",
                        unit = "Orang",
                        valueColor = Color(0xFFBA1A1A)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Revenue Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(GreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AccountBalanceWallet, null, tint = GreenPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pemasukan Hari Ini", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Rp%,d".format(todayRevenue ?: 0),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.TrendingUp, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sync Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (isConnected) viewModel.syncNow() },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    (if (isConnected) GreenPrimary else Color.Gray).copy(alpha = 0.1f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CloudSync,
                                null,
                                tint = if (isConnected) GreenPrimary else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Status Sinkronisasi", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isConnected) "Online" else "Offline",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) GreenPrimary else Color.Gray
                            )
                            if (pendingCount > 0) {
                                Text(
                                    "$pendingCount pending sync",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF4900C)
                                )
                            } else {
                                Text(
                                    if (isConnected) "Tekan untuk sinkronisasi" else "Sync otomatis saat online",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isConnected) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                                null,
                                tint = if (isConnected) GreenPrimary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Outlined.ChevronRight, null, tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Nav bar bottom padding
            }
        }

        // ── Syncing Overlay ────────────────────────────────────────────────────
        if (isSyncing) {
            com.example.ui.components.SupabaseLoader()
        }

        // ── Bottom Navigation Bar ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNavBar(
                currentRoute = "dashboard",
                onDashboard = {},
                onHistory = onNavigateHistory,
                onProfile = onNavigateProfile,
                GreenPrimary = GreenPrimary
            )
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    valueColor: Color = Color(0xFF138A4A)
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(unit, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onDashboard: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
    GreenPrimary: Color = Color(0xFF138A4A)
) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = onDashboard,
            icon = {
                Icon(
                    if (currentRoute == "dashboard") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = null
                )
            },
            label = { Text("Dashboard", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.12f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = currentRoute == "history",
            onClick = onHistory,
            icon = {
                Icon(
                    if (currentRoute == "history") Icons.Filled.Schedule else Icons.Outlined.Schedule,
                    contentDescription = null
                )
            },
            label = { Text("Riwayat", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.12f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onProfile,
            icon = {
                Icon(
                    if (currentRoute == "profile") Icons.Filled.Person else Icons.Outlined.PersonOutline,
                    contentDescription = null
                )
            },
            label = { Text("Profil", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.12f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}
