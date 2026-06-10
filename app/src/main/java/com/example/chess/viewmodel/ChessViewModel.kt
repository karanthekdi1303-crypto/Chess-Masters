package com.example.chess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.data.GameStats
import com.example.chess.data.GameStatsRepository
import com.example.chess.model.ChessAI
import com.example.chess.model.ChessGame
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameMode { PVP, AI }

data class ChessUiState(
    val game: ChessGame? = null,
    val mode: GameMode = GameMode.AI,
    val aiDepth: Int = 2,
    val selectedSquare: Position? = null,
    val validMoves: List<Move> = emptyList(),
    val whiteTimeMs: Long = 10 * 60 * 1000L,
    val blackTimeMs: Long = 10 * 60 * 1000L,
    val isGameOver: Boolean = false,
    val winner: PieceColor? = null,
    val isDraw: Boolean = false,
    val soundTrigger: String? = null, // "move", "capture", "check", "mate"
    val liveCommentary: String? = "Welcome to Master Chess Pro! Make your opening move.",
    val autoMovesLeft: Int = 3,
    val isPaused: Boolean = false
)

class ChessViewModel(private val repository: GameStatsRepository, private val context: android.content.Context) : ViewModel() {

    private val jsEngine = com.example.chess.model.JSChessEngineBridge(context)

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    val stats = repository.stats.stateIn(viewModelScope, SharingStarted.Lazily, GameStats())

    private val _aiAnalysisState = MutableStateFlow<String?>(null)
    val aiAnalysisState: StateFlow<String?> = _aiAnalysisState.asStateFlow()
    
    private val _postGameAnalysis = MutableStateFlow<String?>(null)
    val postGameAnalysis: StateFlow<String?> = _postGameAnalysis.asStateFlow()

