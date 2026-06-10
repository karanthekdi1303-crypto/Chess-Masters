package com.example.chess.ui

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.viewmodel.GameMode

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun GameScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()

    BackHandler {
        viewModel.pauseGame()
        onBack()
    }

    val context = LocalContext.current
    val ttsEngine = remember { mutableStateOf<TextToSpeech?>(null) }
    val isTtsReady = remember { mutableStateOf(false) }

    DisposableEffect(context, stats.commentaryEnabled) {
        var ttts: TextToSpeech? = null
        if (stats.commentaryEnabled) {
            ttts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // Settings for Indian men voice little bit like Ranbir kapoor
                    ttts?.setPitch(0.85f)
                    ttts?.setSpeechRate(0.9f)
                    ttsEngine.value = ttts
                    isTtsReady.value = true
                }
            }
        }
        onDispose {
            ttts?.stop()
            ttts?.shutdown()
        }
    }

    LaunchedEffect(state.liveCommentary, isTtsReady.value, stats.commentaryEnabled) {
        if (!stats.commentaryEnabled) {
            ttsEngine.value?.stop()
        } else if (isTtsReady.value && state.liveCommentary != null) {
            val lang = stats.language
            val loc = when (lang) {
                "Hindi" -> Locale("en", "IN") // Use en-IN for "Hindi" text as Android Hindi TTS might be tricky to configure directly without language code, but "hi" is better: Locale("hi", "IN")
            	// Realizing Hindi is hi-IN
                "Gujarati" -> Locale("gu", "IN")
                "Marathi" -> Locale("mr", "IN")
                else -> Locale("en", "IN")
            }
            if (lang == "Hindi") {
                ttsEngine.value?.setLanguage(Locale("hi", "IN"))
            } else {
                ttsEngine.value?.setLanguage(loc)
            }
            
            val voices = ttsEngine.value?.voices
            if (voices != null) {
                val indVoices = voices.filter { it.locale.country == "IN" }
                // try choosing a male voice
                val preferred = indVoices.firstOrNull { it.name.lowercase().contains("male") } ?: indVoices.firstOrNull()
                if (preferred != null) {
                    ttsEngine.value?.voice = preferred
                }
            }
            
            ttsEngine.value?.speak(state.liveCommentary, TextToSpeech.QUEUE_FLUSH, null, "comm")
        }
    }

    // Play sound effects
    LaunchedEffect(state.soundTrigger) {
        state.soundTrigger?.let {
            viewModel.clearSoundTrigger()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        // Background Blobs for Glassmorphism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 50.dp) // shift blobs slightly
        ) {
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
                    .offset(x = 50.dp, y = 50.dp)
                    .size(300.dp)
                    .background(Color(0xFF064E3B).copy(alpha = 0.3f), CircleShape)
                    .blur(70.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp)
        ) {
            val game = state.game ?: return@Box

            val stats by viewModel.stats.collectAsState()

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Master Chess Pro",
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White, Color(0xFF94A3B8))
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = if (state.mode == GameMode.AI) "RANKED MATCH" else "LOCAL MULTIPLAYER",
                        color = Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (state.isPaused) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (state.isPaused) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .clickable {
                                if (state.isPaused) {
                                    viewModel.resumeGameExecution()
                                } else {
                                    viewModel.pauseGame()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (state.isPaused) "▶️" else "⏸️", fontSize = 16.sp)
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.pauseGame()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚪", fontSize = 15.sp)
                            Text("Exit", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Opponent Dashboard
            PlayerDashboard(
                name = if (state.mode == GameMode.AI) "Grandmaster AI" else "Player 2",
                subtext = if (state.mode == GameMode.AI) "Level ${state.aiDepth} • 2450 ELO" else "Black Pieces",
                avatar = "🤖",
                colorColors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                timeMs = state.blackTimeMs,
                captured = game.getCapturedPieces(PieceColor.BLACK),
                isTurn = game.turn == PieceColor.BLACK && !state.isGameOver,
                textColor = Color(0xFFCBD5E1),
                pieceStyle = stats.pieceStyle
            )

            Spacer(modifier = Modifier.weight(0.5f))

            val liveMessage = remember(game.moveHistory.size) {
                val whiteCaptured = game.getCapturedPieces(PieceColor.BLACK) 
                val blackCaptured = game.getCapturedPieces(PieceColor.WHITE)
                
                val wPoints = whiteCaptured.sumOf { it.value }
                val bPoints = blackCaptured.sumOf { it.value }
                
                when {
                    stats.winStreak >= 5 && game.moveHistory.size <= 4 -> "Unstoppable! 🔥"
                    stats.historyList.takeLast(3).count { it == "L" } >= 3 && game.moveHistory.size <= 4 -> "Rebuilding strategy... \uD83E\uDDE0"
                    wPoints - bPoints >= 8 -> "Dominating the board! ♟️🔥"
                    bPoints - wPoints >= 5 && wPoints > 0 -> "Comeback incoming? 😏"
                    else -> ""
                }
            }
            
            AnimatedVisibility(
                visible = liveMessage.isNotEmpty(),
                enter = fadeIn() + slideInVertically { 20 },
                exit = fadeOut() + slideOutVertically { -20 }
            ) {
                Text(
                    text = liveMessage,
                    color = Color(0xFFFBBF24),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Chess Board
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.4f))
                    .border(4.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                ChessBoardView(
                    board = game.board,
                    selectedSquare = state.selectedSquare,
                    validMoves = state.validMoves,
                    theme = stats.theme,
                    pieceStyle = stats.pieceStyle,
                    pieceColorStyle = stats.pieceColorStyle,
                    onSquareClick = { r, c -> viewModel.onSquareSelected(r, c) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Player Dashboard
            PlayerDashboard(
                name = stats.username, // using the profile
                subtext = "Your Turn",
                avatar = stats.avatar,
                colorColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                timeMs = state.whiteTimeMs,
                captured = game.getCapturedPieces(PieceColor.WHITE),
                isTurn = game.turn == PieceColor.WHITE && !state.isGameOver,
                isPlayer = true,
                textColor = Color.White,
                pieceStyle = stats.pieceStyle
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            // AI Commentary Indicator
            if (stats.commentaryEnabled) {
                state.liveCommentary?.let { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Commentary: $comment",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Footer actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        icon = "↩️",
                        label = "UNDO",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.undoMove() }
                    )
                    ActionButton(
                        icon = "🔄",
                        label = "RESET",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startNewGame(state.mode, state.aiDepth) }
                    )
                    ActionButton(
                        icon = "🧠",
                        label = "ANALYZE",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.requestAIAnalysis() }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        icon = "🤖",
                        label = "AUTO MOVE (${state.autoMovesLeft})",
                        modifier = Modifier.weight(1f),
                        enabled = state.autoMovesLeft > 0 && !state.isGameOver,
                        onClick = { viewModel.autoMove() }
                    )
                    ActionButton(
                        icon = "⚔️",
                        label = "NEW GAME",
                        modifier = Modifier.weight(1f),
                        isPrimary = true,
                        onClick = { viewModel.startNewGame(state.mode, state.aiDepth) }
                    )
                }
            }
        }

        val aiAnalysis = viewModel.aiAnalysisState.collectAsState().value
        if (aiAnalysis != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { viewModel.dismissAIAnalysis() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧠", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Analysis",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val scrollState = rememberScrollState()
                        Text(
                            text = aiAnalysis,
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .verticalScroll(scrollState)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.dismissAIAnalysis() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Dismiss", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
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
                            text = "⏸️",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Game Paused",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Timers are paused. Click the button below or the top header button to continue playing.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.resumeGameExecution() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("▶️ Resume Game", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = { 
                                    viewModel.startNewGame(state.mode, state.aiDepth)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("🔄 Start New Game", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = { 
                                    viewModel.resumeGameExecution()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("🚪 Exit to Main Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        if (state.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
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
                        Text(
                            text = "Game Over",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.isDraw) "It's a draw!"
                            else if (state.winner == PieceColor.WHITE) "White wins!"
                            else "Black wins!",
                            color = Color(0xFF34D399),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) {
                                Text("Home")
                            }
                            Button(onClick = { viewModel.startNewGame(state.mode, state.aiDepth) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                                Text("Play Again", color = Color(0xFF0B0E14))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerDashboard(
    name: String,
    subtext: String,
    avatar: String,
    colorColors: List<Color>,
    timeMs: Long,
    captured: List<PieceType>,
    isTurn: Boolean,
    isPlayer: Boolean = false,
    textColor: Color,
    pieceStyle: String = "Classic Outline"
) {
    val mins = timeMs / 60000
    val secs = (timeMs % 60000) / 1000
    val timeStr = String.format("%02d:%02d", mins, secs)

    val modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Color.White.copy(alpha = if (isPlayer) 0.1f else 0.05f))
        .border(
            width = if (isTurn && isPlayer) 2.dp else 1.dp,
            color = if (isTurn && isPlayer) Color(0xFF10B981).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(24.dp)
        )

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(colorColors))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = avatar, fontSize = 20.sp, color = Color.White)
            }
            Column {
                Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = subtext, color = if (isPlayer) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeStr,
                color = textColor,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isPlayer) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-1).sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                captured.forEach { pt ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isPlayer) 0.2f else 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getPieceSymbolByStyle(Piece(pt, PieceColor.WHITE), pieceStyle),
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.8f) // tint text slightly
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChessBoardView(
    board: Array<Array<Piece?>>,
    selectedSquare: Position?,
    validMoves: List<Move>,
    theme: String,
    pieceStyle: String,
    pieceColorStyle: String,
    onSquareClick: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
        for (r in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (c in 0..7) {
                    val isLight = (r + c) % 2 == 0
                    
                    val bgColor = when (theme) {
                        "Classic Wood" -> if (isLight) Color(0xFFF0D9B5) else Color(0xFFB58863)
                        "Deep Oak" -> if (isLight) Color(0xFFDEB887) else Color(0xFF4A2C11)
                        "Classic Marble" -> if (isLight) Color(0xFFF3F4F6) else Color(0xFF9CA3AF)
                        "Neon Cyber" -> if (isLight) Color(0xFF1E1E3F) else Color(0xFF8B008B)
                        "Midnight Neon" -> if (isLight) Color(0xFF1E1B4B) else Color(0xFF311042)
                        else -> if (isLight) Color(0xFFE2E8F0) else Color(0xFF475569) // Frosted Glass (Default)
                    }

                    val isSelected = selectedSquare?.row == r && selectedSquare?.col == c
                    val isValidMove = validMoves.any { it.to.row == r && it.to.col == c }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(bgColor)
                            .clickable { onSquareClick(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            val selectColor = if (theme == "Neon Cyber" || theme == "Midnight Neon") Color(0xFF00FFFF) else Color(0xFF10B981)
                            Box(modifier = Modifier.fillMaxSize().background(selectColor.copy(alpha = 0.3f)).border(2.dp, selectColor.copy(alpha = 0.5f)))
                        }

                        val piece = board[r][c]
                        if (piece != null) {
                            PieceView(piece, theme, pieceStyle, pieceColorStyle)
                        }

                        if (isValidMove) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece, theme: String, pieceStyle: String, pieceColorStyle: String) {
    Box(contentAlignment = Alignment.Center) {
        val symbol = getPieceSymbolByStyle(piece, pieceStyle)
        val pc = getPieceColor(piece, pieceColorStyle, theme)

        Text(
            text = symbol,
            fontSize = if (pieceStyle == "Championship Minimal") 25.sp else 34.sp,
            color = pc,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                fontWeight = if (pieceStyle == "Championship Minimal") FontWeight.Bold else FontWeight.Normal,
                shadow = if (piece.color == PieceColor.WHITE || theme == "Neon Cyber" || theme == "Midnight Neon" || pieceColorStyle == "Matrix Neon") androidx.compose.ui.graphics.Shadow(
                    color = if (theme == "Neon Cyber" || theme == "Midnight Neon" || pieceColorStyle == "Matrix Neon") pc.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                    blurRadius = 8f
                ) else null
            )
        )
    }
}

fun getPieceSymbolByStyle(piece: Piece, pieceStyle: String): String {
    return when (pieceStyle) {
        "Classic Solid" -> {
            when (piece.type) {
                PieceType.PAWN -> "♟"
                PieceType.KNIGHT -> "♞"
                PieceType.BISHOP -> "♝"
                PieceType.ROOK -> "♜"
                PieceType.QUEEN -> "♛"
                PieceType.KING -> "♚"
            }
        }
        "Elite Symbolist" -> {
            when (piece.type) {
                PieceType.PAWN -> "♙"
                PieceType.KNIGHT -> "♘"
                PieceType.BISHOP -> "♗"
                PieceType.ROOK -> "♖"
                PieceType.QUEEN -> "♕"
                PieceType.KING -> "♔"
            }
        }
        "Championship Minimal" -> {
            when (piece.type) {
                PieceType.PAWN -> "P"
                PieceType.KNIGHT -> "N"
                PieceType.BISHOP -> "B"
                PieceType.ROOK -> "R"
                PieceType.QUEEN -> "Q"
                PieceType.KING -> "K"
            }
        }
        else -> { // "Classic Outline"
            if (piece.color == PieceColor.WHITE) {
                when (piece.type) {
                    PieceType.PAWN -> "♙"
                    PieceType.KNIGHT -> "♘"
                    PieceType.BISHOP -> "♗"
                    PieceType.ROOK -> "♖"
                    PieceType.QUEEN -> "♕"
                    PieceType.KING -> "♔"
                }
            } else {
                when (piece.type) {
                    PieceType.PAWN -> "♟"
                    PieceType.KNIGHT -> "♞"
                    PieceType.BISHOP -> "♝"
                    PieceType.ROOK -> "♜"
                    PieceType.QUEEN -> "♛"
                    PieceType.KING -> "♚"
                }
            }
        }
    }
}

fun getPieceColor(piece: Piece, pieceColorStyle: String, theme: String): Color {
    return when (pieceColorStyle) {
        "Ivory & Walnut" -> {
            if (piece.color == com.example.chess.model.PieceColor.WHITE) {
                Color(0xFFFFFBEB)
            } else {
                Color(0xFF4A3728)
            }
        }
        "Crimson Gold" -> {
            if (piece.color == com.example.chess.model.PieceColor.WHITE) {
                Color(0xFFFBBF24)
            } else {
                Color(0xFFEF4444)
            }
        }
        "Matrix Neon" -> {
            if (piece.color == com.example.chess.model.PieceColor.WHITE) {
                Color(0xFF00FFFF)
            } else {
                Color(0xFFFF00FF)
            }
        }
        else -> { // "Standard Crisp"
            if (piece.color == com.example.chess.model.PieceColor.WHITE) {
                Color.White
            } else {
                if (theme == "Neon Cyber" || theme == "Midnight Neon") {
                    Color(0xFFFF00FF)
                } else {
                    Color(0xFF1E293B)
                }
            }
        }
    }
}

fun getOutlinedPieceSymbol(piece: Piece): String {
    // In design, html uses ♙ ♘ ♗ ♖ ♕ ♔ and ♟ ♞ ♝ ♜ ♛ ♚
    return if(piece.color == PieceColor.WHITE) {
        when (piece.type) {
            PieceType.PAWN -> "♙"
            PieceType.KNIGHT -> "♘"
            PieceType.BISHOP -> "♗"
            PieceType.ROOK -> "♖"
            PieceType.QUEEN -> "♕"
            PieceType.KING -> "♔"
        }
    } else {
        when (piece.type) {
            PieceType.PAWN -> "♟"
            PieceType.KNIGHT -> "♞"
            PieceType.BISHOP -> "♝"
            PieceType.ROOK -> "♜"
            PieceType.QUEEN -> "♛"
            PieceType.KING -> "♚"
        }
    }
}

fun getPieceSymbol(piece: Piece): String {
    return when (piece.type) {
        PieceType.PAWN -> "♙"
        PieceType.KNIGHT -> "♘"
        PieceType.BISHOP -> "♗"
        PieceType.ROOK -> "♖"
        PieceType.QUEEN -> "♕"
        PieceType.KING -> "♔"
    }
}


@Composable
fun ActionButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPrimary) Color(0xFF10B981).copy(alpha = if (enabled) 1f else 0.5f) else Color.White.copy(alpha = if (enabled) 0.05f else 0.02f))
            .border(1.dp, if (isPrimary) Color.Transparent else Color.White.copy(alpha = if (enabled) 0.05f else 0.02f), RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = icon, fontSize = 18.sp, modifier = Modifier.alpha(if (enabled) 1f else 0.5f))
            Text(
                text = label,
                color = if (isPrimary) Color(0xFF0B0E14).copy(alpha = if (enabled) 1f else 0.5f) else Color.White.copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
