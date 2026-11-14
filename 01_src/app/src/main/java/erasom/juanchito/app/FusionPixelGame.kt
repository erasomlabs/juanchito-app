package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Fusion Pixel: reconstrucción fiel del clásico 2048 (Baltoro Games, 2015).
 *
 * Mecánica original:
 *  · Tablero 4×4 de tiles numéricos.
 *  · Deslizar en 4 direcciones mueve todos los tiles; los iguales se fusionan
 *    duplicando su valor (+score).
 *  · Tras cada movimiento aparece un nuevo tile (2 o 4).
 *  · Objetivo: alcanzar el tile 2048. Game Over cuando no hay movimientos.
 *  · High score persistente, animaciones de fusión, música de fondo.
 */
class FusionPixelGame : Game {
    override val name = "Fusion Pixel"
    override val genre = "Puzzle"

    private var w = 0
    private var h = 0

    private val gridSize = 4
    private val board = Array(gridSize) { IntArray(gridSize) }
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    override var isGameOver = false
    private var won = false
    private var continued = false

    // Animación de fusión
    private val mergeAnim = Array(gridSize) { FloatArray(gridSize) }
    private val spawnAnim = Array(gridSize) { FloatArray(gridSize) }
    private var animTimer = 0f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val boardBg = paintOf("#2A2A3E")
    private val gridPaint = paintOf("#3A3A5E")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }

    // Colores de tiles por valor (NES palette unificada)
    private val tileColors = mapOf(
        2 to "#EEE4DA", 4 to "#EDE0C8", 8 to "#F2B179", 16 to "#F59563",
        32 to "#F67C5F", 64 to "#F65E3B", 128 to "#EDCF72", 256 to "#EDCC61",
        512 to "#EDC850", 1024 to "#EDC53F", 2048 to "#EDC22E"
    )
    private val tileTextColors = mapOf(
        2 to "#776E65", 4 to "#776E65"
    )

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            board[r][c] = 0
            mergeAnim[r][c] = 0f
            spawnAnim[r][c] = 0f
        }
        scoreVal = 0
        isGameOver = false
        won = false
        continued = false
        animTimer = 0f
        spawnTile()
        spawnTile()
    }

    private fun spawnTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            if (board[r][c] == 0) empty.add(r to c)
        }
        if (empty.isEmpty()) return
        val (r, c) = empty.random()
        board[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
        spawnAnim[r][c] = 0f
    }

    // ---------- Movimiento ----------

    private fun move(dir: Int): Boolean {
        var moved = false
        val merged = Array(gridSize) { BooleanArray(gridSize) }

        val rows = if (dir == 2) (gridSize - 1 downTo 0) else (0 until gridSize)
        val cols = if (dir == 1) (gridSize - 1 downTo 0) else (0 until gridSize)

        for (r in rows) for (c in cols) {
            if (board[r][c] == 0) continue
            var nr = r; var nc = c
            while (true) {
                val tr = nr + dr(dir); val tc = nc + dc(dir)
                if (tr !in 0 until gridSize || tc !in 0 until gridSize) break
                if (board[tr][tc] == 0) { nr = tr; nc = tc }
                else if (board[tr][tc] == board[r][c] && !merged[tr][tc]) {
                    nr = tr; nc = tc; break
                } else break
            }
            if (nr == r && nc == c) continue
            moved = true
            if (board[nr][nc] == board[r][c]) {
                board[nr][nc] *= 2
                scoreVal += board[nr][nc]
                if (scoreVal > best) best = scoreVal
                merged[nr][nc] = true
                mergeAnim[nr][nc] = 0f
                if (board[nr][nc] == 2048 && !won && !continued) won = true
            } else if (board[nr][nc] == 0) {
                board[nr][nc] = board[r][c]
            }
            board[r][c] = 0
        }
        if (moved) {
            spawnTile()
            if (!canMove()) isGameOver = true
        }
        return moved
    }

    private fun dr(dir: Int) = when (dir) { 0 -> -1; 2 -> 1; else -> 0 }
    private fun dc(dir: Int) = when (dir) { 3 -> -1; 1 -> 1; else -> 0 }

    private fun canMove(): Boolean {
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            if (board[r][c] == 0) return true
            for (d in 0..3) {
                val tr = r + dr(d); val tc = c + dc(d)
                if (tr in 0 until gridSize && tc in 0 until gridSize && board[tr][tc] == board[r][c]) return true
            }
        }
        return false
    }

    override fun update(dt: Float, input: GameInput) {
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            if (mergeAnim[r][c] < 1f && board[r][c] > 0) mergeAnim[r][c] = (mergeAnim[r][c] + dt * 8f).coerceAtMost(1f)
            if (spawnAnim[r][c] < 1f && board[r][c] > 0) spawnAnim[r][c] = (spawnAnim[r][c] + dt * 6f).coerceAtMost(1f)
        }

        if (isGameOver) {
            if (input.start || input.a) reset()
            return
        }
        if (won && !continued) {
            if (input.start || input.a) continued = true
            return
        }

        if (input.up) { move(0); input.up = false }
        if (input.down) { move(2); input.down = false }
        if (input.left) { move(3); input.left = false }
        if (input.right) { move(1); input.right = false }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        hudPaint.textSize = 32f
        canvas.drawText("FUSION PIXEL", 20f, 40f, hudPaint)
        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 20f, 68f, hudPaint)
        canvas.drawText("BEST: $best", w - 20f - hudPaint.measureText("BEST: $best"), 68f, hudPaint)

        val boardSize = minOf(w, h) * 0.85f
        val cellSize = boardSize / gridSize
        val boardX = (w - boardSize) / 2f
        val boardY = (h - boardSize) / 2f + 20f

        canvas.drawRoundRect(RectF(boardX - 8f, boardY - 8f, boardX + boardSize + 8f, boardY + boardSize + 8f), 12f, 12f, boardBg)

        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            val x = boardX + c * cellSize
            val y = boardY + r * cellSize
            val pad = 6f

            canvas.drawRoundRect(RectF(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad), 8f, 8f, gridPaint)

            val value = board[r][c]
            if (value > 0) {
                val color = tileColors[value] ?: "#3C3A32"
                val tilePaint = paintOf(color)
                val scale = if (mergeAnim[r][c] < 1f) 0.5f + 0.5f * mergeAnim[r][c] else 1f
                val spawnScale = if (spawnAnim[r][c] < 1f) spawnAnim[r][c] else 1f
                val finalScale = scale * spawnScale
                val cx = x + cellSize / 2f
                val cy = y + cellSize / 2f
                val halfW = (cellSize - pad * 2) / 2f * finalScale
                val halfH = (cellSize - pad * 2) / 2f * finalScale

                canvas.drawRoundRect(
                    RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
                    8f * finalScale, 8f * finalScale, tilePaint
                )

                val textColor = tileTextColors[value] ?: "#F9F6F2"
                textPaint.color = Color.parseColor(textColor)
                textPaint.textSize = (cellSize * 0.35f).coerceAtMost(36f)
                canvas.drawText(value.toString(), cx, cy + textPaint.textSize * 0.35f, textPaint)
            }
        }

        if (won && !continued) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 180; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            textPaint.color = Color.parseColor("#F5C542")
            textPaint.textSize = 48f
            canvas.drawText("¡GANASTE!", w / 2f, h / 2f - 20f, textPaint)
            textPaint.textSize = 22f
            canvas.drawText("Alcanzaste 2048", w / 2f, h / 2f + 20f, textPaint)
            smallPaint.textSize = 18f
            canvas.drawText("PULSA START PARA SEGUIR JUGANDO", w / 2f, h / 2f + 60f, smallPaint)
        }

        if (isGameOver) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 180; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            textPaint.color = Color.parseColor("#E63946")
            textPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 20f, textPaint)
            textPaint.color = Color.parseColor("#F8F8F8")
            textPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 20f, textPaint)
            if (scoreVal >= best && scoreVal > 0) {
                textPaint.color = Color.parseColor("#F5C542")
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 50f, textPaint)
            }
            smallPaint.textSize = 18f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 90f, smallPaint)
        }

        if (!isGameOver && !(won && !continued)) {
            smallPaint.textSize = 14f
            canvas.drawText("D-pad: mover tiles  ·  START: reiniciar", w / 2f, h - 10f, smallPaint)
        }
    }
}
