package com.example.chess.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.viewmodel.GameMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.example.chess.data.AdManager

@Composable
fun HomeScreen(
    viewModel: ChessViewModel,
    onNavigateToGame: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToTrainer: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    var showAiDialog by remember { mutableStateOf(false) }
    var pendingGameModeAndAction by remember { mutableStateOf<GameMode?>(null) }

    var showCompulsoryLoginDialog by remember { mutableStateOf(false) }
    var pendingActionAfterLogin by remember { mutableStateOf<(() -> Unit)?>(null) }
    var loginEmail by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authErrorMsg by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        // Celestial theme background: Lord Krishna vs Shakuni/Chaos fateful match representation
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_bg_dharma_chaos_1780980906641),
                contentDescription = "The Fateful Chess Game - Dharma vs Chaos Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Premium semi-transparent dark gradient overlay
            // Confirms highest accessibility standards by securing contrast for text and controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A).copy(alpha = 0.78f),
                                Color(0xFF111827).copy(alpha = 0.88f),
                                Color(0xFF0B0E14).copy(alpha = 0.98f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .clickable { onNavigateToProfile() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.3f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stats.avatar, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = stats.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Theme: ${stats.theme}", color = Color(0xFF34D399), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "⚙️", 
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable { onNavigateToProfile() }
                                .padding(4.dp)
                        )
                        Text(
                            text = "🚪", 
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable { showLogoutConfirmDialog = true }
                                .padding(4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Master Chess Pro",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFF94A3B8))
                        ),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "♙ ♘ ♗ ♖ ♕ ♔",
                    fontSize = 28.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                val gameState by viewModel.uiState.collectAsState()
                val hasActiveGame = gameState.game != null && !gameState.isGameOver

                if (hasActiveGame) {
                    Button(
                        onClick = {
                            if (!stats.cloudSynced || stats.email.isEmpty()) {
                                pendingActionAfterLogin = { onNavigateToGame() }
                                showCompulsoryLoginDialog = true
                            } else {
                                onNavigateToGame()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(64.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("▶️ Resume Game", fontSize = 18.sp, color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        val action = {
                            if (hasActiveGame) {
                                pendingGameModeAndAction = GameMode.AI
                            } else {
                                showAiDialog = true
                            }
                        }
                        if (!stats.cloudSynced || stats.email.isEmpty()) {
                            pendingActionAfterLogin = action
                            showCompulsoryLoginDialog = true
                        } else {
                            action()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Play vs Computer", fontSize = 18.sp, color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .clickable {
                            val action = {
                                if (hasActiveGame) {
                                    pendingGameModeAndAction = GameMode.PVP
                                } else {
                                    viewModel.startNewGame(GameMode.PVP, 1)
                                    onNavigateToGame()
                                }
                            }
                            if (!stats.cloudSynced || stats.email.isEmpty()) {
                                pendingActionAfterLogin = action
                                showCompulsoryLoginDialog = true
                            } else {
                                action()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Play vs Friend", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .clickable {
                            if (!stats.cloudSynced || stats.email.isEmpty()) {
                                pendingActionAfterLogin = { onNavigateToTrainer() }
                                showCompulsoryLoginDialog = true
                            } else {
                                onNavigateToTrainer()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Opening Trainer", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Glass Card (Esports Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color(0xFF34D399).copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clickable { onNavigateToStats() }
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stats.playerTitle.uppercase(), 
                            color = Color(0xFF34D399), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            EsportsStatItem("KR RATIO", String.format("%.2f", stats.krRatio), Color.White)
                            EsportsStatItem("WIN RATE", "${stats.winRate.toInt()}%", Color(0xFF34D399))
                            EsportsStatItem("SCORE", "${stats.battleScore}", Color(0xFFFBBF24))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Sleek Outline Logout Option on main page
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🚪", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SECURE ARENA LOG OUT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Pinned/Reserved Banner Ad Area at the bottom
            BannerAdArea(modifier = Modifier.navigationBarsPadding())
        }

        if (showAiDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showAiDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select Difficulty", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        val levels = listOf("Beginner", "Intermediate", "Advanced", "Expert")
                        levels.forEachIndexed { index, level ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        showAiDialog = false
                                        viewModel.startNewGame(GameMode.AI, index + 1)
                                        onNavigateToGame()
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAiDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        pendingGameModeAndAction?.let { mode ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable { pendingGameModeAndAction = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp).widthIn(max = 400.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Active Game Found",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You already have an active game in progress. Would you like to resume it, or start a completely new game instead?",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        Button(
                            onClick = {
                                pendingGameModeAndAction = null
                                onNavigateToGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("▶️ Resume Active Game", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                if (mode == GameMode.AI) {
                                    showAiDialog = true
                                } else {
                                    viewModel.startNewGame(GameMode.PVP, 1)
                                    onNavigateToGame()
                                }
                                pendingGameModeAndAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("➕ Start New Game", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        TextButton(
                            onClick = { pendingGameModeAndAction = null },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (showCompulsoryLoginDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable { /* Block clicks to background */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👑", fontSize = 28.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "CHALLENGER PORTAL",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A secure authentication is compulsory before entering the tournament arena. Enter your email only to resume or register automatically.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { 
                                loginEmail = it 
                                authErrorMsg = null
                            },
                            label = { Text("Enter Email Address", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        authErrorMsg?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "⚠️ $msg",
                                color = Color(0xFFF87171),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                if (loginEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(loginEmail.trim()).matches()) {
                                    authErrorMsg = "Please supply a valid email address."
                                    return@Button
                                }
                                isAuthenticating = true
                                authErrorMsg = null
                                viewModel.authenticateWithEmailOnly(
                                    email = loginEmail.trim(),
                                    onSuccess = {
                                        isAuthenticating = false
                                        showCompulsoryLoginDialog = false
                                        pendingActionAfterLogin?.invoke()
                                        pendingActionAfterLogin = null
                                    },
                                    onFailure = { error ->
                                        isAuthenticating = false
                                        authErrorMsg = error
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isAuthenticating
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF0F172A),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "ENTER TOURNAMENT ARENA", 
                                    color = Color(0xFF0F172A), 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        TextButton(
                            onClick = {
                                showCompulsoryLoginDialog = false
                                pendingActionAfterLogin = null
                                authErrorMsg = null
                            },
                            enabled = !isAuthenticating
                        ) {
                            Text("CLOSE PORTAL", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showLogoutConfirmDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable { showLogoutConfirmDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 400.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🚪", fontSize = 28.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "LOG OUT SECURELY?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Are you sure you want to sign out from the Arena database? Your local session will be securely unlinked.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                showLogoutConfirmDialog = false
                                viewModel.logoutCloudAcc()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "YES, LOG OUT", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        TextButton(
                            onClick = { showLogoutConfirmDialog = false }
                        ) {
                            Text("CANCEL", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EsportsStatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BannerAdArea(
    modifier: Modifier = Modifier
) {
    var isAdFailedToLoad by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isAdFailedToLoad) {
            // Accidental click prevention & professional labeled area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SPONSORED",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Banner Ad Area",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Real Google Mobile Ads SDK Banner loading via AndroidView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            adUnitId = AdManager.BANNER_AD_UNIT_ID
                            setAdSize(AdSize.BANNER)
                            adListener = object : AdListener() {
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    // Collapse space gracefully on fail as per AdMob guidelines
                                    isAdFailedToLoad = true
                                }
                                override fun onAdLoaded() {
                                    isAdFailedToLoad = false
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    update = {
                        // The AdView updates automatically when loadAd is finished
                    }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
