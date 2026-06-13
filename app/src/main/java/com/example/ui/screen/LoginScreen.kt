package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JimpitanViewModel
import com.example.ui.viewmodel.LoginState

@Composable
fun LoginScreen(
    viewModel: JimpitanViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    val GreenPrimary = Color(0xFF138A4A)
    val GreenDark = Color(0xFF0F6E3B)
    val BackgroundWhite = Color(0xFFF8F9FA)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundWhite
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Background with curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GreenDark, GreenPrimary)
                        ),
                        shape = RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Selamat Datang,",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Jimpitan Digital",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(64.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Login Petugas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Email address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.DarkGray,
                                unfocusedTextColor = Color.DarkGray,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                unfocusedContainerColor = BackgroundWhite,
                                focusedContainerColor = BackgroundWhite,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = GreenPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.DarkGray,
                                unfocusedTextColor = Color.DarkGray,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                unfocusedContainerColor = BackgroundWhite,
                                focusedContainerColor = BackgroundWhite,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = GreenPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { viewModel.login(email, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("login_button"),
                            enabled = loginState !is LoginState.Loading,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            if (loginState is LoginState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Sign In", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        if (loginState is LoginState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                (loginState as LoginState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

