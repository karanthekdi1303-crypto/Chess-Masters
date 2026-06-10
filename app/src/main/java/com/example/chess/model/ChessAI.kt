package com.example.chess.model

class ChessAI {
    
    // Evaluation weights
    private val pieceValues = mapOf(
        PieceType.PAWN to 10,
        PieceType.KNIGHT to 30,
        PieceType.BISHOP to 30,
        PieceType.ROOK to 50,
        PieceType.QUEEN to 90,
        PieceType.KING to 900
    )

    fun calculateBestMove(game: ChessGame, depth: Int, color: PieceColor): Move? {
        val allMoves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = game.board[r][c]
                if (p != null && p.color == color) {
                    allMoves.addAll(game.getValidMovesFor(Position(r, c)))
                }
            }
        }
        if (allMoves.isEmpty()) return null
        
        if (depth == 1) { // Beginner - random with slightly more chance for capture
            val captures = allMoves.filter { it.isCapture }
            if (captures.isNotEmpty() && Math.random() > 0.5) {
                return captures.random()
            }
            return allMoves.random()
        }

        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        var beta = Int.MAX_VALUE

        // Shuffle moves to add variety
        allMoves.shuffle()

        // Prioritize captures and promotions for better pruning
        allMoves.sortByDescending { it.isCapture || it.promotion != null }

        for (move in allMoves) {
            val copy = game.deepCopy()
            copy.makeMove(move)
            val score = minimax(copy, depth - 1, alpha, beta, false, color)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = maxOf(alpha, bestScore)
            if (beta <= alpha) break
        }

        return bestMove ?: allMoves.random()
    }

    private fun minimax(game: ChessGame, depth: Int, alphaParam: Int, betaParam: Int, isMaximizing: Boolean, aiColor: PieceColor): Int {
        if (depth == 0 || game.isMate || game.isStalemate) {
            if (game.isMate) {
                return if (isMaximizing) -10000 else 10000
            }
            if (game.isStalemate) {
                return 0
            }
            return evaluateBoard(game.board, aiColor)
        }

        var alpha = alphaParam
        var beta = betaParam
        val color = if (isMaximizing) aiColor else if (aiColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

        val allMoves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = game.board[r][c]
                if (p != null && p.color == color) {
                    allMoves.addAll(game.getValidMovesFor(Position(r, c)))
                }
            }
        }
        
        allMoves.sortByDescending { it.isCapture || it.promotion != null }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in allMoves) {
                val copy = game.deepCopy()
                copy.makeMove(move)
                val eval = minimax(copy, depth - 1, alpha, beta, false, aiColor)
                maxEval = maxOf(maxEval, eval)
                alpha = maxOf(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in allMoves) {
                val copy = game.deepCopy()
                copy.makeMove(move)
                val eval = minimax(copy, depth - 1, alpha, beta, true, aiColor)
                minEval = minOf(minEval, eval)
                beta = minOf(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(board: Array<Array<Piece?>>, aiColor: PieceColor): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null) {
                    val value = pieceValues[p.type] ?: 0
                    // Central control bias
                    val centerBias = if (r in 3..4 && c in 3..4) 1 else 0
                    if (p.color == aiColor) {
                        score += value + centerBias
                    } else {
                        score -= (value + centerBias)
                    }
                }
            }
        }
        return score
    }
}
