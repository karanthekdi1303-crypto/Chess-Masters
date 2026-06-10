package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.viewmodel.LeaderboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val leaderboardState by viewModel.leaderboardState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            viewModel.fetchLeaderboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Statistics", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Material 3 Dark-Themed TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF10B981)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Performance", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF10B981),
                    unselectedContentColor = Color(0xFF64748B)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Global Leaderboard", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF10B981),
                    unselectedContentColor = Color(0xFF64748B)
                )
            }

            if (selectedTab == 0) {
                // MY PERFORMANCE VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (stats.cloudSynced) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = Color(0xFF34D399).copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val letter = if (stats.fullName.isNotEmpty()) stats.fullName.take(1).uppercase() else "C"
                                        Text(letter, color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stats.fullName.ifEmpty { "Cloud Challenger" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("ID: ${stats.playerId} • Securing Gmail: ${stats.email}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("Last login: ${stats.lastLogin}", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Rank Progression Bar
                    Text("RANK: ${stats.playerTitle.uppercase()}", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { stats.winRate / 100f },
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFF34D399),
                        trackColor = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Score Logic
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Battle Score Breakdown", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Win: +5 pts | Draw: +2 pts | Loss: -2 pts\nKR Ratio means King Ratio", color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Total Score: ${stats.battleScore}", 
                                color = Color(0xFFFBBF24), 
                                fontSize = 28.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // Recent Match History
                    Text("Recent Matches (Last 10)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val history = stats.historyList
                    if (history.isEmpty()) {
                        Text("No match history yet.", color = Color(0xFF64748B))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(history) { result ->
                                val (bgColor, textColor, label) = when (result) {
                                    "W" -> Triple(Color(0xFF047857), Color.White, "WIN")
                                    "L" -> Triple(Color(0xFF9F1239), Color.White, "LOSS")
                                    else -> Triple(Color(0xFF334155), Color.White, "DRAW")
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Win/Loss Trend Graph
                    Text("Win Rate History", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            history.forEach {
                                val height = when (it) {
                                    "W" -> 80.dp
                                    "D" -> 40.dp
                                    else -> 10.dp
                                }
                                val color = when (it) {
                                     "W" -> Color(0xFF34D399)
                                     "D" -> Color(0xFFFBBF24)
                                     else -> Color(0xFFEF4444)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(height)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            } else {
                // GLOBAL LEADERBOARD VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Leaderboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Top 10 Worldwide Champions", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = { viewModel.fetchLeaderboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f), contentColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Refresh", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (val state = leaderboardState) {
                        is LeaderboardUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF10B981))
                            }
                        }
                        is LeaderboardUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚠️", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(state.message, color = Color(0xFFF87171), fontSize = 14.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.fetchLeaderboard() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Retry", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is LeaderboardUiState.Success -> {
                            val items = state.topPlayers
                            if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No registered matches yet.", color = Color(0xFF64748B), fontSize = 14.sp)
                                }
                            } else {
                                items.forEachIndexed { index, player ->
                                    val isMe = player.email.isNotEmpty() && player.email == stats.email
                                    
                                    val borderStroke = if (isMe) {
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                                    } else null
                                    
                                    val cardBgColor = if (isMe) {
                                        Color(0xFF1E293B)
                                    } else {
                                        Color(0xFF1E293B).copy(alpha = 0.5f)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                        border = borderStroke
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Rank Badge
                                            Box(
                                                modifier = Modifier.size(36.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                when (index) {
                                                    0 -> Text("🥇", fontSize = 24.sp)
                                                    1 -> Text("🥈", fontSize = 24.sp)
                                                    2 -> Text("🥉", fontSize = 24.sp)
                                                    else -> {
                                                        Surface(
                                                            modifier = Modifier.size(28.dp),
                                                            shape = androidx.compose.foundation.shape.CircleShape,
                                                            color = Color.White.copy(alpha = 0.05f)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = "${index + 1}",
                                                                    color = Color(0xFF94A3B8),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Avatar
                                            Surface(
                                                modifier = Modifier.size(40.dp),
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                                color = if (index == 0) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = player.avatar.ifEmpty { "♔" },
                                                        color = if (index == 0) Color(0xFFFBBF24) else Color.White,
                                                        fontSize = 18.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Name + Details
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = player.fullName.ifEmpty { "Pro Chess Champion" },
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (isMe) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                "YOU", 
                                                                color = Color(0xFF10B981), 
                                                                fontSize = 9.sp, 
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (player.isDatabaseAdmin) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFEF4444).copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                "ADMIN", 
                                                                color = Color(0xFFEF4444), 
                                                                fontSize = 9.sp, 
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "TITLE: ${player.playerTitle.uppercase()} • ID: ${player.playerId}",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Wins: ${player.wins} | Losses: ${player.losses} | Win Rate: ${player.winRate}%",
                                                    color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Score Pillar
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${player.battleScore}",
                                                    color = Color(0xFFFBBF24),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 18.sp
                                                )
                                                Text(
                                                    text = "PTS",
                                                    color = Color(0xFF94A3B8),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 9.sp
                                                )
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
}