    fun requestPostGameAnalysis() {
        val state = _uiState.value
        val game = state.game ?: return
        
        _postGameAnalysis.value = "Analyzing full game..."

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                
                val historyStr = game.moveHistory.mapIndexed { idx, m -> 
                    val color = if (idx % 2 == 0) "White" else "Black"
                    val p = game.board[m.to.row][m.to.col]
                    val pName = p?.type?.name ?: "Unknown"
                    "$color moves $pName from (${m.from.row},${m.from.col}) to (${m.to.row},${m.to.col})"
                }.joinToString("\n")

                val prompt = "You are a chess grandmaster. Analyze the following game history. Point out any major mistakes, blunders, or brilliant moves if you can discern them from the sequence. The moves are in sequence:\n$historyStr\n\nProvide a post-match evaluation and advice for improvement. Include simple, beginner-friendly advice if obvious mistakes are present."

                val request = com.example.chess.data.GenerateContentRequest(
                    contents = listOf(
                        com.example.chess.data.Content(
                            parts = listOf(com.example.chess.data.Part(text = prompt))
                        )
                    )
                )

                val response = com.example.chess.data.RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _postGameAnalysis.value = text ?: "Analysis failed."
            } catch (e: Exception) {
                _postGameAnalysis.value = "Error: ${e.message}"
            }
        }
    }

    fun dismissPostGameAnalysis() {
        _postGameAnalysis.value = null
    }

    fun requestAIAnalysis() {
        val state = _uiState.value
        val game = state.game ?: return
        
        _aiAnalysisState.value = "Analyzing..."

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                
                var boardVisual = ""
                for (r in 0..7) {
                    for (c in 0..7) {
                        val p = game.board[r][c]
                        boardVisual += if (p != null) {
                            val symbol = if (p.color == PieceColor.WHITE) {
                                when (p.type) {
                                    com.example.chess.model.PieceType.PAWN -> "P"
                                    com.example.chess.model.PieceType.KNIGHT -> "N"
                                    com.example.chess.model.PieceType.BISHOP -> "B"
                                    com.example.chess.model.PieceType.ROOK -> "R"
                                    com.example.chess.model.PieceType.QUEEN -> "Q"
                                    com.example.chess.model.PieceType.KING -> "K"
                                }
                            } else {
                                when (p.type) {
                                    com.example.chess.model.PieceType.PAWN -> "p"
                                    com.example.chess.model.PieceType.KNIGHT -> "n"
                                    com.example.chess.model.PieceType.BISHOP -> "b"
                                    com.example.chess.model.PieceType.ROOK -> "r"
                                    com.example.chess.model.PieceType.QUEEN -> "q"
                                    com.example.chess.model.PieceType.KING -> "k"
                                }
                            }
                            symbol
                        } else {
                            "."
                        }
                    }
                    boardVisual += "\n"
                }

                val prompt = "You are a chess grandmaster. Analyze the following board state where 'P,N,B,R,Q,K' are white pieces and 'p,n,b,r,q,k' are black pieces. The board is standard 8x8 (row 0 to 7). It is ${game.turn}'s turn to move.\nBoard:\n$boardVisual\n\nProvide a very concise suggested move and brief explanation. Keep it under 2 sentences to ensure a fast response."

                val request = com.example.chess.data.GenerateContentRequest(
                    contents = listOf(
                        com.example.chess.data.Content(
                            parts = listOf(com.example.chess.data.Part(text = prompt))
                        )
                    )
                )

                kotlinx.coroutines.withTimeout(5000L) {
                    val response = com.example.chess.data.RetrofitClient.service.generateContentStream(apiKey, request)
                    response.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        var fullText = ""
                        val moshi = com.squareup.moshi.Moshi.Builder().build()
                        val adapter = moshi.adapter(com.example.chess.data.GenerateContentResponse::class.java)

                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line!!
                            if (currentLine.startsWith("data: ")) {
                                val jsonString = currentLine.substring(6)
                                try {
                                    val chunkResponse = adapter.fromJson(jsonString)
                                    val text = chunkResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                    if (text != null) {
                                        fullText += text
                                        _aiAnalysisState.value = fullText.ifEmpty { "Analyzing..." }
                                    }
                                } catch (e: Exception) {
                                    // Ignore json parse errors for incomplete chunks
                                }
                            }
                        }
                    }
                }
                if (_aiAnalysisState.value == "Analyzing...") {
                     _aiAnalysisState.value = "Analysis completed."
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                if (_aiAnalysisState.value == "Analyzing..." || _aiAnalysisState.value.isNullOrBlank()) {
                    _aiAnalysisState.value = "Analysis timed out after 5 seconds."
                }
            } catch (e: Exception) {
                _aiAnalysisState.value = "Error: ${e.message}"
            }
        }
    }

    fun dismissAIAnalysis() {
        _aiAnalysisState.value = null
    }

    fun updateProfile(
        username: String,
        avatar: String,
        theme: String,
        language: String,
        commentaryEnabled: Boolean,
        pieceStyle: String,
        pieceColorStyle: String
    ) {
        viewModelScope.launch {
            val updated = stats.value.copy(
                username = username,
                avatar = avatar,
                theme = theme,
                language = language,
                commentaryEnabled = commentaryEnabled,
                pieceStyle = pieceStyle,
                pieceColorStyle = pieceColorStyle
            )
            repository.updateStats(updated)
            syncUpdateCloudIfLinked(updated)
        }
    }

    private var aiJob: Job? = null
    private var timerJob: Job? = null
    private val chessAI = ChessAI()

    fun pauseGame() {
        if (_uiState.value.isPaused || _uiState.value.isGameOver) return
        aiJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeGameExecution() {
        if (!_uiState.value.isPaused) return
        _uiState.update { it.copy(isPaused = false) }
        val state = _uiState.value
        val game = state.game ?: return
        if (!state.isGameOver && state.mode == GameMode.AI && game.turn == PieceColor.BLACK) {
            triggerAI(game)
        }
    }

    fun startNewGame(mode: GameMode, aidepth: Int) {
        aiJob?.cancel()
        timerJob?.cancel()
        val newGame = ChessGame()
        _uiState.value = ChessUiState(
            game = newGame,
            mode = mode,
            aiDepth = aidepth,
            whiteTimeMs = 10 * 60 * 1000L,
            blackTimeMs = 10 * 60 * 1000L,
            isGameOver = false,
            isPaused = false
        )
        viewModelScope.launch {
            val initialState = jsEngine.reset()
            newGame.syncWithJS(initialState)
            _uiState.update { it.copy(game = newGame) }
            startTimers()
        }
    }

    private fun startTimers() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val state = _uiState.value
                val game = state.game ?: continue
                if (state.isGameOver) break
                if (state.isPaused) continue
                
                if (game.turn == PieceColor.WHITE) {
                    val newTime = (state.whiteTimeMs - 100).coerceAtLeast(0)
                    _uiState.update { it.copy(whiteTimeMs = newTime) }
                    if (newTime == 0L) handleTimeOut(PieceColor.WHITE)
                } else {
                    val newTime = (state.blackTimeMs - 100).coerceAtLeast(0)
                    _uiState.update { it.copy(blackTimeMs = newTime) }
                    if (newTime == 0L) handleTimeOut(PieceColor.BLACK)
                }
            }
        }
    }

    private fun handleTimeOut(colorWhoLost: PieceColor) {
        endGame(winner = if (colorWhoLost == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
    }

    fun onSquareSelected(row: Int, col: Int) {
        val state = _uiState.value
        if (state.isGameOver || state.isPaused) return
        val game = state.game ?: return
        
        // If it's AI mode and it's AI's turn, ignore user input
        if (state.mode == GameMode.AI && game.turn == PieceColor.BLACK) return

        val pos = Position(row, col)

        viewModelScope.launch {
            if (state.selectedSquare == null) {
                // Get valid moves instantly in pure Kotlin without evaluating Javascript
                val valid = game.getValidMovesFor(pos)
                if (valid.isNotEmpty()) {
                    _uiState.update { it.copy(selectedSquare = pos, validMoves = valid) }
                }
            } else {
                val move = state.validMoves.find { it.to == pos }
                if (move != null) {
                    processExecutedMove(game, move)
                } else {
                    // Clicked on another of my pieces, select it instead instantly in Kotlin
                    val valid = game.getValidMovesFor(pos)
                    if (valid.isNotEmpty()) {
                        _uiState.update { it.copy(selectedSquare = pos, validMoves = valid) }
                    } else {
                        _uiState.update { it.copy(selectedSquare = null, validMoves = emptyList()) }
                    }
                }
            }
        }
    }

    private fun getCommentary(isGood: Boolean, language: String): String {
        val goodEng = listOf("Brilliant move!", "Masterstroke!", "Perfect control of the board!", "You’re playing like a champion!")
        val badEng = listOf("That’s a blunder!", "You just gave it away…", "Not a good move at all!", "This weakens your position badly!")
        
        val goodHin = listOf("Kamaal ka move!", "Shandar chal!", "Masterstroke laga diya!", "Bilkul pro level play!")
        val badHin = listOf("Arey yeh kya kar diya!", "Galat move ho gaya!", "Bohot badi galti!", "Position kharab kar di!")
        
        val goodGuj = listOf("Gajab Dikraaa!", "Baapuji shabashi aapo aane!", "Dil thepla thepla kari didhu!", "Marro dikro ekdam set!")
        val badGuj = listOf("Baka jato rehje tu!", "Chakli move of the year!", "Arey su kari didhu aa!", "Aa to bilkul ulto padyo bhai!")
        
        val goodMar = listOf("Kya mast chal aahe!", "Zabardast move re!", "Ek number strategy!", "Tujha game bhari strong aahe!")
        val badMar = listOf("Arey kay kelas he!", "Chuk zali bhari!", "Game ghabarun gela!", "He tar poorna ulta padla re!")
        
        val list = when (language) {
            "Hindi" -> if (isGood) goodHin else badHin
            "Gujarati" -> if (isGood) goodGuj else badGuj
            "Marathi" -> if (isGood) goodMar else badMar
            else -> if (isGood) goodEng else badEng
        }
        return list.random()
    }

    private suspend fun processExecutedMove(game: ChessGame, move: Move) {
        val fromAlg = com.example.chess.model.JSMove.toAlgebraic(move.from.row, move.from.col)
        val toAlg = com.example.chess.model.JSMove.toAlgebraic(move.to.row, move.to.col)
        
        val promotionStr = when (move.promotion) {
            com.example.chess.model.PieceType.QUEEN -> "q"
            com.example.chess.model.PieceType.ROOK -> "r"
            com.example.chess.model.PieceType.BISHOP -> "b"
            com.example.chess.model.PieceType.KNIGHT -> "n"
            else -> if (game.board[move.from.row][move.from.col]?.type == com.example.chess.model.PieceType.PAWN && (move.to.row == 0 || move.to.row == 7)) "q" else null
        }

        // 1. Optimistic UI update: Make the move instantly in Kotlin
        val originalGameSnapshot = game.deepCopy()
        
        game.applyMove(move)
        val tentativeSound = if (move.isCapture) "capture" else "move"
        
        // Advance turn instantly
        game.turn = if (game.turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        game.moveHistory.add(move)

        val optimisticGame = game.deepCopy()
        _uiState.update { it.copy(
            game = optimisticGame,
            selectedSquare = null,
            validMoves = emptyList(),
            soundTrigger = tentativeSound
        ) }

        // 2. background synchronisation with the WebView engine rules truth
        val result = jsEngine.makeMove(fromAlg, toAlg, promotionStr)
        if (!result.success || result.state == null) {
            // Rollback optimistic move if chess rules sync failed for some reason
            _uiState.update { it.copy(game = originalGameSnapshot) }
            return
        }

        game.syncWithJS(result.state)

        val finalSound = when {
            result.state.isGameOver && result.state.isCheckmate -> "mate"
            result.state.isCheck -> "check"
            else -> tentativeSound
        }

        val lang = stats.value.language
        var comment = "..."
        var isGood = false
        var isBad = false

        if (result.state.isGameOver || result.state.isCheck || move.isCapture || move.isCastling || move.promotion != null) {
            isGood = true
        } else {
            val defendingColor = if (result.state.turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
            val isUnderAttack = game.isSquareAttacked(move.to.row, move.to.col, defendingColor)
            if (isUnderAttack) {
                isBad = true
            }
        }

        if (isGood) {
            comment = getCommentary(true, lang)
        } else if (isBad) {
            comment = getCommentary(false, lang)
        } else {
            val neutralEng = listOf("Solid maneuver.", "Developing pieces.", "Interesting choice.", "The plot thickens...", "A quiet positional move.")
            val neutralHin = listOf("Achi chaal.", "Soch samajh kar khela.", "Khel aage badhta hua.", "Sahi disha mein kadam.", "Dheere dhamm aage badhte hue.")
            val neutralGuj = listOf("Sari chaal.", "Vicharine rami rahya chho.", "Pachhal thi majbuti.", "Dheeme dheeme aagal vadhiye.", "Game jamse have.")
            val neutralMar = listOf("Vyavasthit chal.", "Changli strategy disat aahe.", "Santh aani samjun.", "Khel pudhe jato ahe.", "Ek santulit move.")
            val list = when (lang) {
                "Hindi" -> neutralHin
                "Gujarati" -> neutralGuj
                "Marathi" -> neutralMar
                else -> neutralEng
            }
            comment = list.random()
        }

        val syncedGame = game.deepCopy()

        _uiState.update { it.copy(
            game = syncedGame,
            soundTrigger = finalSound,
            liveCommentary = comment
        ) }

        checkGameOver(syncedGame)

        if (!_uiState.value.isGameOver && _uiState.value.mode == GameMode.AI && result.state.turn == PieceColor.BLACK) {
            triggerAI(syncedGame)
        }
    }

    private fun checkGameOver(game: ChessGame) {
        if (game.isMate) {
            endGame(winner = if (game.turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
        } else if (game.isStalemate) {
            endGame(winner = null, isDraw = true)
        }
    }

    private fun endGame(winner: PieceColor?, isDraw: Boolean = false) {
        timerJob?.cancel()
        _uiState.update { it.copy(isGameOver = true, winner = winner, isDraw = isDraw) }
        
        viewModelScope.launch {
            val currentStats = repository.stats.first()
            val played = currentStats.gamesPlayed + 1
            
            var newStreak = currentStats.winStreak
            var resultStr = ""
            
            if (isDraw) {
                newStreak = 0
                resultStr = "D"
            } else if (winner == PieceColor.WHITE) {
                newStreak += 1
                resultStr = "W"
            } else {
                newStreak = 0
                resultStr = "L"
            }
            
            val newHistoryList = currentStats.historyList.toMutableList()
            newHistoryList.add(resultStr)
            if (newHistoryList.size > 10) newHistoryList.removeAt(0)
            val newHistoryStr = newHistoryList.joinToString(",")
            
            val finishedStats = if (isDraw) {
                currentStats.copy(gamesPlayed = played, draws = currentStats.draws + 1, winStreak = newStreak, gameHistory = newHistoryStr)
            } else if (winner == PieceColor.WHITE) {
                currentStats.copy(gamesPlayed = played, wins = currentStats.wins + 1, winStreak = newStreak, gameHistory = newHistoryStr)
            } else {
                currentStats.copy(gamesPlayed = played, losses = currentStats.losses + 1, winStreak = newStreak, gameHistory = newHistoryStr)
            }
            repository.updateStats(finishedStats)
            syncUpdateCloudIfLinked(finishedStats)
        }
    }

    fun autoMove() {
        if (_uiState.value.isGameOver) return
        val game = _uiState.value.game ?: return
        if (_uiState.value.autoMovesLeft <= 0) return
        
        _uiState.update { it.copy(autoMovesLeft = it.autoMovesLeft - 1) }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val depth = _uiState.value.aiDepth
            val turn = game.turn
            val aiMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                chessAI.calculateBestMove(game, depth, turn)
            }
            if (aiMove != null) {
                processExecutedMove(game, aiMove)
            }
        }
    }

    private fun triggerAI(game: ChessGame) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(500) // 0.5s delay for natural response time
            val depth = _uiState.value.aiDepth
            val aiMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                chessAI.calculateBestMove(game, depth, PieceColor.BLACK)
            }
            if (aiMove != null) {
                processExecutedMove(game, aiMove)
            } else {
                checkGameOver(game)
            }
        }
    }

    fun undoMove() {
        aiJob?.cancel()
        val state = _uiState.value
        val game = state.game ?: return
        viewModelScope.launch {
            if (state.mode == GameMode.AI) {
                // Undo 2 steps
                jsEngine.undo()
                val finalState = jsEngine.undo()
                val newGame = game.deepCopy()
                newGame.syncWithJS(finalState)
                if (newGame.moveHistory.isNotEmpty()) newGame.moveHistory.removeAt(newGame.moveHistory.lastIndex)
                if (newGame.moveHistory.isNotEmpty()) newGame.moveHistory.removeAt(newGame.moveHistory.lastIndex)
                _uiState.update { it.copy(game = newGame, selectedSquare = null, validMoves = emptyList()) }
            } else {
                val finalState = jsEngine.undo()
                val newGame = game.deepCopy()
                newGame.syncWithJS(finalState)
                if (newGame.moveHistory.isNotEmpty()) newGame.moveHistory.removeAt(newGame.moveHistory.lastIndex)
                _uiState.update { it.copy(game = newGame, selectedSquare = null, validMoves = emptyList()) }
            }
        }
    }

    fun clearSoundTrigger() {
        _uiState.update { it.copy(soundTrigger = null) }
    }

    // --- Firebase Cloud Sync & Authentication Services ---

    fun syncUpdateCloudIfLinked(updatedStats: GameStats) {
        if (updatedStats.cloudSynced && updatedStats.cloudUserId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                        com.example.chess.data.FirebaseService.currentProjectId,
                        "players",
                        updatedStats.cloudUserId,
                        com.example.chess.data.FirebaseService.currentApiKey,
                        com.example.chess.data.FirebaseService.buildFirestoreDocument(updatedStats)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseSync", "Auto-synchronisation to cloud failed: ${e.message}")
                }
            }
        }
    }

    fun signUpWithEmailFirebase(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        dob: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = com.example.chess.data.FirebaseAuthRequest(email, password)
                val resp = com.example.chess.data.FirebaseService.authApi.signUp(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val authBody = resp.body()!!
                    val localId = authBody.localId ?: ""
                    
                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val nowStr = formatter.format(java.util.Date())
                    val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                    
                    val currentLocalStats = stats.value
                    val newCloudProfile = currentLocalStats.copy(
                        fullName = fullName,
                        email = email,
                        phoneNumber = phoneNumber,
                        dob = dob,
                        playerId = generatedPlayerId,
                        lastLogin = nowStr,
                        deviceInformation = device,
                        cloudSynced = true,
                        cloudUserId = localId,
                        creationDate = nowStr,
                        isDatabaseAdmin = email.trim().lowercase() == "karanthekdi1303@gmail.com"
                    )
                    
                    repository.updateStats(newCloudProfile)
                    
                    val firestoreDoc = com.example.chess.data.FirebaseService.buildFirestoreDocument(newCloudProfile)
                    val cloudResp = com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                        com.example.chess.data.FirebaseService.currentProjectId,
                        "players",
                        localId,
                        com.example.chess.data.FirebaseService.currentApiKey,
                        firestoreDoc
                    )
                    
                    if (cloudResp.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure("Identity created, but database sync returned status: ${cloudResp.code()}")
                    }
                } else {
                    val errorMsg = resp.errorBody()?.string() ?: "Signup failed"
                    onFailure(errorMsg)
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Network error")
            }
        }
    }

    fun signInWithEmailFirebase(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = com.example.chess.data.FirebaseAuthRequest(email, password)
                val resp = com.example.chess.data.FirebaseService.authApi.signIn(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val authBody = resp.body()!!
                    val localId = authBody.localId ?: ""
                    
                    val docResp = com.example.chess.data.FirebaseService.firestoreApi.getDocument(
                        com.example.chess.data.FirebaseService.currentProjectId,
                        "players",
                        localId,
                        com.example.chess.data.FirebaseService.currentApiKey
                    )
                    
                    if (docResp.isSuccessful && docResp.body() != null) {
                        val cloudProfile = com.example.chess.data.FirebaseService.parseFirestoreDocument(docResp.body()!!)
                        if (cloudProfile != null) {
                            if (cloudProfile.accountStatus == "Banned") {
                                onFailure("This account has been permanently banned.")
                                return@launch
                            }
                            if (cloudProfile.accountStatus == "Inactive") {
                                onFailure("This account is currently deactivated.")
                                return@launch
                            }
                            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val nowStr = formatter.format(java.util.Date())
                            val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                            
                            val updatedProfile = cloudProfile.copy(
                                lastLogin = nowStr,
                                deviceInformation = device,
                                cloudSynced = true,
                                cloudUserId = localId
                            )
                            
                            repository.updateStats(updatedProfile)
                            
                            viewModelScope.launch {
                                try {
                                    com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                                        com.example.chess.data.FirebaseService.currentProjectId,
                                        "players",
                                        localId,
                                        com.example.chess.data.FirebaseService.currentApiKey,
                                        com.example.chess.data.FirebaseService.buildFirestoreDocument(updatedProfile)
                                    )
                                } catch (e: Exception) {
                                    // ignore bg sync failures
                                }
                            }
                            onSuccess()
                        } else {
                            onFailure("Failed to decode cloud profile document structure.")
                        }
                    } else if (docResp.code() == 404) {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val nowStr = formatter.format(java.util.Date())
                        val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        
                        val newProfile = stats.value.copy(
                            fullName = "Pro Chess Player",
                            email = email,
                            playerId = generatedPlayerId,
                            lastLogin = nowStr,
                            deviceInformation = device,
                            cloudSynced = true,
                            cloudUserId = localId,
                            creationDate = nowStr,
                            isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekadi90@gmail.com")
                        )
                        repository.updateStats(newProfile)
                        
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "players",
                            localId,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            com.example.chess.data.FirebaseService.buildFirestoreDocument(newProfile)
                        )
                        onSuccess()
                    } else {
                        onFailure("Authentication success, but failed to retrieve cloud profile (HTTP: ${docResp.code()})")
                    }
                } else {
                    val errorMsg = resp.errorBody()?.string() ?: "Login credentials failed"
                    onFailure(errorMsg)
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Authentication network is unavailable.")
            }
        }
    }

    fun authenticateWithEmailOnly(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val password = "MasterChessProPass2026!"
                val req = com.example.chess.data.FirebaseAuthRequest(email.trim(), password)
                val resp = com.example.chess.data.FirebaseService.authApi.signIn(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val authBody = resp.body()!!
                    val localId = authBody.localId ?: ""
                    
                    val docResp = com.example.chess.data.FirebaseService.firestoreApi.getDocument(
                        com.example.chess.data.FirebaseService.currentProjectId,
                        "players",
                        localId,
                        com.example.chess.data.FirebaseService.currentApiKey
                    )
                    
                    if (docResp.isSuccessful && docResp.body() != null) {
                        val cloudProfile = com.example.chess.data.FirebaseService.parseFirestoreDocument(docResp.body()!!)
                        if (cloudProfile != null) {
                            if (cloudProfile.accountStatus == "Banned") {
                                onFailure("This account has been permanently banned.")
                                return@launch
                            }
                            if (cloudProfile.accountStatus == "Inactive") {
                                onFailure("This account is currently deactivated.")
                                return@launch
                            }
                            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val nowStr = formatter.format(java.util.Date())
                            val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                            
                            val updatedProfile = cloudProfile.copy(
                                lastLogin = nowStr,
                                deviceInformation = device,
                                cloudSynced = true,
                                cloudUserId = localId
                            )
                            repository.updateStats(updatedProfile)
                            onSuccess()
                        } else {
                            onFailure("Failed to decode cloud profile document structure.")
                        }
                    } else {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val nowStr = formatter.format(java.util.Date())
                        val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        
                        val newProfile = stats.value.copy(
                            fullName = "Pro Chess Player",
                            email = email.trim(),
                            playerId = generatedPlayerId,
                            lastLogin = nowStr,
                            deviceInformation = device,
                            cloudSynced = true,
                            cloudUserId = localId,
                            creationDate = nowStr,
                            isDatabaseAdmin = email.trim().lowercase() == "karanthekdi1303@gmail.com"
                        )
                        repository.updateStats(newProfile)
                        
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "players",
                            localId,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            com.example.chess.data.FirebaseService.buildFirestoreDocument(newProfile)
                        )
                        onSuccess()
                    }
                } else {
                    val signUpReq = com.example.chess.data.FirebaseAuthRequest(email.trim(), password)
                    val signUpResp = com.example.chess.data.FirebaseService.authApi.signUp(
                        com.example.chess.data.FirebaseService.currentApiKey,
                        signUpReq
                    )
                    if (signUpResp.isSuccessful && signUpResp.body() != null) {
                        val authBody = signUpResp.body()!!
                        val localId = authBody.localId ?: ""
                        
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val nowStr = formatter.format(java.util.Date())
                        val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        
                        val newCloudProfile = stats.value.copy(
                            fullName = "Pro Chess Player",
                            email = email.trim(),
                            playerId = generatedPlayerId,
                            lastLogin = nowStr,
                            deviceInformation = device,
                            cloudSynced = true,
                            cloudUserId = localId,
                            creationDate = nowStr,
                            isDatabaseAdmin = email.trim().lowercase() == "karanthekdi1303@gmail.com"
                        )
                        
                        repository.updateStats(newCloudProfile)
                        
                        val firestoreDoc = com.example.chess.data.FirebaseService.buildFirestoreDocument(newCloudProfile)
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "players",
                            localId,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            firestoreDoc
                        )
                        onSuccess()
                    } else {
                        val errorMsg = signUpResp.errorBody()?.string() ?: "Login and Registration process failed. Please check internet connection."
                        onFailure(errorMsg)
                    }
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Network error during seamless authentication.")
            }
        }
    }

    fun syncStatsToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val current = stats.value
        if (!current.cloudSynced || current.cloudUserId.isEmpty()) {
            onComplete(false, "Cloud Account is not currently linked.")
            return
        }
        viewModelScope.launch {
            try {
                val resp = com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                    com.example.chess.data.FirebaseService.currentProjectId,
                    "players",
                    current.cloudUserId,
                    com.example.chess.data.FirebaseService.currentApiKey,
                    com.example.chess.data.FirebaseService.buildFirestoreDocument(current)
                )
                if (resp.isSuccessful) {
                    onComplete(true, "Cloud profile updated successfully.")
                } else {
                    onComplete(false, "Database refused sync with error status: ${resp.code()}")
                }
            } catch (e: Exception) {
                onComplete(false, "Database connection unavailable: ${e.message}")
            }
        }
    }

    fun syncStatsFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val current = stats.value
        if (!current.cloudSynced || current.cloudUserId.isEmpty()) {
            onComplete(false, "Cloud Account is not currently linked.")
            return
        }
        viewModelScope.launch {
            try {
                val resp = com.example.chess.data.FirebaseService.firestoreApi.getDocument(
                    com.example.chess.data.FirebaseService.currentProjectId,
                    "players",
                    current.cloudUserId,
                    com.example.chess.data.FirebaseService.currentApiKey
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val cloudProfile = com.example.chess.data.FirebaseService.parseFirestoreDocument(resp.body()!!)
                    if (cloudProfile != null) {
                        repository.updateStats(cloudProfile)
                        onComplete(true, "Progress and profile stats restored successfully from the cloud!")
                    } else {
                        onComplete(false, "Could not decode cloud schema.")
                    }
                } else {
                    onComplete(false, "Could not retrieve cloud data (HTTP: ${resp.code()})")
                }
            } catch (e: Exception) {
                onComplete(false, "Network unavailable: ${e.message}")
            }
        }
    }

    fun logoutCloudAcc(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val current = stats.value
            val unlinked = current.copy(
                fullName = "",
                email = "",
                phoneNumber = "",
                dob = "",
                playerId = "",
                cloudSynced = false,
                cloudUserId = "",
                creationDate = "",
                isDatabaseAdmin = false,
                emailVerified = false
            )
            repository.updateStats(unlinked)
            _verificationOtp.value = ""
            onComplete()
        }
    }

    // Secure One-Time Email Verification (OTP) generator
    private val _verificationOtp = MutableStateFlow("")
    val verificationOtp: StateFlow<String> = _verificationOtp.asStateFlow()

    fun generateSessionOtp(): String {
        val otp = (100000..999999).random().toString()
        _verificationOtp.value = otp
        return otp
    }

    fun markAccountAsVerified(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val current = stats.value
            val verified = current.copy(emailVerified = true)
            repository.updateStats(verified)
            
            // Sync status to cloud database securely if cloud synced
            if (verified.cloudSynced && verified.cloudUserId.isNotEmpty()) {
                try {
                    com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                        com.example.chess.data.FirebaseService.currentProjectId,
                        "players",
                        verified.cloudUserId,
                        com.example.chess.data.FirebaseService.currentApiKey,
                        com.example.chess.data.FirebaseService.buildFirestoreDocument(verified)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ChessViewModel", "Syncing verified status to Firestore failed: ${e.message}")
                }
            }
            onComplete()
        }
    }

    fun fetchPlayerAnalytics(onResult: (List<com.example.chess.data.GameStats>?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = com.example.chess.data.FirebaseService.firestoreApi.getCollection(
                    com.example.chess.data.FirebaseService.currentProjectId,
                    "players",
                    com.example.chess.data.FirebaseService.currentApiKey
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val documents = resp.body()!!.documents ?: emptyList()
                    val players = documents.mapNotNull { doc ->
                        com.example.chess.data.FirebaseService.parseFirestoreDocument(doc)
                    }
                    onResult(players, null)
                } else {
                    onResult(null, "Refused registry fetch with status: ${resp.code()}")
                }
            } catch (e: Exception) {
                onResult(null, "Registry connection failed: ${e.message}")
            }
        }
    }

    fun configureCustomFirebase(apiKey: String, projectId: String) {
        if (apiKey.isNotBlank() && projectId.isNotBlank()) {
            com.example.chess.data.FirebaseService.currentApiKey = apiKey.trim()
            com.example.chess.data.FirebaseService.currentProjectId = projectId.trim()
        }
    }

    fun updatePlayerAdminStatus(cloudUserId: String, status: String, newWarningLevel: Int) {
        viewModelScope.launch {
            try {
                if (cloudUserId.isEmpty()) return@launch
                val resp = com.example.chess.data.FirebaseService.firestoreApi.getDocument(
                    com.example.chess.data.FirebaseService.currentProjectId,
                    "players",
                    cloudUserId,
                    com.example.chess.data.FirebaseService.currentApiKey
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val p = com.example.chess.data.FirebaseService.parseFirestoreDocument(resp.body()!!)
                    if (p != null) {
                        val updated = p.copy(accountStatus = status, warningLevel = newWarningLevel)
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "players",
                            cloudUserId,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            com.example.chess.data.FirebaseService.buildFirestoreDocument(updated)
                        )
                        // Trigger a refetch of leaderboard list so UI updates
                        fetchPlayerAnalytics { _, _ -> }
                    }
                }
            } catch (e: Exception) {
                // error updating status
            }
        }
    }

    private val _leaderboardState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val leaderboardState: StateFlow<LeaderboardUiState> = _leaderboardState.asStateFlow()

    fun fetchLeaderboard() {
        _leaderboardState.value = LeaderboardUiState.Loading
        viewModelScope.launch {
            try {
                val resp = com.example.chess.data.FirebaseService.firestoreApi.getCollection(
                    com.example.chess.data.FirebaseService.currentProjectId,
                    "players",
                    com.example.chess.data.FirebaseService.currentApiKey
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val documents = resp.body()!!.documents ?: emptyList()
                    val players = documents.mapNotNull { doc ->
                        com.example.chess.data.FirebaseService.parseFirestoreDocument(doc)
                    }
                    val sortedTopTen = players
                        .sortedByDescending { it.battleScore }
                        .take(10)
                    _leaderboardState.value = LeaderboardUiState.Success(sortedTopTen)
                } else {
                    _leaderboardState.value = LeaderboardUiState.Error("Failed to load: HTTP ${resp.code()}")
                }
            } catch (e: Exception) {
                _leaderboardState.value = LeaderboardUiState.Error(e.message ?: "Network connection failed.")
            }
        }
    }

    fun firebaseCreateAccountWithPassword(
        email: String,
        password: String,
        fullName: String,
        dob: String,
        phone: String,
        onSuccess: (idToken: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = com.example.chess.data.FirebaseAuthRequest(email.trim(), password)
                val resp = com.example.chess.data.FirebaseService.authApi.signUp(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                
                if (resp.isSuccessful && resp.body() != null) {
                    val authBody = resp.body()!!
                    val localId = authBody.localId ?: ""
                    val idToken = authBody.idToken ?: ""
                    
                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val nowStr = formatter.format(java.util.Date())
                    val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                    
                    val currentLocalStats = stats.value
                    val newCloudProfile = currentLocalStats.copy(
                        fullName = if (fullName.isNotBlank()) fullName.trim() else "Pro Chess Player",
                        email = email.trim(),
                        phoneNumber = phone.trim(),
                        dob = dob.trim(),
                        playerId = generatedPlayerId,
                        lastLogin = nowStr,
                        deviceInformation = device,
                        cloudSynced = true,
                        cloudUserId = localId,
                        creationDate = nowStr,
                        isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com"),
                        emailVerified = false
                    )
                    
                    repository.updateStats(newCloudProfile)
                    
                    try {
                        val firestoreDoc = com.example.chess.data.FirebaseService.buildFirestoreDocument(newCloudProfile)
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "players",
                            localId,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            firestoreDoc
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseSync", "Initial firestore write threw: ${e.message}")
                    }
                    
                    onSuccess(idToken)
                } else {
                    val errorStr = resp.errorBody()?.string() ?: ""
                    val containsDemoPlaceholder = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                    
                    if (containsDemoPlaceholder) {
                        val localId = "DEMO-USER-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                        val idToken = "DEMO-TOKEN-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
                        
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val nowStr = formatter.format(java.util.Date())
                        val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        
                        val currentLocalStats = stats.value
                        val newCloudProfile = currentLocalStats.copy(
                            fullName = if (fullName.isNotBlank()) fullName.trim() else "Sandbox Player",
                            email = email.trim(),
                            phoneNumber = phone.trim(),
                            dob = dob.trim(),
                            playerId = generatedPlayerId,
                            lastLogin = nowStr,
                            deviceInformation = device,
                            cloudSynced = true,
                            cloudUserId = localId,
                            creationDate = nowStr,
                            isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com"),
                            emailVerified = false
                        )
                        repository.updateStats(newCloudProfile)
                        onSuccess(idToken)
                    } else {
                        val parsedMsg = when {
                            errorStr.contains("EMAIL_EXISTS") -> "This email matches an existing account. Please go to the LOG IN tab."
                            errorStr.contains("INVALID_EMAIL") -> "The email address you provided is invalid."
                            errorStr.contains("WEAK_PASSWORD") -> "Password must be at least 6 characters."
                            else -> "Sign Up failed: Email already exists or connection is invalid."
                        }
                        onFailure(parsedMsg)
                    }
                }
            } catch (e: Exception) {
                val containsDemoPlaceholder = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                if (containsDemoPlaceholder) {
                    val localId = "DEMO-USER-OFFLINE"
                    val idToken = "DEMO-TOKEN-OFFLINE"
                    
                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val nowStr = formatter.format(java.util.Date())
                    val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                    
                    val newCloudProfile = stats.value.copy(
                        fullName = if (fullName.isNotBlank()) fullName.trim() else "Offline Challenger",
                        email = email.trim(),
                        phoneNumber = phone.trim(),
                        dob = dob.trim(),
                        playerId = generatedPlayerId,
                        lastLogin = nowStr,
                        deviceInformation = device,
                        cloudSynced = true,
                        cloudUserId = localId,
                        creationDate = nowStr,
                        isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com"),
                        emailVerified = false
                    )
                    repository.updateStats(newCloudProfile)
                    onSuccess(idToken)
                } else {
                    onFailure(e.message ?: "Authentication network is unavailable.")
                }
            }
        }
    }

    fun firebaseCreateAccountOnly(
        email: String,
        fullName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseCreateAccountWithPassword(email, "MasterChessProPass2026!", fullName, "1995-10-15", "", { onSuccess() }, onFailure)
    }

    fun firebaseLoginWithPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onVerificationRequired: ((String) -> Unit)? = null,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = com.example.chess.data.FirebaseAuthRequest(email.trim(), password)
                val resp = com.example.chess.data.FirebaseService.authApi.signIn(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val authBody = resp.body()!!
                    val localId = authBody.localId ?: ""
                    val idToken = authBody.idToken ?: ""
                    
                    // Lookup user to check emailVerified
                    val lookupReq = com.example.chess.data.FirebaseUserLookupRequest(idToken)
                    val lookupResp = com.example.chess.data.FirebaseService.authApi.lookupUser(
                        com.example.chess.data.FirebaseService.currentApiKey,
                        lookupReq
                    )
                    
                    var isVerified = false
                    if (lookupResp.isSuccessful && lookupResp.body()?.users?.isNotEmpty() == true) {
                        isVerified = lookupResp.body()!!.users!!.first().emailVerified == true
                    }
                    
                    val containsDemo = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                    if (!isVerified && !containsDemo) {
                        onVerificationRequired?.invoke(idToken)
                        return@launch
                    }
                    
                    val docResp = com.example.chess.data.FirebaseService.firestoreApi.getDocument(
                        com.example.chess.data.FirebaseService.currentProjectId,
                        "players",
                        localId,
                        com.example.chess.data.FirebaseService.currentApiKey
                    )
                    
                    if (docResp.isSuccessful && docResp.body() != null) {
                        val cloudProfile = com.example.chess.data.FirebaseService.parseFirestoreDocument(docResp.body()!!)
                        if (cloudProfile != null) {
                            if (cloudProfile.accountStatus == "Banned") {
                                onFailure("This account has been permanently banned.")
                                return@launch
                            }
                            if (cloudProfile.accountStatus == "Inactive") {
                                onFailure("This account is currently deactivated.")
                                return@launch
                            }
                            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val nowStr = formatter.format(java.util.Date())
                            val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                            
                            val updatedProfile = cloudProfile.copy(
                                lastLogin = nowStr,
                                deviceInformation = device,
                                cloudSynced = true,
                                cloudUserId = localId,
                                isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekadi90@gmail.com")
                            )
                            
                            repository.updateStats(updatedProfile)
                            
                            viewModelScope.launch {
                                try {
                                    com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                                        com.example.chess.data.FirebaseService.currentProjectId,
                                        "players",
                                        localId,
                                        com.example.chess.data.FirebaseService.currentApiKey,
                                        com.example.chess.data.FirebaseService.buildFirestoreDocument(updatedProfile)
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("FirebaseSync", "Failed syncing login time to Firestore: ${e.message}")
                                }
                            }
                            
                            onSuccess()
                        } else {
                            onFailure("Decoding error: The player's registry record has an unexpected format.")
                        }
                    } else {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val nowStr = formatter.format(java.util.Date())
                        val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        
                        val newProfile = stats.value.copy(
                            fullName = "Pro Chess Player",
                            email = email.trim(),
                            playerId = generatedPlayerId,
                            lastLogin = nowStr,
                            deviceInformation = device,
                            cloudSynced = true,
                            cloudUserId = localId,
                            creationDate = nowStr,
                            isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com")
                        )
                        repository.updateStats(newProfile)
                        
                        try {
                            com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                                com.example.chess.data.FirebaseService.currentProjectId,
                                "players",
                                localId,
                                com.example.chess.data.FirebaseService.currentApiKey,
                                com.example.chess.data.FirebaseService.buildFirestoreDocument(newProfile)
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseSync", "Cloud record sync failed: ${e.message}")
                        }
                        onSuccess()
                    }
                } else {
                    val containsDemoPlaceholder = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                    if (containsDemoPlaceholder) {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val nowStr = formatter.format(java.util.Date())
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        
                        val demoUser = stats.value.copy(
                            fullName = "Challenger Pro",
                            email = email.trim(),
                            lastLogin = nowStr,
                            deviceInformation = device,
                            cloudSynced = true,
                            cloudUserId = "DEMO-LOCAL-" + java.util.UUID.randomUUID().toString().take(6),
                            isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com")
                        )
                        repository.updateStats(demoUser)
                        onSuccess()
                    } else {
                        val errorStr = resp.errorBody()?.string() ?: ""
                        val parsedMsg = when {
                            errorStr.contains("EMAIL_NOT_FOUND") || errorStr.contains("INVALID_LOGIN_CREDENTIALS") -> "No registered chess account matches this email. Please switch to the CREATE ACCOUNT tab."
                            errorStr.contains("USER_DISABLED") -> "Your master account has been disabled by administrators."
                            errorStr.contains("INVALID_PASSWORD") -> "Invalid login credentials."
                            else -> "Log In failed: No account registered or network is invalid."
                        }
                        onFailure(parsedMsg)
                    }
                }
            } catch (e: Exception) {
                val containsDemoPlaceholder = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                if (containsDemoPlaceholder) {
                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val nowStr = formatter.format(java.util.Date())
                    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                    
                    val demoUser = stats.value.copy(
                        fullName = "Challenger Offline",
                        email = email.trim(),
                        lastLogin = nowStr,
                        deviceInformation = device,
                        cloudSynced = true,
                        cloudUserId = "DEMO-OFFLINE-USER",
                        isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com")
                    )
                    repository.updateStats(demoUser)
                    onSuccess()
                } else {
                    onFailure(e.message ?: "Failed connecting to verification service.")
                }
            }
        }
    }

    fun firebaseResetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = com.example.chess.data.FirebaseOobCodeRequest(
                    requestType = "PASSWORD_RESET",
                    email = email.trim()
                )
                val resp = com.example.chess.data.FirebaseService.authApi.sendOobCode(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    val containsDemo = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                    if (containsDemo) {
                        onSuccess()
                    } else {
                        val errorStr = resp.errorBody()?.string() ?: ""
                        val msg = when {
                            errorStr.contains("EMAIL_NOT_FOUND") -> "No registered chess account matches this email address."
                            else -> "Recovery email setup failed. Please check network settings."
                        }
                        onFailure(msg)
                    }
                }
            } catch (e: Exception) {
                val containsDemo = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                if (containsDemo) {
                    onSuccess()
                } else {
                    onFailure(e.message ?: "Could not dispatch recovery instructions.")
                }
            }
        }
    }

    fun firebaseSendEmailVerification(
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = com.example.chess.data.FirebaseOobCodeRequest(
                    requestType = "VERIFY_EMAIL",
                    idToken = idToken
                )
                val resp = com.example.chess.data.FirebaseService.authApi.sendOobCode(
                    com.example.chess.data.FirebaseService.currentApiKey,
                    req
                )
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    val containsDemo = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                    if (containsDemo) {
                        onSuccess()
                    } else {
                        onFailure("Failed. Verification trigger denied.")
                    }
                }
            } catch (e: Exception) {
                val containsDemo = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                if (containsDemo) {
                    onSuccess()
                } else {
                    onFailure(e.message ?: "Verification email dispatcher failure.")
                }
            }
        }
    }

    fun firebaseGoogleSignInSandbox(
        email: String,
        fullName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val nowStr = formatter.format(java.util.Date())
                val generatedPlayerId = "PLY-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
                val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                
                val gUser = stats.value.copy(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    playerId = generatedPlayerId,
                    lastLogin = nowStr,
                    deviceInformation = device,
                    cloudSynced = true,
                    cloudUserId = "GOOGLE-USER-" + java.util.UUID.randomUUID().toString().take(6).uppercase(),
                    isDatabaseAdmin = (email.trim().lowercase() == "karanthekdi1303@gmail.com" || email.trim().lowercase() == "karanthekdi90@gmail.com")
                )
                repository.updateStats(gUser)
                
                val containsPlaceholder = com.example.chess.data.FirebaseService.currentApiKey.contains("demo") || com.example.chess.data.FirebaseService.currentApiKey.contains("placeholder")
                if (!containsPlaceholder) {
                    try {
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "players",
                            gUser.cloudUserId,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            com.example.chess.data.FirebaseService.buildFirestoreDocument(gUser)
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseSync", "Cloud sync failed for Google login: ${e.message}")
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "Google Sign In failed.")
            }
        }
    }

    fun dispatchDailyAdminReportCloud(
        context: android.content.Context,
        players: List<com.example.chess.data.GameStats>,
        onResult: (msg: String?, isSuccess: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val csvHeader = "Player Name,Email,Date of Birth,Phone,Battle Score,Wins,Losses,Draws,Rank,Player ID,Last Login,Account Creation Date,Device Information\n"
                val csvRows = players.joinToString("\n") { p ->
                    "${p.fullName.replace(",", " ")},${p.email},${p.dob},${p.phoneNumber},${p.battleScore},${p.wins},${p.losses},${p.draws},${p.playerTitle},${p.playerId},${p.lastLogin},${p.creationDate},${p.deviceInformation.replace(",", " ")}"
                }
                val fullCsvContent = csvHeader + csvRows
                
                val directory = java.io.File(context.filesDir, "secure_reports")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val reportFile = java.io.File(directory, "master_chess_report_$todayStr.csv")
                reportFile.writeText(fullCsvContent)
                
                // Allow admin to email themselves the report using SEND intent
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.packageName + ".provider",
                        reportFile
                    )
                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("karanthekadi90@gmail.com"))
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Chess Admin Backup: $todayStr")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Hello Admin,\n\nAttached is the automated database backup CSV.\n\nTotal Accounts: ${players.size}")
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Report via Email..."))
                } catch (e: Exception) {
                    android.util.Log.e("Admin", "Failed to launch intent: $e")
                }
                
                val reportKey = "report_$todayStr"
                val fields = mutableMapOf<String, com.example.chess.data.FirestoreValue>()
                fields["reportName"] = com.example.chess.data.FirestoreValue(stringValue = reportFile.name)
                fields["generationTime"] = com.example.chess.data.FirestoreValue(stringValue = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
                fields["totalPlayersCount"] = com.example.chess.data.FirestoreValue(integerValue = players.size.toString())
                fields["securePath"] = com.example.chess.data.FirestoreValue(stringValue = reportFile.absolutePath)
                fields["adminReceived"] = com.example.chess.data.FirestoreValue(booleanValue = true)
                fields["targetAdmin"] = com.example.chess.data.FirestoreValue(stringValue = "karanthekadi90@gmail.com")
                
                viewModelScope.launch {
                    try {
                        com.example.chess.data.FirebaseService.firestoreApi.updateDocument(
                            com.example.chess.data.FirebaseService.currentProjectId,
                            "daily_reports",
                            reportKey,
                            com.example.chess.data.FirebaseService.currentApiKey,
                            com.example.chess.data.FirestoreDocument(fields = fields)
                        )
                    } catch (e: java.lang.Exception) {
                        android.util.Log.e("AdminSync", "Daily backup doc sync threw: ${e.message}")
                    }
                }
                
                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("karanthekdi90@gmail.com"))
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "MASTER CHESS DAILY ADMINISTRATIVE ANALYTICS REPORT - $todayStr")
                    putExtra(android.content.Intent.EXTRA_TEXT, 
                        "Dear Administrative Director,\n\n" +
                        "This report was automatically compiled by the Master Chess Pro backend engine system.\n\n" +
                        "EXECUTIVE BRIEF:\n" +
                        "- Report Title: Daily Player Analytics Review\n" +
                        "- Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n" +
                        "- Active Registered Master Profiles: ${players.size}\n" +
                        "- Total Accumulated Games Played: ${players.sumOf { it.gamesPlayed }}\n" +
                        "- Destination Sandbox Vault: Saved securely in local database files directory (secure_reports/master_chess_report_$todayStr.csv)\n\n" +
                        "CSV DATA EXPORT (COMPACT FORMAT):\n" +
                        "=========================================\n" +
                        fullCsvContent + "\n" +
                        "=========================================\n\n" +
                        "CONFIDENTIALITY NOTICE: All user passwords are encrypted, hidden, and completely private. They are never exported or emailed in reports.\n\n" +
                        "Best regards,\n" +
                        "Backend Administrative Module v2026.06\n"
                    )
                }
                
                context.startActivity(android.content.Intent.createChooser(emailIntent, "Choose email client to send confidential report to karanthekdi90@gmail.com..."))
                
                onResult("Consolidated Executive Report generated and saved securely. Action triggered to send private email to karanthekdi90@gmail.com!", true)
            } catch (e: Exception) {
                onResult(e.message ?: "Failed compiling database CSV backup.", false)
            }
        }
    }

    fun firebaseLoginOnly(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseLoginWithPassword(email = email, password = "MasterChessProPass2026!", onSuccess = onSuccess, onFailure = onFailure)
    }
}

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    data class Success(val topPlayers: List<com.example.chess.data.GameStats>) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}
