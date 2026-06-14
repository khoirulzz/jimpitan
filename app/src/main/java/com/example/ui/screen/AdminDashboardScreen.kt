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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.local.entity.WargaEntity
import com.example.data.remote.ProfileDto
import com.example.data.repository.WargaArrearsInfo
import com.example.ui.viewmodel.JimpitanViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import io.github.g0dkar.qrcode.QRCode
import java.io.OutputStream
import java.util.Locale

private val GreenPrimary = Color(0xFF138A4A)
private val GreenDark = Color(0xFF0F6E3B)
private val BackgroundWhite = Color(0xFFF6F8F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: JimpitanViewModel,
    onLogout: () -> Unit,
    onNavigateWargaDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val wargaList by viewModel.allWarga.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val weekRevenue by viewModel.weekRevenue.collectAsState()
    val monthRevenue by viewModel.monthRevenue.collectAsState()
    val arrearsCount by viewModel.todayArrearsCount.collectAsState()
    val arrearsWarga by viewModel.arrearsWargaList.collectAsState()
    val arrearsInfoList by viewModel.arrearsInfoList.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val petugasList by viewModel.petugasList.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddWargaDialog by remember { mutableStateOf(false) }
    var showAddPetugasDialog by remember { mutableStateOf(false) }
    var selectedWargaForQr by remember { mutableStateOf<WargaEntity?>(null) }
    var showExportSnackbar by remember { mutableStateOf("") }


    // Build arrears info whenever list changes
    LaunchedEffect(arrearsWarga) {
        if (arrearsWarga.isNotEmpty()) viewModel.buildArrearsInfoList(arrearsWarga)
    }

    // Load petugas when tab selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) viewModel.loadPetugas()
    }

    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.importWargaFromExcel(
                uri = uri,
                context = context,
                onSuccess = { Toast.makeText(context, "Berhasil mengimpor data warga!", Toast.LENGTH_SHORT).show() },
                onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundWhite)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Green Header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(GreenDark, GreenPrimary))
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dashboard Admin", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text("Jimpitan Digital RT 03 / RW 01", fontSize = 12.sp, color = Color.White.copy(0.75f))
                        }
                        Row {
                            IconButton(onClick = {
                                viewModel.exportLaporanPdf(
                                    context = context,
                                    onSuccess = { showExportSnackbar = "✓ Laporan PDF disimpan ke Downloads" },
                                    onError = { showExportSnackbar = "Gagal membuat laporan PDF" }
                                )
                            }) {
                                Icon(Icons.Outlined.PictureAsPdf, null, tint = Color.White)
                            }
                            IconButton(onClick = {
                                viewModel.exportQrSheetPdf(
                                    context = context,
                                    onSuccess = { showExportSnackbar = "✓ QR Sheet PDF disimpan ke Downloads" },
                                    onError = { showExportSnackbar = "Gagal membuat QR Sheet" }
                                )
                            }) {
                                Icon(Icons.Outlined.QrCode2, null, tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.syncNow() }) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.logout(); onLogout() }) {
                                Icon(Icons.Default.ExitToApp, null, tint = Color.White)
                            }
                        }
                    }

                    // Stats Row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AdminStatChip("Hari Ini", "Rp%,d".format(todayRevenue ?: 0))
                        }
                        item {
                            AdminStatChip("Minggu Ini", "Rp%,d".format(weekRevenue ?: 0))
                        }
                        item {
                            AdminStatChip("Bulan Ini", "Rp%,d".format(monthRevenue ?: 0))
                        }
                        item {
                            AdminStatChip("Warga", "${wargaList.size} / $arrearsCount tunggak")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Tab Row ───────────────────────────────────────────────────────
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
                    text = { Text("Tunggakan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.AssignmentLate, null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = GreenPrimary,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Kelola Warga", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.People, null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = GreenPrimary,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Petugas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Badge, null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = GreenPrimary,
                    unselectedContentColor = Color.Gray
                )
            }

            // ── Tab Content ───────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> ArrearsTabContent(
                        arrearsInfoList = arrearsInfoList,
                        allWarga = arrearsWarga,
                        onWargaClick = { onNavigateWargaDetail(it.id) }
                    )
                    1 -> CitizensTabContent(
                        wargaList = wargaList,
                        onImportExcel = { excelPickerLauncher.launch("*/*") },
                        onShowQr = { selectedWargaForQr = it },
                        onWargaDetail = { onNavigateWargaDetail(it.id) }
                    )
                    2 -> OfficersTabContent(
                        petugasList = petugasList,
                        onAddPetugas = { showAddPetugasDialog = true }
                    )
                }
            }
        }

        // FAB for tab 1
        if (selectedTab == 1) {
            FloatingActionButton(
                onClick = { showAddWargaDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        }

        // Export Snackbar
        if (showExportSnackbar.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showExportSnackbar = "" }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF1A1C1B)
                ) {
                    Text(showExportSnackbar, color = Color.White)
                }
            }
        }

        if (isSyncing) {

            com.example.ui.components.SupabaseLoader()
        }
    }

    // Dialogs
    if (showAddWargaDialog) {
        AddWargaDialog(
            onDismiss = { showAddWargaDialog = false },
            onConfirm = { nama, rt, rw, nomorRumah, alamat ->
                viewModel.addWarga(nama, rt, rw, nomorRumah, alamat) {
                    showAddWargaDialog = false
                    viewModel.syncNow()
                }
            }
        )
    }

    if (showAddPetugasDialog) {
        AddPetugasDialog(
            onDismiss = { showAddPetugasDialog = false },
            onConfirm = { email, nama, pass ->
                viewModel.addPetugas(email, nama, pass,
                    onSuccess = {
                        showAddPetugasDialog = false
                        viewModel.loadPetugas()
                        Toast.makeText(context, "Petugas berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { Toast.makeText(context, "Gagal mendaftarkan petugas.", Toast.LENGTH_LONG).show() }
                )
            }
        )
    }

    if (selectedWargaForQr != null) {
        QrCodeDisplayDialog(
            warga = selectedWargaForQr!!,
            onDismiss = { selectedWargaForQr = null },
            context = context
        )
    }
}

// ─── Admin Stat Chip ──────────────────────────────────────────────────────────

@Composable
fun AdminStatChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(label, color = Color.White.copy(0.8f), fontSize = 10.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ─── Tunggakan Tab ────────────────────────────────────────────────────────────

@Composable
fun ArrearsTabContent(
    arrearsInfoList: List<WargaArrearsInfo>,
    allWarga: List<WargaEntity>,
    onWargaClick: (WargaEntity) -> Unit
) {
    var filterRt by remember { mutableStateOf("Semua") }
    var filterDays by remember { mutableStateOf("Semua") }

    val rtList = remember(allWarga) { listOf("Semua") + allWarga.map { it.rt }.distinct().sorted() }
    val dayFilters = listOf("Semua", ">3 hari", ">7 hari", "Belum pernah")

    val filtered = remember(arrearsInfoList, filterRt, filterDays) {
        arrearsInfoList
            .filter { if (filterRt == "Semua") true else it.warga.rt == filterRt }
            .filter {
                when (filterDays) {
                    ">3 hari" -> it.arrearsdays > 3
                    ">7 hari" -> it.arrearsdays > 7
                    "Belum pernah" -> it.lastCoverageDate == null
                    else -> true
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        Column(modifier = Modifier.padding(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rtList) { rt ->
                    FilterChip(
                        selected = filterRt == rt,
                        onClick = { filterRt = rt },
                        label = { Text(if (rt == "Semua") "Semua RT" else "RT $rt", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayFilters) { f ->
                    FilterChip(
                        selected = filterDays == f,
                        onClick = { filterDays = f },
                        label = { Text(f, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFBA1A1A),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Semua warga lunas hari ini!", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { info ->
                    ArrearsWargaCard(
                        info = info,
                        onClick = { onWargaClick(info.warga) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArrearsWargaCard(info: WargaArrearsInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFBA1A1A).copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    info.warga.nama.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBA1A1A),
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.warga.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                Text(
                    "Rumah ${info.warga.nomorRumah} · RT ${info.warga.rt}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (info.lastCoverageDate != null) {
                    Text("Terakhir: ${info.lastCoverageDate}", fontSize = 11.sp, color = Color.Gray)
                } else {
                    Text("Belum pernah bayar", fontSize = 11.sp, color = Color(0xFFBA1A1A))
                }
            }
            // Days badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFBA1A1A).copy(0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "${info.arrearsdays} hari",
                    color = Color(0xFFBA1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ─── Citizens Tab ─────────────────────────────────────────────────────────────

@Composable
fun CitizensTabContent(
    wargaList: List<WargaEntity>,
    onImportExcel: () -> Unit,
    onShowQr: (WargaEntity) -> Unit,
    onWargaDetail: (WargaEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterRt by remember { mutableStateOf("Semua") }

    val rtList = remember(wargaList) { listOf("Semua") + wargaList.map { it.rt }.distinct().sorted() }

    val filtered = remember(wargaList, searchQuery, filterRt) {
        wargaList.filter { warga ->
            val matchSearch = searchQuery.isBlank() ||
                    warga.nama.contains(searchQuery, ignoreCase = true) ||
                    warga.nomorRumah.contains(searchQuery, ignoreCase = true)
            val matchRt = filterRt == "Semua" || warga.rt == filterRt
            matchSearch && matchRt
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search + import row
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama atau nomor rumah...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = GreenPrimary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedTextColor = Color.DarkGray,
                    unfocusedTextColor = Color.DarkGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RT filter chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(rtList) { rt ->
                        FilterChip(
                            selected = filterRt == rt,
                            onClick = { filterRt = rt },
                            label = { Text(if (rt == "Semua") "Semua" else "RT $rt", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                // Import button
                OutlinedButton(
                    onClick = onImportExcel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import", fontSize = 12.sp)
                }
            }
            Text(
                "${filtered.size} dari ${wargaList.size} warga",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada warga yang cocok", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { warga ->
                    WargaListCard(
                        warga = warga,
                        onCardClick = { onWargaDetail(warga) },
                        onQrClick = { onShowQr(warga) }
                    )
                }
            }
        }
    }
}

@Composable
fun WargaListCard(
    warga: WargaEntity,
    onCardClick: () -> Unit,
    onQrClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GreenPrimary.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    warga.nama.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(warga.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                Text(
                    "No. ${warga.nomorRumah}  ·  RT ${warga.rt} / RW ${warga.rw}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            // QR icon — separate clickable from card
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(GreenPrimary.copy(0.08f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onQrClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.QrCode, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ─── Officers Tab ─────────────────────────────────────────────────────────────

@Composable
fun OfficersTabContent(
    petugasList: List<ProfileDto>,
    onAddPetugas: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Add button at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${petugasList.size} Petugas Terdaftar",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Button(
                onClick = onAddPetugas,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah", fontSize = 13.sp, color = Color.White)
            }
        }

        if (petugasList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Badge, null, tint = Color.Gray.copy(0.4f), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada petugas terdaftar", color = Color.Gray)
                    Text("atau sedang memuat...", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(petugasList) { petugas ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(GreenPrimary.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    petugas.nama.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(petugas.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                                Text(petugas.id.take(20) + "...", fontSize = 11.sp, color = Color.Gray)
                            }
                            Box(
                                modifier = Modifier
                                    .background(GreenPrimary.copy(0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Petugas", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
fun AddPetugasDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Akun Petugas", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nama, onValueChange = { nama = it },
                    label = { Text("Nama Petugas") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    )
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    )
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password (min 6 karakter)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    )
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
            ) { Text("Daftarkan", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
        }
    )
}

@Composable
fun QrCodeDisplayDialog(
    warga: WargaEntity,
    onDismiss: () -> Unit,
    context: Context
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
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(warga.nama, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                Text("ID: ${warga.qrUuid}", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = saveQrToGallery(context, qrBitmap, "QR_${warga.qrUuid}")
                    Toast.makeText(
                        context,
                        if (success) "QR berhasil disimpan ke Galeri!" else "Gagal menyimpan QR.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unduh QR", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = Color.Gray) }
        }
    )
}

@Composable
fun AddWargaDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var rt by remember { mutableStateOf("03") }
    var rw by remember { mutableStateOf("01") }
    var nomorRumah by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Warga Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nama, onValueChange = { nama = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rt, onValueChange = { rt = it },
                        label = { Text("RT") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = rw, onValueChange = { rw = it },
                        label = { Text("RW") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray
                        )
                    )
                }
                OutlinedTextField(
                    value = nomorRumah, onValueChange = { nomorRumah = it },
                    label = { Text("Nomor Rumah") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    )
                )
                OutlinedTextField(
                    value = alamat, onValueChange = { alamat = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && rt.isNotBlank() && nomorRumah.isNotBlank()) {
                        onConfirm(nama, rt, rw, nomorRumah, alamat)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text("Simpan", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
        }
    )
}

// ─── QR Generation Helpers ────────────────────────────────────────────────────

fun generateQrCodeBitmap(content: String, name: String, idCode: String, size: Int = 600): Bitmap {
    val pngBytes = QRCode(content).render().getBytes()
    val qrBitmap = android.graphics.BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)

    val extraHeight = 120
    val bitmap = Bitmap.createBitmap(size, size + extraHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val srcRect = android.graphics.Rect(0, 0, qrBitmap.width, qrBitmap.height)
    val dstRect = android.graphics.Rect(0, 0, size, size)
    canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)

    val paintName = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val paintId = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 22f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText(name, (size / 2).toFloat(), (size + 40).toFloat(), paintName)
    canvas.drawText("ID: $idCode", (size / 2).toFloat(), (size + 85).toFloat(), paintId)
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
        } else false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
