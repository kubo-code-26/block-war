package com.blockwar.model

data class BlockPiece(
    val type: PieceType,
    val x: Int,
    val y: Int,
    val rotation: Int = 0
) {
    val shape: List<List<Int>>
        get() = type.getShape(rotation)

    fun getOccupiedCoordinates(): List<Pair<Int, Int>> {
        val coords = mutableListOf<Pair<Int, Int>>()
        val currentShape = shape
        for (r in currentShape.indices) {
            for (c in currentShape[r].indices) {
                if (currentShape[r][c] != 0) {
                    coords.add(Pair(x + c, y + r))
                }
            }
        }
        return coords
    }

    fun move(dx: Int, dy: Int): BlockPiece = copy(x = x + dx, y = y + dy)

    fun rotateClockwise(): BlockPiece = copy(rotation = (rotation + 1) % 4)

    fun rotateCounterClockwise(): BlockPiece = copy(rotation = (rotation + 3) % 4)

    companion object {
        fun spawn(type: PieceType, boardWidth: Int = PlayerState.BOARD_COLS): BlockPiece {
            val shape = type.getShape(0)
            val pieceWidth = shape[0].size
            val startX = (boardWidth - pieceWidth) / 2
            val startY = 0
            return BlockPiece(type = type, x = startX, y = startY, rotation = 0)
        }
    }
}
