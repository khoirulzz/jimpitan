package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WargaEntity
import com.example.ui.viewmodel.JimpitanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: JimpitanViewModel,
    onLogout: () -> Unit
) {
    val wargaList by viewModel.allWarga.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val arrearsCount by viewModel.todayArrearsCount.collectAsState()
    val arrearsWarga by viewModel.arrearsWargaList.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val GreenPrimary = Color(0xFF138A4A)
    val GreenDark = Color(0xFF0F6E3B)
    val BackgroundWhite = Color(0xFFF8F9FA)

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Admin", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Citizen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Admin metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Warga Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Warga", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${wargaList.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
                
                // Menunggak Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Menunggak", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$arrearsCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBA1A1A))
                    }
                }

                // Pemasukan Card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Jimpitan Hari Ini", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Rp%,d".format(todayRevenue ?: 0), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Warga Menunggak Hari Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (arrearsWarga.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Semua warga lunas hari ini!", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(arrearsWarga) { warga ->
                        ArrearsWargaItem(warga = warga, GreenPrimary = GreenPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddDialog) {
        AddWargaDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nama, rt, rw, nomorRumah, alamat ->
                viewModel.addWarga(nama, rt, rw, nomorRumah, alamat) {
                    showAddDialog = false
                    viewModel.syncNow()
                }
            },
            GreenPrimary = GreenPrimary
        )
    }
}

@Composable
fun ArrearsWargaItem(warga: WargaEntity, GreenPrimary: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(warga.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Rumah: ${warga.nomorRumah} | RT ${warga.rt} RW ${warga.rw}", fontSize = 12.sp, color = Color.Gray)
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFFBA1A1A).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Menunggak", color = Color(0xFFBA1A1A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddWargaDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit,
    GreenPrimary: Color
) {
    var nama by remember { mutableStateOf("") }
    var rt by remember { mutableStateOf("") }
    var rw by remember { mutableStateOf("") }
    var nomorRumah by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Warga Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedBorderColor = GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rt,
                        onValueChange = { rt = it },
                        label = { Text("RT") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray,
                            focusedBorderColor = GreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rw,
                        onValueChange = { rw = it },
                        label = { Text("RW") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray,
                            focusedBorderColor = GreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = nomorRumah,
                    onValueChange = { nomorRumah = it },
                    label = { Text("Nomor Rumah") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedBorderColor = GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedBorderColor = GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && rt.isNotBlank() && rw.isNotBlank() && nomorRumah.isNotBlank()) {
                        onConfirm(nama, rt, rw, nomorRumah, alamat)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Simpan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}
