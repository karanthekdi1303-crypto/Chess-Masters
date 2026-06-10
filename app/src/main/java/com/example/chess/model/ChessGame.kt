package com.example.chess.model

import kotlin.math.abs

enum class PieceColor { WHITE, BLACK }

enum class PieceType(val value: Int) {
    PAWN(1), KNIGHT(3), BISHOP(3), ROOK(5), QUEEN(9), KING(1000)
}

data class Piece(val type: PieceType, val color: PieceColor)

data class Position(val row: Int, val col: Int)

data class Move(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false
)

class ChessGame(initializeBoard: Boolean = true) {
    var board: Array<Array<Piece?>> = Array(8) { Array(8) { null } }
        private set
    var turn: PieceColor = PieceColor.WHITE
    
    var moveHistory = mutableListOf<Move>()
        private set
    
    var isCheck: Boolean = false
        private set
    var isMate: Boolean = false
        private set
    var isStalemate: Boolean = false
        private set
    
    // Castling availability
    var whiteCanCastleKingside = true
    var whiteCanCastleQueenside = true
    var blackCanCastleKingside = true
    var blackCanCastleQueenside = true

    init {
        if (initializeBoard) {
            resetBoard()
        }
    }

    fun resetBoard() {
        board = Array(8) { Array(8) { null } }
        turn = PieceColor.WHITE
        moveHistory.clear()
        isCheck = false
        isMate = false
        isStalemate = false
        whiteCanCastleKingside = true
        whiteCanCastleQueenside = true
        blackCanCastleKingside = true
        blackCanCastleQueenside = true

        // Pawns
        for (col in 0..7) {
            board[1][col] = Piece(PieceType.PAWN, PieceColor.BLACK)
            board[6][col] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }
        val backRowRank = arrayOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
        for (col in 0..7) {
            board[0][col] = Piece(backRowRank[col], PieceColor.BLACK)
            board[7][col] = Piece(backRowRank[col], PieceColor.WHITE)
        }
    }

    fun syncWithJS(jsState: JSGameState) {
        this.board = Array(8) { r -> Array(8) { c -> jsState.board[r][c] } }
        this.turn = jsState.turn
        this.isCheck = jsState.isCheck
        this.isMate = jsState.isGameOver && jsState.isCheckmate
        this.isStalemate = jsState.isGameOver && jsState.isStalemate
    }


    fun makeMove(move: Move): Boolean {
        if (isMate || isStalemate) return false
        val piece = board[move.from.row][move.from.col] ?: return false
        if (piece.color != turn) return false
        
        val validMoves = getValidMovesFor(move.from)
        val validMove = validMoves.find { it.to == move.to && it.promotion == move.promotion }
        if (validMove == null) return false

        applyMove(validMove)
        
        turn = if (turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        moveHistory.add(validMove)
        
        updateGameState()
        return true
    }
    
    fun applyMove(move: Move) {
        val piece = board[move.from.row][move.from.col]!!
        board[move.from.row][move.from.col] = null
        board[move.to.row][move.to.col] = piece

        if (move.promotion != null) {
            board[move.to.row][move.to.col] = Piece(move.promotion, piece.color)
        }

        if (move.isEnPassant) {
            val dir = if (piece.color == PieceColor.WHITE) 1 else -1
            board[move.to.row + dir][move.to.col] = null
        }

        if (move.isCastling) {
            if (move.to.col == 6) { // Kingside
                val rook = board[move.from.row][7]
                board[move.from.row][7] = null
                board[move.from.row][5] = rook
            } else if (move.to.col == 2) { // Queenside
                val rook = board[move.from.row][0]
                board[move.from.row][0] = null
                board[move.from.row][3] = rook
            }
        }
        
        // Update castling rights
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                whiteCanCastleKingside = false
                whiteCanCastleQueenside = false
            } else {
                blackCanCastleKingside = false
                blackCanCastleQueenside = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (move.from.row == 7 && move.from.col == 0) whiteCanCastleQueenside = false
            if (move.from.row == 7 && move.from.col == 7) whiteCanCastleKingside = false
            if (move.from.row == 0 && move.from.col == 0) blackCanCastleQueenside = false
            if (move.from.row == 0 && move.from.col == 7) blackCanCastleKingside = false
        }
    }
    
