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
    private var nextPiece = 0
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
        nextPiece = Random.nextInt(7)
        spawnPiece()
    }

    private fun spawnPiece() {
        piece = nextPiece
        nextPiece = Random.nextInt(7)
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

    /** Celda biselada estilo NES: cara clara arriba-izquierda, oscura abajo-derecha. */
    private fun drawCell(canvas: Canvas, x: Float, y: Float, size: Float, colorIdx: Int) {
        val base = Color.parseColor(colors[colorIdx])
        paint.color = base
        canvas.drawRect(x, y, x + size, y + size, paint)
        // bisel claro
        paint.color = Color.argb(255, Math.min(255, Color.red(base) + 60), Math.min(255, Color.green(base) + 60), Math.min(255, Color.blue(base) + 60))
        canvas.drawRect(x, y, x + size - 2f, y + 5f, paint)
        canvas.drawRect(x, y, x + 5f, y + size - 2f, paint)
        // bisel oscuro
        paint.color = Color.argb(255, Color.red(base) / 2, Color.green(base) / 2, Color.blue(base) / 2)
        canvas.drawRect(x, y + size - 4f, x + size, y + size, paint)
        canvas.drawRect(x + size - 4f, y, x + size, y + size, paint)
        // punto de brillo
        paint.color = Color.parseColor("#F8F8F8")
        paint.alpha = 90
        canvas.drawRect(x + size * 0.25f, y + size * 0.25f, x + size * 0.45f, y + size * 0.45f, paint)
        paint.alpha = 255
    }

    override fun draw(canvas: Canvas) {
        // Sala de máquinas: fondo morado + marco neón
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#181028", "#302048", 6)
        NesScenery.drawStars(canvas, w, h, 25)
        NesScenery.drawNeonFrame(canvas, w, h, "#00D9FF")
        val ox = (w - cols * cell) / 2f
        val oy = (h - rows * cell) / 2f
        // Marco del pozo
        paint.color = Color.parseColor("#7C7C7C")
        canvas.drawRect(ox - 6f, oy - 6f, ox + cols * cell + 6f, oy + rows * cell + 6f, paint)
        paint.color = Color.parseColor("#000000")
        canvas.drawRect(ox - 3f, oy - 3f, ox + cols * cell + 3f, oy + rows * cell + 3f, paint)
        // board
        val grid = Paint().apply { color = Color.parseColor("#1E2229"); isAntiAlias = false }
        for (r in 0 until rows) for (c in 0 until cols) {
            val v = board[r][c]
            val x = ox + c * cell; val y = oy + r * cell
            if (v != 0) {
                drawCell(canvas, x, y, cell - 1f, v)
            } else {
                paint.color = Color.parseColor("#101018")
                canvas.drawRect(x, y, x + cell - 1f, y + cell - 1f, paint)
                // rejilla sutil
                paint.color = Color.parseColor("#20202E")
                canvas.drawRect(x, y, x + cell - 1f, y + 1f, paint)
                canvas.drawRect(x, y, x + 1f, y + cell - 1f, paint)
            }
        }
        // current piece
        for (c in cells(piece, rot)) {
            val cx = px + c.first; val cy = py + c.second
            if (cy in 0 until rows && cx in 0 until cols) {
                drawCell(canvas, ox + cx * cell, oy + cy * cell, cell - 1f, piece + 1)
            }
        }
        // Vista previa de pieza siguiente
        val pvx = ox + cols * cell + 14f
        val pvy = oy + 10f
        paint.color = Color.parseColor("#F8F8F8")
        paint.strokeWidth = 2f; paint.style = Paint.Style.STROKE
        canvas.drawRect(pvx, pvy, pvx + cell * 4f + 8f, pvy + cell * 2.4f + 8f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("SIGUE", pvx + cell * 2f, pvy + 14f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8D878") })
        for (c in cells(nextPiece, 0)) {
            drawCell(canvas, pvx + 4f + c.first * cell, pvy + 22f + c.second * cell * 0.8f, cell * 0.7f, nextPiece + 1)
        }
        // Pixel mirando el pozo (decorativo, esquina inferior)
        PixelSprite.draw(canvas, ox - 24f, oy + rows * cell - 20f, 28f, facingRight = true, hatColor = 0x5C94FC)
        // HUD
        canvas.drawText("Score: $score", w / 2f, 30f, textPaint)
        canvas.drawText("Level: $level  Lines: $lines", w / 2f, 60f, smallPaint)
        if (isGameOver) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            canvas.drawText("GAME OVER", w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#FF6B6B") })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}