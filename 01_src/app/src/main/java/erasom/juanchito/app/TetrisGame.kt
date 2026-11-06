package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class TetrisGame : Game {
    override val name = "Block Drop"
    override val genre = "Puzzle"
    private var w = 0; private var h = 0
    private val cols = 10; private val rows = 20
    private var cell = 0f
    private val board = Array(rows) { IntArray(cols) } // 0 empty, 1-7 piece color
    private var piece = 0; private var px = 0; private var py = 0
    private var rot = 0
    private var dropTimer = 0f; private var dropInterval = 0.8f
    private var softDrop = false
    override var isGameOver = false
    override var score = 0
    private var level = 1; private var lines = 0
    private var highScore = 0
    private val colors = listOf("#000000", "#5C94FC", "#00A800", "#FC9838", "#F8D878", "#D82800", "#8814A8", "#F8F8F8")
    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    // Tetrominoes: each piece has 4 rotations, each rotation 4 cells (x,y)
    private val pieces = listOf(
        // I
        listOf(listOf(Pair(0,1),Pair(1,1),Pair(2,1),Pair(3,1)), listOf(Pair(1,0),Pair(1,1),Pair(1,2),Pair(1,3)), listOf(Pair(0,2),Pair(1,2),Pair(2,2),Pair(3,2)), listOf(Pair(2,0),Pair(2,1),Pair(2,2),Pair(2,3))),
        // O
        listOf(listOf(Pair(0,0),Pair(1,0),Pair(0,1),Pair(1,1)), listOf(Pair(0,0),Pair(1,0),Pair(0,1),Pair(1,1)), listOf(Pair(0,0),Pair(1,0),Pair(0,1),Pair(1,1)), listOf(Pair(0,0),Pair(1,0),Pair(0,1),Pair(1,1))),
        // T
        listOf(listOf(Pair(1,0),Pair(0,1),Pair(1,1),Pair(2,1)), listOf(Pair(1,0),Pair(1,1),Pair(2,1),Pair(1,2)), listOf(Pair(0,1),Pair(1,1),Pair(2,1),Pair(1,2)), listOf(Pair(1,0),Pair(0,1),Pair(1,1),Pair(1,2))),
        // S
        listOf(listOf(Pair(1,0),Pair(2,0),Pair(0,1),Pair(1,1)), listOf(Pair(1,0),Pair(1,1),Pair(2,1),Pair(2,2)), listOf(Pair(1,1),Pair(2,1),Pair(0,2),Pair(1,2)), listOf(Pair(0,0),Pair(0,1),Pair(1,1),Pair(1,2))),
        // Z
        listOf(listOf(Pair(0,0),Pair(1,0),Pair(1,1),Pair(2,1)), listOf(Pair(2,0),Pair(1,1),Pair(2,1),Pair(1,2)), listOf(Pair(0,1),Pair(1,1),Pair(1,2),Pair(2,2)), listOf(Pair(1,0),Pair(0,1),Pair(1,1),Pair(0,2))),
        // J
        listOf(listOf(Pair(0,0),Pair(0,1),Pair(1,1),Pair(2,1)), listOf(Pair(1,0),Pair(2,0),Pair(1,1),Pair(1,2)), listOf(Pair(0,1),Pair(1,1),Pair(2,1),Pair(2,2)), listOf(Pair(1,0),Pair(1,1),Pair(0,2),Pair(1,2))),
        // L
        listOf(listOf(Pair(2,0),Pair(0,1),Pair(1,1),Pair(2,1)), listOf(Pair(1,0),Pair(1,1),Pair(1,2),Pair(2,2)), listOf(Pair(0,1),Pair(1,1),Pair(2,1),Pair(0,2)), listOf(Pair(0,0),Pair(1,0),Pair(1,1),Pair(1,2)))
    )

    override fun init(width: Int, height: Int) {
        w = width; h = height
        cell = (h / rows).coerceAtMost(w / cols).toFloat()
        reset()
    }

    override fun reset() {
        for (r in 0 until rows) for (c in 0 until cols) board[r][c] = 0
        score = 0; lines = 0; level = 1; dropInterval = 0.8f
        isGameOver = false; highScore = maxOf(highScore, score)
        spawnPiece()
    }

    private fun spawnPiece() {
        piece = Random.nextInt(7)
        px = cols / 2 - 2; py = 0; rot = 0
        if (!canPlace(piece, px, py, rot)) isGameOver = true
    }

    private fun cells(p: Int, r: Int) = pieces[p][r % 4]

    private fun canPlace(p: Int, x: Int, y: Int, r: Int): Boolean {
        for (c in cells(p, r)) {
            val cx = x + c.first; val cy = y + c.second
            if (cx < 0 || cx >= cols || cy >= rows) return false
            if (cy >= 0 && board[cy][cx] != 0) return false
        }
        return true
    }

    private fun lockPiece() {
        for (c in cells(piece, rot)) {
            val cx = px + c.first; val cy = py + c.second
            if (cy in 0 until rows && cx in 0 until cols) board[cy][cx] = piece + 1
        }
        clearLines()
        spawnPiece()
    }

    private fun clearLines() {
        var cleared = 0
        var r = rows - 1
        while (r >= 0) {
            if (board[r].all { it != 0 }) {
                for (rr in r downTo 1) board[rr] = board[rr-1].copyOf()
                board[0] = IntArray(cols)
                cleared++; r++ // recheck same row
            }
            r--
        }
        if (cleared > 0) {
            val pts = intArrayOf(0, 100, 300, 500, 800)[cleared.coerceAtMost(4)] * level
            score += pts
            lines += cleared
            level = lines / 10 + 1
            dropInterval = (0.8f - (level - 1) * 0.07f).coerceAtLeast(0.1f)
            if (score > highScore) highScore = score
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        if (input.left) { if (canPlace(piece, px-1, py, rot)) px--; input.left = false }
        if (input.right) { if (canPlace(piece, px+1, py, rot)) px++; input.right = false }
        if (input.up) { val nr = (rot+1)%4; if (canPlace(piece, px, py, nr)) rot = nr; input.up = false }
        if (input.down) softDrop = true else softDrop = false
        if (input.a) { // hard drop
            while (canPlace(piece, px, py+1, rot)) py++
            lockPiece(); input.a = false; return
        }
        dropTimer += dt * (if (softDrop) 8f else 1f)
        if (dropTimer >= dropInterval) {
            dropTimer = 0f
            if (canPlace(piece, px, py+1, rot)) py++ else lockPiece()
        }
        if (input.start) { /* pause not implemented */ }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#000000"))
        val ox = (w - cols * cell) / 2f
        val oy = (h - rows * cell) / 2f
        // board
        for (r in 0 until rows) for (c in 0 until cols) {
            val v = board[r][c]
            if (v != 0) {
                paint.color = Color.parseColor(colors[v])
                canvas.drawRect(ox + c*cell, oy + r*cell, ox + (c+1)*cell-1, oy + (r+1)*cell-1, paint)
            } else {
                paint.color = Color.parseColor("#2D3748")
                canvas.drawRect(ox + c*cell, oy + r*cell, ox + (c+1)*cell-1, oy + (r+1)*cell-1, paint)
            }
        }
        // current piece
        for (c in cells(piece, rot)) {
            val cx = px + c.first; val cy = py + c.second
            if (cy in 0 until rows && cx in 0 until cols) {
                paint.color = Color.parseColor(colors[piece+1])
                canvas.drawRect(ox + cx*cell, oy + cy*cell, ox + (cx+1)*cell-1, oy + (cy+1)*cell-1, paint)
            }
        }
        // HUD
        canvas.drawText("Score: $score", w/2f, 30f, textPaint)
        canvas.drawText("Level: $level  Lines: $lines", w/2f, 60f, smallPaint)
        if (isGameOver) {
            canvas.drawText("GAME OVER", w/2f, h/2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#FF6B6B") })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}