    fun undoLastMove() {
        if (moveHistory.isEmpty()) return
        // Real undo would be complex involving state restoration. For simplicty, a cheap way is reconstructing the board state.
        // Given we are maintaining history, we can just rebuild up to n-1.
        val movesToApply = moveHistory.toList().dropLast(1)
        resetBoard()
        for (m in movesToApply) {
            applyMove(m)
            turn = if (turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
            moveHistory.add(m)
        }
        updateGameState()
    }
    
    // Create a copy of the game to simulate moves
    fun deepCopy(): ChessGame {
        val copy = ChessGame(initializeBoard = false)
        for (row in 0..7) {
            for (col in 0..7) {
                copy.board[row][col] = this.board[row][col]
            }
        }
        copy.turn = this.turn
        copy.whiteCanCastleKingside = this.whiteCanCastleKingside
        copy.whiteCanCastleQueenside = this.whiteCanCastleQueenside
        copy.blackCanCastleKingside = this.blackCanCastleKingside
        copy.blackCanCastleQueenside = this.blackCanCastleQueenside
        copy.isCheck = this.isCheck
        copy.isMate = this.isMate
        copy.isStalemate = this.isStalemate
        // We do not copy moveHistory completely if it's slow, but we'll need the last move for en passant
        if (this.moveHistory.isNotEmpty()) {
            copy.moveHistory.add(this.moveHistory.last())
        }
        return copy
    }

    private fun updateGameState() {
        val kingPos = findKing(turn)
        isCheck = kingPos != null && isSquareAttacked(kingPos.row, kingPos.col, turn)
        
        var hasValidMove = false
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == turn) {
                    if (getValidMovesFor(Position(r, c)).isNotEmpty()) {
                        hasValidMove = true
                        break
                    }
                }
            }
            if (hasValidMove) break
        }
        
        if (!hasValidMove) {
            if (isCheck) isMate = true
            else isStalemate = true
        }
    }
    
    fun getValidMovesFor(pos: Position): List<Move> {
        val piece = board[pos.row][pos.col] ?: return emptyList()
        if (piece.color != turn) return emptyList()
        
        val pseudoMoves = getPseudoLegalMoves(pos, piece)
        val validMoves = mutableListOf<Move>()
        
        for (pm in pseudoMoves) {
            val copy = this.deepCopy()
            copy.applyMove(pm)
            val kingPosCopy = copy.findKing(piece.color)
            if (kingPosCopy != null && !copy.isSquareAttacked(kingPosCopy.row, kingPosCopy.col, piece.color)) {
                validMoves.add(pm)
            }
        }
        return validMoves
    }
    
    private fun getPseudoLegalMoves(pos: Position, piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        val row = pos.row
        val col = pos.col
        
        val dirs = when (piece.type) {
            PieceType.ROOK -> listOf(Pair(-1,0), Pair(1,0), Pair(0,-1), Pair(0,1))
            PieceType.BISHOP -> listOf(Pair(-1,-1), Pair(-1,1), Pair(1,-1), Pair(1,1))
            PieceType.QUEEN -> listOf(Pair(-1,0), Pair(1,0), Pair(0,-1), Pair(0,1), Pair(-1,-1), Pair(-1,1), Pair(1,-1), Pair(1,1))
            PieceType.KNIGHT -> listOf(Pair(-2,-1), Pair(-2,1), Pair(-1,-2), Pair(-1,2), Pair(1,-2), Pair(1,2), Pair(2,-1), Pair(2,1))
            PieceType.KING -> listOf(Pair(-1,0), Pair(1,0), Pair(0,-1), Pair(0,1), Pair(-1,-1), Pair(-1,1), Pair(1,-1), Pair(1,1))
            PieceType.PAWN -> emptyList() // Handled specially
        }
        
        if (piece.type != PieceType.PAWN && piece.type != PieceType.KNIGHT && piece.type != PieceType.KING) {
            for (dir in dirs) {
                var r = row + dir.first
                var c = col + dir.second
                while (r in 0..7 && c in 0..7) {
                    val target = board[r][c]
                    if (target == null) {
                        moves.add(Move(pos, Position(r, c), isCapture = false))
                    } else {
                        if (target.color != piece.color) {
                            moves.add(Move(pos, Position(r, c), isCapture = true))
                        }
                        break
                    }
                    r += dir.first
                    c += dir.second
                }
            }
        } else if (piece.type == PieceType.KNIGHT || piece.type == PieceType.KING) {
            for (dir in dirs) {
                val r = row + dir.first
                val c = col + dir.second
                if (r in 0..7 && c in 0..7) {
                    val target = board[r][c]
                    if (target == null || target.color != piece.color) {
                        moves.add(Move(pos, Position(r, c), isCapture = target != null))
                    }
                }
            }
            // Castling
            if (piece.type == PieceType.KING) {
                if (piece.color == PieceColor.WHITE) {
                    if (whiteCanCastleKingside && board[7][5] == null && board[7][6] == null) {
                        if (!isSquareAttacked(7, 4, PieceColor.WHITE) && !isSquareAttacked(7, 5, PieceColor.WHITE)) {
                            moves.add(Move(pos, Position(7, 6), isCastling = true))
                        }
                    }
                    if (whiteCanCastleQueenside && board[7][1] == null && board[7][2] == null && board[7][3] == null) {
                        if (!isSquareAttacked(7, 4, PieceColor.WHITE) && !isSquareAttacked(7, 3, PieceColor.WHITE)) {
                            moves.add(Move(pos, Position(7, 2), isCastling = true))
                        }
                    }
                } else {
                    if (blackCanCastleKingside && board[0][5] == null && board[0][6] == null) {
                        if (!isSquareAttacked(0, 4, PieceColor.BLACK) && !isSquareAttacked(0, 5, PieceColor.BLACK)) {
                            moves.add(Move(pos, Position(0, 6), isCastling = true))
                        }
                    }
                    if (blackCanCastleQueenside && board[0][1] == null && board[0][2] == null && board[0][3] == null) {
                        if (!isSquareAttacked(0, 4, PieceColor.BLACK) && !isSquareAttacked(0, 3, PieceColor.BLACK)) {
                            moves.add(Move(pos, Position(0, 2), isCastling = true))
                        }
                    }
                }
            }
        } else if (piece.type == PieceType.PAWN) {
            val dir = if (piece.color == PieceColor.WHITE) -1 else 1
            val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
            val promoRow = if (piece.color == PieceColor.WHITE) 0 else 7
            
            // Forward 1
            if (row + dir in 0..7 && board[row + dir][col] == null) {
                if (row + dir == promoRow) {
                    listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                        moves.add(Move(pos, Position(row + dir, col), promotion = it))
                    }
                } else {
                    moves.add(Move(pos, Position(row + dir, col)))
                }
                
                // Forward 2
                if (row == startRow && board[row + 2 * dir][col] == null) {
                    moves.add(Move(pos, Position(row + 2 * dir, col)))
                }
            }
            // Capture
            for (cOffset in listOf(-1, 1)) {
                val r = row + dir
                val c = col + cOffset
                if (r in 0..7 && c in 0..7) {
                    val target = board[r][c]
                    if (target != null && target.color != piece.color) {
                        if (r == promoRow) {
                            listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                                moves.add(Move(pos, Position(r, c), promotion = it, isCapture = true))
                            }
                        } else {
                            moves.add(Move(pos, Position(r, c), isCapture = true))
                        }
                    } else if (target == null && moveHistory.isNotEmpty()) { // En passant
                        val lastMove = moveHistory.last()
                        val lastPiece = board[lastMove.to.row][lastMove.to.col]
                        if (lastPiece?.type == PieceType.PAWN && abs(lastMove.from.row - lastMove.to.row) == 2) {
                            if (lastMove.to.col == c && lastMove.to.row == row) {
                                moves.add(Move(pos, Position(r, c), isCapture = true, isEnPassant = true))
                            }
                        }
                    }
                }
            }
        }
        
        return moves
    }

    private fun findKing(color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p?.type == PieceType.KING && p.color == color) return Position(r, c)
            }
        }
        return null
    }

    fun isSquareAttacked(r: Int, c: Int, defendingColor: PieceColor): Boolean {
        // A naive but correct enough check: pretend a superset piece is at r,c and see if it hits an enemy.
        // We do this by tracing out lines
        val attackingColor = if (defendingColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        
        // Check Knight
        val knightDirs = listOf(Pair(-2,-1), Pair(-2,1), Pair(-1,-2), Pair(-1,2), Pair(1,-2), Pair(1,2), Pair(2,-1), Pair(2,1))
        for (d in knightDirs) {
            val nr = r + d.first
            val nc = c + d.second
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p?.color == attackingColor && p.type == PieceType.KNIGHT) return true
            }
        }
        
        // Lines
        val lines = listOf(Pair(-1,0), Pair(1,0), Pair(0,-1), Pair(0,1))
        for (d in lines) {
            var nr = r + d.first
            var nc = c + d.second
            while (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null) {
                    if (p.color == attackingColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += d.first
                nc += d.second
            }
        }
        
        // Diagonals
        val diags = listOf(Pair(-1,-1), Pair(-1,1), Pair(1,-1), Pair(1,1))
        for (d in diags) {
            var nr = r + d.first
            var nc = c + d.second
            while (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null) {
                    if (p.color == attackingColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += d.first
                nc += d.second
            }
        }
        
        // Pawns
        val pawnDir = if (defendingColor == PieceColor.WHITE) -1 else 1
        for (dc in listOf(-1, 1)) {
            val nr = r + pawnDir
            val nc = c + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p?.color == attackingColor && p.type == PieceType.PAWN) return true
            }
        }
        
        // Kings
        val kingDirs = listOf(Pair(-1,0), Pair(1,0), Pair(0,-1), Pair(0,1), Pair(-1,-1), Pair(-1,1), Pair(1,-1), Pair(1,1))
        for (d in kingDirs) {
            val nr = r + d.first
            val nc = c + d.second
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p?.color == attackingColor && p.type == PieceType.KING) return true
            }
        }
        
        return false
    }

    val capturedWhite: List<PieceType>
        get() = moveHistory.filter { it.isCapture }.mapNotNull { m -> 
            val pColor = if (moveHistory.indexOf(m) % 2 == 0) PieceColor.WHITE else PieceColor.BLACK
            if (pColor == PieceColor.BLACK) board[m.to.row][m.to.col]?.type // Note: incomplete tracking due to overrides. Will do simple diff. 
            null
        }
        // Instead of calculating from history properly, better compute from missing pieces
        
    fun getCapturedPieces(color: PieceColor): List<PieceType> {
        val original = mutableListOf<PieceType>()
        original.addAll(List(8) { PieceType.PAWN })
        original.addAll(listOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)) // Ignore king
        
        val current = mutableListOf<PieceType>()
        for(r in 0..7) {
            for(c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == color && p.type != PieceType.KING) {
                    current.add(p.type)
                }
            }
        }
        
        for (c in current) {
            original.remove(c)
        }
        return original
    }
}
