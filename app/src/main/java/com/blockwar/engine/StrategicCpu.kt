package com.blockwar.engine

import com.blockwar.model.BlockPiece
import com.blockwar.model.Cell
import com.blockwar.model.PieceType

/** Version 2: deterministic two-ply search, mirrored by server/functions/strategic-cpu.js. */
object StrategicCpu {
    private data class Piece(val type: Int, val x: Int, val y: Int, val r: Int)
    private data class Candidate(val x: Int, val r: Int, val board: Array<IntArray>, val reward: Int, val score: Int)

    fun decide(board: List<List<Cell?>>, piece: BlockPiece, next: PieceType): OpponentAI.MoveDecision {
        val grid = Array(board.size) { y -> IntArray(board[y].size) { x -> if (board[y][x] == null) 0 else 1 } }
        val p = Piece(piece.type.blockCount, piece.x, piece.y, piece.rotation)
        val options = candidates(grid, p)
        var best: Candidate? = null
        var bestScore = Int.MIN_VALUE
        for (c in options) {
            val future = candidates(c.board, Piece(next.blockCount, (grid[0].size-next.blockCount)/2, 0, 0))
            val futureScore = future.maxOfOrNull { it.score } ?: -1000000
            // Weight the actual next piece without losing the value of an immediate attack.
            val score = c.reward * 10 + (c.score-c.reward) * 3 + futureScore * 7
            if (score > bestScore) { bestScore = score; best = c }
        }
        return OpponentAI.MoveDecision(best?.x ?: piece.x, best?.r ?: piece.rotation, bestScore.toDouble())
    }

    private fun valid(b: Array<IntArray>, p: Piece): Boolean = (0 until p.type).all { i ->
        val x=p.x+if(p.r%2==0)i else 0
        val y=p.y+if(p.r%2==1)i else 0
        x in b[0].indices && y < b.size && (y < 0 || b[y][x]==0)
    }
    private fun rotate(b: Array<IntArray>, p: Piece): Piece? {
        for(dx in intArrayOf(0,-1,1,-2,2)) {
            val q=p.copy(x=p.x+dx,r=(p.r+1)%4)
            if(valid(b,q)) return q
        }
        return null
    }
    private fun candidates(b: Array<IntArray>, initial: Piece): List<Candidate> {
        val result=mutableListOf<Candidate>()
        for(r in 0..1) for(x in b[0].indices) {
            var p=initial
            while(!valid(b,p) && p.y > -4) p=p.copy(y=p.y-1)
            if(!valid(b,p)) continue
            var turns=0
            while(p.r!=r && turns++<4) { p=rotate(b,p) ?: break }
            if(p.r!=r) continue
            while(p.x!=x) {
                val q=p.copy(x=p.x+if(x>p.x)1 else -1)
                if(!valid(b,q)) break
                p=q
            }
            if(p.x!=x) continue
            while(valid(b,p.copy(y=p.y+1))) p=p.copy(y=p.y+1)
            if(p.y<0) continue // Never prefer a placement that loses immediately.
            val copy=Array(b.size) { b[it].clone() }
            for(i in 0 until p.type) copy[p.y+if(r%2==1)i else 0][p.x+if(r%2==0)i else 0]=1
            val remaining=copy.filter { row -> row.any { it==0 } }
            val lines=b.size-remaining.size
            val cleared=Array(b.size) { y -> if(y<lines)IntArray(b[0].size) else remaining[y-lines] }
            val reward=intArrayOf(0,5,8,12,16)[lines.coerceAtMost(4)]*40
            result.add(Candidate(x,r,cleared,reward,reward+evaluate(cleared)))
        }
        return result
    }
    private fun evaluate(b: Array<IntArray>): Int {
        val heights=IntArray(b[0].size)
        var holes=0; var covered=0
        for(x in heights.indices) {
            var blocks=0
            for(y in b.indices) {
                if(b[y][x]!=0) { if(blocks==0) heights[x]=b.size-y; blocks++ }
                else if(blocks>0) { holes++; covered+=blocks }
            }
        }
        val max=heights.maxOrNull() ?: 0
        var bump=0; var wells=0
        for(x in heights.indices) {
            if(x>0) bump+=kotlin.math.abs(heights[x]-heights[x-1])
            val left=if(x==0)b.size else heights[x-1]
            val right=if(x==heights.lastIndex)b.size else heights[x+1]
            val depth=(minOf(left,right)-heights[x]-3).coerceAtLeast(0)
            wells+=depth*depth
        }
        return -8*heights.sum()-120*holes-10*covered-6*bump-2*max*max-12*wells
    }
}
