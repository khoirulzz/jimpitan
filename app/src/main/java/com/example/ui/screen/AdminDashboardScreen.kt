package com.example.ui.screen

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WargaEntity
import com.example.ui.viewmodel.JimpitanViewModel
import qrcode.QRCode
import java.io.OutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: JimpitanViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val wargaList by viewModel.allWarga.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val arrearsCount by viewModel.todayArrearsCount.collectAsState()
    val arrearsWarga by viewModel.arrearsWargaList.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddWargaDialog by remember { mutableStateOf(false) }
    var showAddPetugasDialog by remember { mutableStateOf(false) }
    
    var selectedWargaForQr by remember { mutableStateOf<WargaEntity?>(null) }

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
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Now", tint = Color.White)
                    }
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
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddWargaDialog = true },
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Citizen")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddPetugasDialog = true },
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Officer")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Metrics overview banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPrimary)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pemasukan Hari Ini", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("Rp%,d".format(todayRevenue ?: 0), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Warga / Arrears", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("${wargaList.size} / $arrearsCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                // Tab selection row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = GreenPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GreenPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tunggakan", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Outlined.AssignmentLate, contentDescription = null) },
                        selectedContentColor = GreenPrimary,
                        unselectedContentColor = Color.Gray
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Kelola Warga", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Outlined.People, contentDescription = null) },
                        selectedContentColor = GreenPrimary,
                        unselectedContentColor = Color.Gray
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Kelola Petugas", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                        selectedContentColor = GreenPrimary,
                        unselectedContentColor = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    when (selectedTab) {
                        0 -> ArrearsTabContent(arrearsWarga, GreenPrimary)
                        1 -> CitizensTabContent(wargaList, GreenPrimary) { selectedWargaForQr = it }
                        2 -> OfficersTabContent(GreenPrimary)
                    }
                }
            }

            if (isSyncing) {
                com.example.ui.components.SupabaseLoader()
            }
        }
    }

    // Citizen Addition Dialog
    if (showAddWargaDialog) {
        AddWargaDialog(
            onDismiss = { showAddWargaDialog = false },
            onConfirm = { nama, rt, rw, nomorRumah, alamat ->
                viewModel.addWarga(nama, rt, rw, nomorRumah, alamat) {
                    showAddWargaDialog = false
                    viewModel.syncNow()
                }
            },
            GreenPrimary = GreenPrimary
        )
    }

    // Officer Addition Dialog
    if (showAddPetugasDialog) {
        AddPetugasDialog(
            onDismiss = { showAddPetugasDialog = false },
            onConfirm = { email, nama, pass ->
                viewModel.addPetugas(email, nama, pass,
                    onSuccess = {
                        showAddPetugasDialog = false
                        Toast.makeText(context, "Petugas berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        Toast.makeText(context, "Gagal mendaftarkan petugas. Periksa koneksi.", Toast.LENGTH_LONG).show()
                    }
                )
            },
            GreenPrimary = GreenPrimary
        )
    }

    // QR Code Display Dialog
    if (selectedWargaForQr != null) {
        QrCodeDisplayDialog(
            warga = selectedWargaForQr!!,
            onDismiss = { selectedWargaForQr = null },
            context = context,
            GreenPrimary = GreenPrimary
        )
    }
}

@Composable
fun ArrearsTabContent(arrearsWarga: List<WargaEntity>, GreenPrimary: Color) {
    if (arrearsWarga.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Semua warga lunas hari ini!", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(arrearsWarga) { warga ->
                ArrearsWargaItem(warga = warga, GreenPrimary = GreenPrimary)
            }
        }
    }
}

@Composable
fun CitizensTabContent(wargaList: List<WargaEntity>, GreenPrimary: Color, onShowQr: (WargaEntity) -> Unit) {
    if (wargaList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Data warga kosong. Tambah data warga baru.", color = Color.Gray, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wargaList) { warga ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowQr(warga) },
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
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Show QR",
                            tint = GreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficersTabContent(GreenPrimary: Color) {
    // List default seed officers for display
    val dummyOfficers = listOf(
        Pair("Slamet Santoso", "slamet@gempala.com"),
        Pair("Joko Widodo", "joko@gempala.com")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dummyOfficers) { officer ->
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
                            .size(40.dp)
                            .background(GreenPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(officer.first, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                        Text(officer.second, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPetugasDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    GreenPrimary: Color
) {
    var email by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Akun Petugas", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Nama Petugas") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedBorderColor = GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedBorderColor = GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
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
                    if (email.isNotBlank() && nama.isNotBlank() && password.length >= 6) {
                        onConfirm(email, nama, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Daftarkan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}

@Composable
fun QrCodeDisplayDialog(
    warga: WargaEntity,
    onDismiss: () -> Unit,
    context: Context,
    GreenPrimary: Color
) {
    val qrCodeContent = "JMP|${warga.qrUuid}"
    val qrBitmap = remember(qrCodeContent) { generateQrCodeBitmap(qrCodeContent, warga.nama, warga.qrUuid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Kode QR Warga",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = warga.nama,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "ID: ${warga.qrUuid}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = saveQrToGallery(context, qrBitmap, "QR_${warga.qrUuid}")
                    if (success) {
                        Toast.makeText(context, "QR Code berhasil disimpan ke Galeri!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal menyimpan QR Code.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unduh QR", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Color.Gray)
            }
        }
    )
}

// QR Code generation drawing centering details on bitmap
fun generateQrCodeBitmap(content: String, name: String, idCode: String, size: Int = 600): Bitmap {
    val qrCode = QRCode.ofSquares().build(content)
    val grid = qrCode.rawLayout()
    val gridWidth = grid.size
    val gridHeight = grid[0].size

    val qrHeight = size
    val extraHeight = 120
    val bitmap = Bitmap.createBitmap(size, size + extraHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw White Background
    canvas.drawColor(AndroidColor.WHITE)

    // Draw QR Blocks
    val paintBlock = Paint().apply {
        color = AndroidColor.BLACK
    }
    val blockWidth = size.toFloat() / gridWidth
    val blockHeight = qrHeight.toFloat() / gridHeight

    for (y in 0 until gridHeight) {
        for (x in 0 until gridWidth) {
            if (grid[y][x]) {
                canvas.drawRect(
                    x * blockWidth,
                    y * blockHeight,
                    (x + 1) * blockWidth,
                    (y + 1) * blockHeight,
                    paintBlock
                )
            }
        }
    }

    // Draw text (name and ID) at the bottom
    val paintTextName = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        fontWeight = FontWeight.Bold.value
    }
    
    val paintTextId = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 22f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText(name, (size / 2).toFloat(), (size + 40).toFloat(), paintTextName)
    canvas.drawText("Jimpitan ID: $idCode", (size / 2).toFloat(), (size + 85).toFloat(), paintTextId)

    return bitmap
}

fun saveQrToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Jimpitan")
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
    return try {
        val stream: OutputStream? = resolver.openOutputStream(uri)
        if (stream != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
