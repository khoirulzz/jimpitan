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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JimpitanViewModel

@Composable
fun DashboardScreen(
    viewModel: JimpitanViewModel,
    onNavigateScan: () -> Unit
) {
    val wargaList by viewModel.allWarga.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val todayPaidCount by viewModel.todayPaidCount.collectAsState()
    val todayArrearsCount by viewModel.todayArrearsCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val GreenPrimary = Color(0xFF138A4A)
    val GreenDark = Color(0xFF0F6E3B)
    val BackgroundWhite = Color(0xFFF8F9FA)

    Scaffold(
        containerColor = BackgroundWhite,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenPrimary,
                        selectedTextColor = GreenPrimary,
                        indicatorColor = GreenPrimary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = { Text("Riwayat") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                    label = { Text("Profil") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Background with curve
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GreenDark, GreenPrimary)
                        ),
                        shape = RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp)
                    )
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Selamat Malam,",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Pak Slamet",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
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
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                }
            }

            // Main Content Scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(180.dp))
            
                // Scan QR Button (Giant Floating)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .shadow(16.dp, CircleShape, spotColor = GreenDark.copy(alpha = 0.5f))
                                .background(Color.White, CircleShape)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary)
                                .clickable { onNavigateScan() }
                                .testTag("scan_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "SCAN QR",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Tap untuk scan",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sudah Bayar Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(GreenPrimary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.HowToReg, contentDescription = null, tint = GreenPrimary)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Sudah Bayar", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$todayPaidCount", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                Text("Orang", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        
                        // Menunggak Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFBA1A1A).copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.PersonOff, contentDescription = null, tint = Color(0xFFBA1A1A))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Menunggak", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$todayArrearsCount", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBA1A1A))
                                Text("Orang", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pemasukan Hari Ini
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(GreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = GreenPrimary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pemasukan Hari Ini", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Rp%,d".format(todayRevenue ?: 0), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(GreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Sinkronisasi
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (isConnected) viewModel.syncNow() },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background((if (isConnected) GreenPrimary else Color.Gray).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = if (isConnected) GreenPrimary else Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Status Sinkronisasi", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(if (isConnected) "Online" else "Offline", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isConnected) GreenPrimary else Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(if (isConnected) "Tekan untuk sinkronisasi" else "Sinkronisasi otomatis saat online", fontSize = 11.sp, color = Color.Gray)
                            }
                            Icon(if (isConnected) Icons.Outlined.Wifi else Icons.Outlined.WifiOff, contentDescription = null, tint = if (isConnected) GreenPrimary else Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            if (isSyncing) {
                com.example.ui.components.SupabaseLoader()
            }
        }
    }
}
