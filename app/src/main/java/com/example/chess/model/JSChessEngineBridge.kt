package com.example.chess.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

class JSChessEngineBridge(context: Context) {
    private val webView: WebView = WebView(context.applicationContext).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoaded = true
                onEngineReady?.invoke()
            }
        }
        loadUrl("file:///android_asset/chess.html")
    }

    var isLoaded = false
        private set

    var onEngineReady: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private suspend fun eval(js: String): String = suspendCancellableCoroutine { continuation ->
        mainHandler.post {
            webView.evaluateJavascript(js) { result ->
                if (continuation.isActive) {
                    val rawString = try {
                        val value = JSONTokener(result).nextValue()
                        if (value is String) value else result ?: ""
                    } catch (e: Exception) {
                        result ?: ""
                    }
                    continuation.resume(rawString)
                }
            }
        }
    }

    suspend fun reset(): JSGameState {
        val resultStr = eval("resetGame()")
        return parseState(resultStr)
    }

    suspend fun loadFen(fen: String): JSGameState {
        val resultStr = eval("loadFenString('${fen.replace("'", "\\'")}')")
        val json = JSONObject(resultStr)
        return parseState(json.getJSONObject("state").toString())
    }

    suspend fun getValidMoves(square: String): List<JSMove> {
        val resultStr = eval("getValidMoves('$square')")
        val arr = JSONArray(resultStr)
        val list = mutableListOf<JSMove>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(parseMove(obj))
        }
        return list
    }

    suspend fun getAllValidMoves(): List<JSMove> {
        val resultStr = eval("getAllValidMoves()")
        val arr = JSONArray(resultStr)
        val list = mutableListOf<JSMove>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(parseMove(obj))
        }
        return list
    }

    suspend fun makeMove(from: String, to: String, promotion: String? = null): JSMoveResult {
        val promoArg = if (promotion != null) "'$promotion'" else "null"
        val resultStr = eval("makeChessMove('$from', '$to', $promoArg)")
        val json = JSONObject(resultStr)
        val success = json.getBoolean("success")
        if (!success) {
            return JSMoveResult(false, null, null)
        }
        val moveObj = json.optJSONObject("move")
        val stateObj = json.getJSONObject("state")
        return JSMoveResult(
            success = true,
            move = if (moveObj != null) parseMove(moveObj) else null,
            state = parseState(stateObj.toString())
        )
    }

    suspend fun undo(): JSGameState {
        val resultStr = eval("undoChessMove()")
        val json = JSONObject(resultStr)
        return parseState(json.getJSONObject("state").toString())
    }

    private fun parseState(jsonStr: String): JSGameState {
        val json = JSONObject(jsonStr)
        val fen = json.getString("fen")
        val turn = json.getString("turn")
        val isCheck = json.getBoolean("isCheck")
        val isCheckmate = json.getBoolean("isCheckmate")
        val isStalemate = json.getBoolean("isStalemate")
        val isDraw = json.getBoolean("isDraw")
        val isGameOver = json.getBoolean("isGameOver")

        val boardArr = json.getJSONArray("board")
        val board = Array(8) { Array<Piece?>(8) { null } }

        for (row in 0..7) {
            val rowArr = boardArr.getJSONArray(row)
            for (col in 0..7) {
                if (rowArr.isNull(col)) {
                    board[row][col] = null
                } else {
                    val pieceObj = rowArr.optJSONObject(col)
                    if (pieceObj != null) {
                        val type = pieceObj.getString("type")
                        val colorStr = pieceObj.getString("color")
                        board[row][col] = Piece(
                            type = when (type) {
                                "p" -> PieceType.PAWN
                                "n" -> PieceType.KNIGHT
                                "b" -> PieceType.BISHOP
                                "r" -> PieceType.ROOK
                                "q" -> PieceType.QUEEN
                                "k" -> PieceType.KING
                                else -> PieceType.PAWN
                            },
                            color = if (colorStr == "w") PieceColor.WHITE else PieceColor.BLACK
                        )
                    } else {
                        board[row][col] = null
                    }
                }
            }
        }

        return JSGameState(
            fen = fen,
            turn = if (turn == "w") PieceColor.WHITE else PieceColor.BLACK,
            isCheck = isCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isDraw = isDraw,
            isGameOver = isGameOver,
            board = board
        )
    }

    private fun parseMove(obj: JSONObject): JSMove {
        val from = obj.getString("from")
        val to = obj.getString("to")
        val color = obj.getString("color")
        val flags = obj.optString("flags", "")
        val piece = obj.optString("piece", "p")
        val captured = obj.optString("captured", "")
        val promotion = obj.optString("promotion", "")

        return JSMove(
            from = from,
            to = to,
            color = if (color == "w") PieceColor.WHITE else PieceColor.BLACK,
            isCapture = captured.isNotEmpty() || flags.contains("c"),
            isEnPassant = flags.contains("e"),
            isCastling = flags.contains("k") || flags.contains("q") || flags.contains("m") || flags.contains("p"),
            promotion = if (promotion.isEmpty()) null else when (promotion) {
                "q" -> PieceType.QUEEN
                "r" -> PieceType.ROOK
                "b" -> PieceType.BISHOP
                "n" -> PieceType.KNIGHT
                else -> null
            }
        )
    }
}

data class JSGameState(
    val fen: String,
    val turn: PieceColor,
    val isCheck: Boolean,
    val isCheckmate: Boolean,
    val isStalemate: Boolean,
    val isDraw: Boolean,
    val isGameOver: Boolean,
    val board: Array<Array<Piece?>>
)

data class JSMove(
    val from: String,
    val to: String,
    val color: PieceColor,
    val isCapture: Boolean,
    val isEnPassant: Boolean,
    val isCastling: Boolean,
    val promotion: PieceType?
) {
    fun toMove(): Move {
        return Move(
            from = fromAlgebraic(from),
            to = fromAlgebraic(to),
            promotion = promotion,
            isCapture = isCapture,
            isEnPassant = isEnPassant,
            isCastling = isCastling
        )
    }

    companion object {
        fun toAlgebraic(row: Int, col: Int): String {
            val file = ('a' + col).toString()
            val rank = ('8' - row).toString()
            return "$file$rank"
        }

        fun fromAlgebraic(alg: String): Position {
            val col = alg[0] - 'a'
            val row = '8' - alg[1]
            return Position(row, col)
        }
    }
}

data class JSMoveResult(
    val success: Boolean,
    val move: JSMove?,
    val state: JSGameState?
)
