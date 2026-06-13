package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JimpitanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: JimpitanViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val warga by viewModel.scannedWarga.collectAsState()
    var manualNominal by remember { mutableStateOf("") }

    val isNominalValid = remember(manualNominal) {
        val amount = manualNominal.toIntOrNull()
        amount != null && amount > 0 && amount % 500 == 0
    }

    val quickAmounts = listOf(500, 1000, 2000, 5000)
    
    val GreenPrimary = Color(0xFF138A4A)
    val GreenDark = Color(0xFF0F6E3B)
    val BackgroundWhite = Color(0xFFF8F9FA)

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Input Pembayaran", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite,
                    titleContentColor = Color.DarkGray
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
                    }
                }
            )
        }
    ) { padding ->
        if (warga == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Warga tidak ditemukan atau data belum sinkron.",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Kembali ke Scan") }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(GreenPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = GreenPrimary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Nama Warga", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text("${warga?.nama}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = BackgroundWhite, thickness = 2.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(BackgroundWhite, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Map, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("RT/RW", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Text("${warga?.rt}/${warga?.rw}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(BackgroundWhite, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Home, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Rumah", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Text("${warga?.nomorRumah}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "Nominal Cepat (Rp)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(quickAmounts) { amount ->
                        val isSelected = manualNominal == amount.toString()
                        OutlinedButton(
                            onClick = { manualNominal = amount.toString() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSelected) Color.White else GreenPrimary,
                                containerColor = if (isSelected) GreenPrimary else Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(GreenPrimary)
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Text("Rp $amount", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = manualNominal,
                    onValueChange = { manualNominal = it },
                    placeholder = { Text("Atau masukkan nominal lain") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("nominal_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = GreenPrimary
                    ),
                    leadingIcon = {
                        Text("Rp", modifier = Modifier.padding(start = 16.dp, end = 8.dp), fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                )
                if (manualNominal.isNotEmpty() && !isNominalValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nominal harus kelipatan Rp500 (contoh: 500, 1000, 2000)",
                        color = Color(0xFFBA1A1A),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = {
                        val amount = manualNominal.toIntOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.savePembayaran(amount)
                            onSuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("simpan_button"),
                    shape = CircleShape,
                    enabled = isNominalValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        disabledContentColor = Color.Gray
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Simpan Pembayaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isNominalValid) Color.White else Color.Gray)
                }
            }
        }
    }
}
