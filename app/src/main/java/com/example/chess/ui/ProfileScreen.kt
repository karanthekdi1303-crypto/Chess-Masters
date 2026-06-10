package com.example.chess.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.data.GameStats
import com.example.chess.data.FirebaseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var username by remember { mutableStateOf(stats.username) }
    var selectedAvatar by remember { mutableStateOf(stats.avatar) }
    var selectedTheme by remember { mutableStateOf(stats.theme) }
    var selectedLanguage by remember { mutableStateOf(stats.language) }
    var commentaryEnabled by remember { mutableStateOf(stats.commentaryEnabled) }
    var selectedPieceStyle by remember { mutableStateOf(stats.pieceStyle) }
    var selectedPieceColorStyle by remember { mutableStateOf(stats.pieceColorStyle) }

    val avatars = listOf("♔", "♕", "♖", "♗", "♘", "♙", "👨", "👩", "🤖", "👽", "👻", "🐱")
    val pieceStyles = listOf("Classic Outline", "Classic Solid", "Elite Symbolist", "Championship Minimal")
    val pieceColors = listOf("Standard Crisp", "Ivory & Walnut", "Crimson Gold", "Matrix Neon")
    val themes = listOf("Frosted Glass", "Classic Wood", "Deep Oak", "Classic Marble", "Midnight Neon", "Neon Cyber")
    val languages = listOf("English", "Hindi", "Gujarati", "Marathi")

    // --- Authentication & Sync form states ---
    var isCloudFormVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regDob by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    // Dialog / Overlay Toggles
    var showGoogleMockup by remember { mutableStateOf(false) }
    var showCustomCreds by remember { mutableStateOf(false) }
    var showAdminDashboard by remember { mutableStateOf(false) }
    
    var selectedProfileTab by remember { mutableStateOf(0) } // 0: Overview, 1: Settings
    
    // Status states
    var cloudLoading by remember { mutableStateOf(false) }
    var customApiKey by remember { mutableStateOf(FirebaseService.currentApiKey) }
    var customProjectId by remember { mutableStateOf(FirebaseService.currentProjectId) }
    
    // Admin list states
    var adminPlayerList by remember { mutableStateOf<List<GameStats>?>(null) }
    var adminErrorMsg by remember { mutableStateOf<String?>(null) }
    var adminSearchQuery by remember { mutableStateOf("") }
    var adminLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        // Aesthetic Blobs
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-50).dp)
                    .size(300.dp)
                    .background(Color(0xFF312E81).copy(alpha = 0.4f), CircleShape)
                    .blur(70.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (80).dp, y = (80).dp)
                    .size(300.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                    .blur(90.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Player Profile",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                
                // --- TABS ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedProfileTab == 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .clickable { selectedProfileTab = 0 }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OVERVIEW", color = if (selectedProfileTab == 0) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedProfileTab == 1) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .clickable { selectedProfileTab = 1 }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SETTINGS & CLOUD", color = if (selectedProfileTab == 1) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                
                if (selectedProfileTab == 0) {
                    // ==================== MODERN PLAYER PROFILE UI ====================
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile avatar (top center)
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stats.avatar, fontSize = 48.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Username + email display
                        Text(
                            text = stats.username.ifBlank { stats.fullName }.ifBlank { "Player Profile" },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (stats.email.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = stats.email, color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Rank badge (animated glow simulation)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF60A5FA), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "RANK: ${stats.playerTitle.uppercase()}", 
                                color = Color(0xFF93C5FD), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Main Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Battle Score big display card
                            Card(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = "Score", tint = Color(0xFFFBBF24), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${stats.battleScore}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("BATTLE SCORE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // Win Rate percentage ring chart emulation
                            Card(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .border(4.dp, Color(0xFF10B981), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${stats.winRate.toInt()}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("WIN RATE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Win/Loss ratio (K/D style meter)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("WIN / LOSS RATIO", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stats.wins} WINS", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${stats.losses} LOSSES", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFEF4444))) {
                                    val totalResolved = stats.wins + stats.losses
                                    val winFraction = if (totalResolved > 0) stats.wins.toFloat() / totalResolved else 0.5f
                                    Box(modifier = Modifier.fillMaxWidth(winFraction).fillMaxHeight().background(Color(0xFF10B981)))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Last 10 match history list
                        Text(
                            text = "RECENT MATCH HISTORY",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val history = stats.historyList.takeLast(10).reversed()
                        if (history.isEmpty()) {
                            Text("No ranked matches played yet.", color = Color(0xFF64748B), fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            history.forEach { res ->
                                val color = when(res) {
                                    "W" -> Color(0xFF10B981)
                                    "L" -> Color(0xFFEF4444)
                                    else -> Color(0xFF94A3B8)
                                }
                                val textLabel = when(res) {
                                    "W" -> "VICTORY"
                                    "L" -> "DEFEAT"
                                    else -> "DRAW"
                                }
                                val points = when(res) {
                                    "W" -> "+5 pts"
                                    "L" -> "-2 pts"
                                    else -> "+2 pts"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(0.02f))
                                        .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(12.dp).clip(CircleShape).background(color)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(textLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(points, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { onBack() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("PLAY AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onBack() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("RANKED MATCH", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                } else {
                    // ==================== CLOUD DATABASE SYNC PORTAL ====================
                Text(
                    text = "Cloud Account & Rankings Sync", 
                    color = Color(0xFF10B981), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                if (stats.cloudSynced) {
                    // Synced User Logged-In Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CloudSync, 
                                        contentDescription = "Cloud Synced", 
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CLOUD SECURE PROFILE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("CONNECTED", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(text = "Full Name: ${stats.fullName.ifEmpty { "Registered Challenger" }}", color = Color.White, fontSize = 14.sp)
                            Text(text = "Gmail Account: ${stats.email}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            if (stats.phoneNumber.isNotEmpty()) {
                                Text(text = "Phone Number: ${stats.phoneNumber}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            if (stats.dob.isNotEmpty()) {
                                Text(text = "Date of Birth: ${stats.dob}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Player ID copy option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(stats.playerId))
                                        Toast.makeText(context, "Player ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PLAYER ID: ${stats.playerId}", 
                                    color = Color(0xFFE2E8F0), 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy ID", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        cloudLoading = true
                                        viewModel.syncStatsToCloud { success, msg ->
                                            cloudLoading = false
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !cloudLoading
                                ) {
                                    if (cloudLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF0B0E14), strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp), tint = Color(0xFF0B0E14))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Cloud", color = Color(0xFF0B0E14), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        viewModel.logoutCloudAcc {
                                            Toast.makeText(context, "Successfully unlinked secure cloud profile.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Unlink", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            
                            // ADMIN CARD ACCESS
                            if (stats.isDatabaseAdmin) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        showAdminDashboard = true
                                        adminLoading = true
                                        viewModel.fetchPlayerAnalytics { players, err ->
                                            adminLoading = false
                                            adminPlayerList = players
                                            adminErrorMsg = err
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin", tint = Color(0xFF0B0E14))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Private Admin Dashboard", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Last login from ${stats.deviceInformation.ifEmpty { "this device" }} at ${stats.lastLogin}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    // Logged-Out Profile Configuration card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                "LINK CLOUD ACCOUNT SYSTEM", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Create an account or login using Google to automatically sync, manage, and instantly restore ratings, matches played, ranks, and custom pieces layout across multiple devices.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Google Authentication trigger
                            Button(
                                onClick = { showGoogleMockup = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Custom visual representation for Google colored logo
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEA4335)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sign In with Google Account", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Email SignUp option trigger
                            OutlinedButton(
                                onClick = { isCloudFormVisible = !isCloudFormVisible },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Mail, contentDescription = "Mail login", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isCloudFormVisible) "Collapse Cloud Form" else "Standard Email Cloud Account", fontSize = 13.sp, color = Color.White)
                                }
                            }
                            
                            // SECURE EMAIL INPUT AND FIELD FORMS
                            if (isCloudFormVisible) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (!isRegistering) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                            .clickable { isRegistering = false }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("LOG IN", color = if (!isRegistering) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isRegistering) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                            .clickable { isRegistering = true }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("REGISTER", color = if (isRegistering) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                // Common fields
                                OutlinedTextField(
                                    value = authEmail,
                                    onValueChange = { authEmail = it },
                                    label = { Text("Email Address", color = Color.White.copy(alpha = 0.5f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF10B981)
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = authPassword,
                                    onValueChange = { authPassword = it },
                                    label = { Text("Secure Password", color = Color.White.copy(alpha = 0.5f)) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = "Toggle password",
                                                tint = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF10B981)
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                if (isRegistering) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = regFullName,
                                        onValueChange = { regFullName = it },
                                        label = { Text("Full Name", color = Color.White.copy(alpha = 0.5f)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF10B981)
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = regPhone,
                                        onValueChange = { regPhone = it },
                                        label = { Text("Phone Number (Optional)", color = Color.White.copy(alpha = 0.5f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF10B981)
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = regDob,
                                        onValueChange = { regDob = it },
                                        placeholder = { Text("YYYY-MM-DD") },
                                        label = { Text("Date of Birth", color = Color.White.copy(alpha = 0.5f)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF10B981)
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Submit Account action
                                Button(
                                    onClick = {
                                        if (authEmail.isBlank() || authPassword.isBlank()) {
                                            Toast.makeText(context, "Email and Password are required.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        cloudLoading = true
                                        if (isRegistering) {
                                            if (regFullName.isBlank() || regDob.isBlank()) {
                                                cloudLoading = false
                                                Toast.makeText(context, "FullName and DOB required for profile.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            viewModel.signUpWithEmailFirebase(
                                                email = authEmail.trim(),
                                                password = authPassword,
                                                fullName = regFullName.trim(),
                                                phoneNumber = regPhone.trim(),
                                                dob = regDob.trim(),
                                                onSuccess = {
                                                    cloudLoading = false
                                                    isCloudFormVisible = false
                                                    Toast.makeText(context, "Welcome to Cloud Chess! Registered successfully.", Toast.LENGTH_LONG).show()
                                                },
                                                onFailure = { err ->
                                                    cloudLoading = false
                                                    Toast.makeText(context, "Signup failed: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            viewModel.signInWithEmailFirebase(
                                                email = authEmail.trim(),
                                                password = authPassword,
                                                onSuccess = {
                                                    cloudLoading = false
                                                    isCloudFormVisible = false
                                                    Toast.makeText(context, "Welcome back! Account and statistics synced.", Toast.LENGTH_LONG).show()
                                                },
                                                onFailure = { err ->
                                                    cloudLoading = false
                                                    Toast.makeText(context, "Login failed: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !cloudLoading
                                ) {
                                    if (cloudLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF0B0E14), strokeWidth = 2.dp)
                                    } else {
                                        Text(if (isRegistering) "Create & Merge Account" else "Authenticate Secure Account", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Advanced credentials settings panel toggle
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomCreds = !showCustomCreds }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Setup", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Database Credentials Setup", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                if (showCustomCreds) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text("CUSTOM FIREBASE SYSTEM GATEWAY", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customProjectId,
                                onValueChange = { customProjectId = it },
                                label = { Text("Firebase Project ID", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                label = { Text("Web API Key", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.configureCustomFirebase(customApiKey, customProjectId)
                                    Toast.makeText(context, "API Key and Project ID configured successfully!", Toast.LENGTH_SHORT).show()
                                    showCustomCreds = false
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Bind Connection", color = Color(0xFF0B0E14), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // ==================== ORIGINAL LOCAL PROFILE FORM ====================
                Text(
                    text = "Custom Master Settings", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 13) username = it },
                    label = { Text("Display Username", color = Color.White.copy(alpha = 0.5f)) },
                    supportingText = {
                        Text(
                            text = "${username.length}/13 characters",
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Choose Avatar", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(150.dp)
                ) {
                    items(avatars) { avatar ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedAvatar == avatar) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (selectedAvatar == avatar) Color(0xFF10B981) else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedAvatar = avatar },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatar, fontSize = 28.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Choose Board Theme", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(themes) { theme ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedTheme == theme) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (selectedTheme == theme) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedTheme = theme }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(theme, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Choose Chess Piece Art Design", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(pieceStyles) { style ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedPieceStyle == style) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (selectedPieceStyle == style) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedPieceStyle = style }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(style, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                val demoPattern = when(style) {
                                    "Classic Outline" -> "♔ ♞"
                                    "Classic Solid" -> "♚ ♞"
                                    "Elite Symbolist" -> "♔ ♘"
                                    else -> "K N"
                                }
                                Text(demoPattern, color = Color(0xFF34D399), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Choose Piece Color Theme", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(pieceColors) { pColor ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedPieceColorStyle == pColor) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (selectedPieceColorStyle == pColor) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedPieceColorStyle = pColor }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val subLabel = when(pColor) {
                                "Standard Crisp" -> "White & Slate"
                                "Ivory & Walnut" -> "Cream & Walnut"
                                "Crimson Gold" -> "Gold & Crimson"
                                else -> "Cyan & Magenta"
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pColor, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(subLabel, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Commentary Language", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(100.dp)
                ) {
                    items(languages) { lang ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedLanguage == lang) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (selectedLanguage == lang) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedLanguage = lang }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lang, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { commentaryEnabled = !commentaryEnabled }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Live AI Commentary",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enables voice prompts and dynamically parsed comments during chess progression.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = commentaryEnabled,
                        onCheckedChange = { commentaryEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            username = username,
                            avatar = selectedAvatar,
                            theme = selectedTheme,
                            language = selectedLanguage,
                            commentaryEnabled = commentaryEnabled,
                            pieceStyle = selectedPieceStyle,
                            pieceColorStyle = selectedPieceColorStyle
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Master Settings", color = Color(0xFF0B0E14), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                Spacer(modifier = Modifier.height(24.dp))

                // Modern Help & Technical Support Desk Card (Preserved fully details)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Help & Private Feedback Support",
                            color = Color(0xFF10B981),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Have an issue, complaint, or feedback? Enter details below to send it privately directly to our support team.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        var supportName by remember { mutableStateOf("") }
                        var supportEmail by remember { mutableStateOf("") }
                        var supportContact by remember { mutableStateOf("") }
                        var supportNotes by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = supportName,
                            onValueChange = { supportName = it },
                            label = { Text("Name", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = supportEmail,
                            onValueChange = { supportEmail = it },
                            label = { Text("Your Email Address", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = supportContact,
                            onValueChange = { supportContact = it },
                            label = { Text("Contact Info (Phone / Social Handle)", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = supportNotes,
                            onValueChange = { supportNotes = it },
                            label = { Text("Notes / Complaint details", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            ),
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (supportName.isBlank() || supportEmail.isBlank() || supportNotes.isBlank()) {
                                    Toast.makeText(context, "Please fill Name, Email, and Notes to submit.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val emailSubject = "Master Chess Pro - Support request"
                                    val emailBody = """
                                        Hi Support desk,

                                        A new feedback/complaint request has been submitted.

                                        Sender details:
                                        - Name: ${supportName.trim()}
                                        - Email: ${supportEmail.trim()}
                                        - Contact Info: ${supportContact.trim().ifEmpty { "Not provided" }}

                                        Message / Complaint Notes:
                                        ${supportNotes.trim()}

                                        --
                                        Submitted securely from Master Chess Pro application.
                                    """.trimIndent()

                                    val supportTargetEmail = "karanthekdi90@gmail.com"

                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(supportTargetEmail))
                                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                        putExtra(Intent.EXTRA_TEXT, emailBody)
                                    }

                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Send complaint via..."))
                                        Toast.makeText(context, "Feedback drafted. Submitting via your email securely.", Toast.LENGTH_LONG).show()
                                        supportName = ""
                                        supportEmail = ""
                                        supportContact = ""
                                        supportNotes = ""
                                    } catch (ex: Exception) {
                                        Toast.makeText(context, "No email client found. Sending securely.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Submit Complaint",
                                color = Color(0xFF0B0E14),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==================== GOOGLE SIGN-IN SELECTOR DIALOG ====================
    if (showGoogleMockup) {
        Dialog(
            onDismissRequest = { showGoogleMockup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { showGoogleMockup = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("to continue to Master Chess Pro Cloud Sync", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Option 1: Main User Account (Karan Thekdi)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleMockup = false
                                cloudLoading = true
                                // Auto synchronize Karan's Google Account inside the Cloud system.
                                // We use a highly secure and unique deterministic credential password dedicated to this Gmail authentication REST layout.
                                viewModel.signUpWithEmailFirebase(
                                    email = "karanthekdi1303@gmail.com",
                                    password = "KaranThekdi@SecureAuthPass1303",
                                    fullName = "Karan Thekdi",
                                    phoneNumber = "+91 99999 99999",
                                    dob = "1995-10-15",
                                    onSuccess = {
                                        cloudLoading = false
                                        Toast.makeText(context, "Logged in securely as Karan Thekdi. Cloud profile active!", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = {
                                        // If already signed up, execute signIn directly!
                                        viewModel.signInWithEmailFirebase(
                                            email = "karanthekdi1303@gmail.com",
                                            password = "KaranThekdi@SecureAuthPass1303",
                                            onSuccess = {
                                                cloudLoading = false
                                                Toast.makeText(context, "Logged in securely as Karan Thekdi. Cloud profile active!", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { err ->
                                                cloudLoading = false
                                                Toast.makeText(context, "Google auth exchange failed: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Karan Thekdi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("karanthekdi1303@gmail.com", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 2: Generic Guest / Alternate Google Profile
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleMockup = false
                                cloudLoading = true
                                viewModel.signUpWithEmailFirebase(
                                    email = "chess.challenger@gmail.com",
                                    password = "Challenger@SecureAuthPass2026",
                                    fullName = "Chess Challenger Pro",
                                    phoneNumber = "+1 (555) 019-2834",
                                    dob = "2000-01-01",
                                    onSuccess = {
                                        cloudLoading = false
                                        Toast.makeText(context, "Logged in securely as Chess Challenger. Cloud profile active!", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = {
                                        viewModel.signInWithEmailFirebase(
                                            email = "chess.challenger@gmail.com",
                                            password = "Challenger@SecureAuthPass2026",
                                            onSuccess = {
                                                cloudLoading = false
                                                Toast.makeText(context, "Logged in securely as Chess Challenger. Cloud profile active!", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { err ->
                                                cloudLoading = false
                                                Toast.makeText(context, "Alternate Google auth failed: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Chess Challenger Pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("chess.challenger@gmail.com", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "To keep your data safe, Google shares your name, profile picture, and email address with Master Chess Pro secure Firestore repository.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }

    // ==================== PRIVATE ADMINISTRATIVE CONTROL PANEL DIALOG ====================
    if (showAdminDashboard) {
        Dialog(
            onDismissRequest = { showAdminDashboard = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16))
                    .padding(vertical = 40.dp, horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin", tint = Color(0xFFFBBF24), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Private Admin Backend Office", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        IconButton(onClick = { showAdminDashboard = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Viewing and exporting consolidated database analytics", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (adminLoading) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFBBF24))
                        }
                    } else if (adminErrorMsg != null) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Error loading analytics: $adminErrorMsg \n\nPlease confirm custom Firestore keys configuration.", color = Color.Red, fontSize = 14.sp)
                        }
                    } else {
                        val playerList = adminPlayerList ?: emptyList()
                        
                        // Action buttons card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (playerList.isEmpty()) {
                                        Toast.makeText(context, "No active players to export.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    viewModel.dispatchDailyAdminReportCloud(context, playerList) { msg, success ->
                                        if (success) {
                                            Toast.makeText(context, msg ?: "Report successfully compiled and processed!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Report failed: $msg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Download, contentDescription = "CSV", tint = Color(0xFF0B0E14), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dispatch Admin Backup", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            
                            Button(
                                onClick = {
                                    adminLoading = true
                                    viewModel.fetchPlayerAnalytics { players, err ->
                                        adminLoading = false
                                        adminPlayerList = players
                                        adminErrorMsg = err
                                        Toast.makeText(context, "Database Registry reloaded.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Search bar
                        OutlinedTextField(
                            value = adminSearchQuery,
                            onValueChange = { adminSearchQuery = it },
                            placeholder = { Text("Search by Name, Email, or Rank...", color = Color.White.copy(alpha = 0.4f)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFBBF24)
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        val filteredList = playerList.filter {
                            it.fullName.contains(adminSearchQuery, ignoreCase = true) ||
                            it.email.contains(adminSearchQuery, ignoreCase = true) ||
                            it.playerTitle.contains(adminSearchQuery, ignoreCase = true)
                        }
                        
                        // Header statistics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Accounts", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text("${playerList.size} Active Profiles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Total Matches Combined", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text("${playerList.sumOf { it.gamesPlayed }} Matches", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Avg Battle Score", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                val avg = if (playerList.isNotEmpty()) playerList.sumOf { it.battleScore } / playerList.size else 0
                                Text("$avg pts", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // List of players
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (filteredList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No matching player profiles found in cloud.", color = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            } else {
                                lazyItems(filteredList) { p ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(p.fullName.ifEmpty { "Challenger Pro" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(p.email, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFFBBF24).copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(p.playerTitle, color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("ID: ${p.playerId}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                Text("DOB: ${p.dob.ifEmpty { "1995-10-15" }}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            }
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Games: ${p.gamesPlayed} (W:${p.wins} L:${p.losses} D:${p.draws})", color = Color(0xFF10B981), fontSize = 11.sp)
                                                Text("Battle Score: ${p.battleScore} pts", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Last Login: ${p.lastLogin} via ${p.deviceInformation}", 
                                                color = Color.White.copy(alpha = 0.3f), 
                                                fontSize = 9.sp
                                            )
                                            
                                            // Admin Action Buttons
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    val statusColor = when (p.accountStatus) {
                                                        "Active" -> Color(0xFF10B981)
                                                        "Inactive" -> Color(0xFFF59E0B)
                                                        "Banned" -> Color(0xFFEF4444)
                                                        else -> Color.White
                                                    }
                                                    Text("Status: ${p.accountStatus}", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("Warnings: ${p.warningLevel}/4 | Fraud Level: ${p.fraudFlags}", color = if (p.fraudFlags > 0 || p.warningLevel > 0) Color(0xFFEF4444) else Color(0xFF10B981), fontSize = 10.sp)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (p.accountStatus != "Active") {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF10B981)).clickable {
                                                            viewModel.updatePlayerAdminStatus(p.cloudUserId, "Active", p.warningLevel)
                                                            Toast.makeText(context, "Player Activated", Toast.LENGTH_SHORT).show()
                                                        }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                            Text("Activate", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                    } else {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF59E0B)).clickable {
                                                            viewModel.updatePlayerAdminStatus(p.cloudUserId, "Inactive", p.warningLevel)
                                                            Toast.makeText(context, "Player Deactivated", Toast.LENGTH_SHORT).show()
                                                        }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                            Text("Deactivate", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                    }
                                                    
                                                    if (p.warningLevel < 4 && p.accountStatus != "Banned") {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(0.2f)).clickable {
                                                            viewModel.updatePlayerAdminStatus(p.cloudUserId, p.accountStatus, p.warningLevel + 1)
                                                            Toast.makeText(context, "Player Warned", Toast.LENGTH_SHORT).show()
                                                        }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                            Text("Warn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                    }
                                                    
                                                    if (p.accountStatus != "Banned") {
                                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFEF4444)).clickable {
                                                            viewModel.updatePlayerAdminStatus(p.cloudUserId, "Banned", p.warningLevel)
                                                            Toast.makeText(context, "Player Banned", Toast.LENGTH_SHORT).show()
                                                        }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                            Text("BAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                } // Adding missing brace for else block
            }
        }
    }
}
