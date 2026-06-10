package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessGame
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.Position
import kotlinx.coroutines.delay
import com.example.chess.viewmodel.ChessViewModel

data class Opening(
    val name: String,
    val description: String,
    val moves: List<String> // Array of simple move coords "r,c-r.c" e.g. "6,4-4,4"
)

val openings = listOf(
    Opening(
        "Italian Game",
        "Classic opening that focuses on rapid development and center control.",
        listOf("6,4-4,4", "1,4-3,4", "7,6-5,5", "0,1-2,2", "7,5-4,2")
    ),
    Opening(
        "Sicilian Defense",
        "Popular, aggressive defense for Black against 1.e4.",
        listOf("6,4-4,4", "1,2-3,2")
    ),
    Opening(
        "Queen's Gambit",
        "White offers a pawn to gain dominance in the center.",
        listOf("6,3-4,3", "1,3-3,3", "6,2-4,2")
    ),
    Opening(
        "Ruy Lopez",
        "A highly strategical opening bringing the bishop out to attack the knight.",
        listOf("6,4-4,4", "1,4-3,4", "7,6-5,5", "0,1-2,2", "7,5-3,1")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningTrainerScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val theme = stats.theme

    var selectedOpening by remember { mutableStateOf<Opening?>(null) }
    var game by remember { mutableStateOf(ChessGame()) }
    var currentMoveIndex by remember { mutableStateOf(0) }
    var userMessage by remember { mutableStateOf("Select an opening to learn.") }
    
    var selectedSquare by remember { mutableStateOf<Position?>(null) }
    var validMoves by remember { mutableStateOf<List<Move>>(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        // Blobs
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = 100.dp, y = (-100).dp)
                    .size(400.dp)
                    .background(Color(0xFF312E81).copy(alpha = 0.3f), CircleShape)
                    .blur(100.dp)
            )
        }

        if (selectedOpening == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, bottom = 24.dp)
            ) {
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
                        text = "Opening Trainer",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                openings.forEach { op ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clickable { 
                                selectedOpening = op
                                game = ChessGame()
                                currentMoveIndex = 0
                                userMessage = "Make the first move: ${op.moves[0]}"
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(op.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(op.description, color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            // Training View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            .clickable { selectedOpening = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = selectedOpening!!.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userMessage, color = Color.White, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                        selectedSquare = selectedSquare,
                        validMoves = validMoves,
                        theme = theme,
                        pieceStyle = stats.pieceStyle,
                        pieceColorStyle = stats.pieceColorStyle,
                        onSquareClick = { r, c ->
                            if (currentMoveIndex >= selectedOpening!!.moves.size) return@ChessBoardView
                            
                            val expectedMoveStr = selectedOpening!!.moves[currentMoveIndex]
                            val parts = expectedMoveStr.split("-")
                            val fromCoords = parts[0].split(",")
                            val toCoords = parts[1].split(",")
                            val targetFromRow = fromCoords[0].toInt()
                            val targetFromCol = fromCoords[1].toInt()
                            val targetToRow = toCoords[0].toInt()
                            val targetToCol = toCoords[1].toInt()

                            val pos = Position(r, c)
                            if (selectedSquare == null) {
                                // Must pick the correct from piece
                                if (r == targetFromRow && c == targetFromCol) {
                                    val valid = game.getValidMovesFor(pos)
                                    selectedSquare = pos
                                    validMoves = valid
                                }
                            } else {
                                val move = validMoves.find { it.to == pos }
                                if (move != null && move.to.row == targetToRow && move.to.col == targetToCol) {
                                    // Correct move!
                                    val nGame = game.deepCopy()
                                    nGame.makeMove(move)
                                    game = nGame
                                    selectedSquare = null
                                    validMoves = emptyList()
                                    
                                    currentMoveIndex++
                                    if (currentMoveIndex >= selectedOpening!!.moves.size) {
                                        userMessage = "Training complete! Excellent!"
                                    } else {
                                        userMessage = "Correct! Now play: ${selectedOpening!!.moves[currentMoveIndex]}"
                                    }
                                } else {
                                    // Wrong move, clear
                                    selectedSquare = null
                                    validMoves = emptyList()
                                    userMessage = "Wrong move. Try again. Expected: piece from $targetFromRow,$targetFromCol to $targetToRow,$targetToCol"
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
