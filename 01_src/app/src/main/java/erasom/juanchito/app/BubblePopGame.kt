package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bubble Pop: reconstrucción fiel de Frozen Bubble (Nokia, 2005).
 *
 * Mecánica original:
 *  · Lanzar burbujas para emparejar 3+ del mismo color.
 *  · Limpiar el tablero antes de que las burbujas bajen.
 *  · Múltiples niveles con dificultad creciente.
 *  · Puntuación por combos y burbujas eliminadas.
 */
class BubblePopGame : Game {
    override val name = "Bubble Pop"
    override val genre = "Puzzle"

    private var w = 0
    private var h = 0

    // Grid de burbujas
    private val cols = 8
    private val rows = 12
    private val bubbleSize = 40f
    private val grid = Array(rows) { IntArray(cols) }  // -1=empty, 0-5=color

    // Lanzador
    private var shooterX = 0f
    private var shooterY = 0f
    private var aimAngle = -90f  // grados
    private var currentColor = 0
    private var nextColor = 0

    // Burbuja en vuelo
    private var flyingBubble: FlyingBubble? = null
    private data class FlyingBubble(val color: Int, var x: Float, var y: Float, var vx: Float, var vy: Float)

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var level = 1
    private var bubblesPopped = 0

    // Animación
    private var popAnim = 0f
    private var popPositions = mutableListOf<Pair<Float, Float>>()

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val gridBg = paintOf("#2A2A3E")
    private val bubbleColors = listOf("#5C94FC", "#F8D878", "#D82800", "#00A800", "#8814A8", "#FC9838")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        shooterX = w / 2f
        shooterY = h - 60f
        reset()
    }

    override fun reset() {
        state = State.PLAYING
        scoreVal = 0
        level = 1
        bubblesPopped = 0
        currentColor = Random.nextInt(bubbleColors.size)
        nextColor = Random.nextInt(bubbleColors.size)
        flyingBubble = null
        popPositions.clear()
        popAnim = 0f
        generateLevel()
    }

    private fun generateLevel() {
        // Limpiar grid
        for (r in 0 until rows) for (c in 0 until cols) grid[r][c] = -1

        // Generar burbujas en las primeras filas
        val numRows = (3 + level).coerceAtMost(rows - 2)
        for (r in 0 until numRows) for (c in 0 until cols) {
            if (Random.nextFloat() < 0.85f) {
                grid[r][c] = Random.nextInt(bubbleColors.size)
            }
        }
    }

    private fun shoot() {
        if (flyingBubble != null) return
        val angleRad = Math.toRadians(aimAngle.toDouble())
        flyingBubble = FlyingBubble(
            currentColor,
            shooterX,
            shooterY,
            (kotlin.math.cos(angleRad) * 600f).toFloat(),
            (kotlin.math.sin(angleRad) * 600f).toFloat()
        )
        currentColor = nextColor
        nextColor = Random.nextInt(bubbleColors.size)
    }

    private fun snapBubble(fb: FlyingBubble) {
        // Encontrar la posición más cercana en el grid
        var bestR = 0; var bestC = 0
        var bestDist = Float.MAX_VALUE
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c] == -1) {
                val cx = c * bubbleSize + bubbleSize / 2f + (if (r % 2 == 1) bubbleSize / 2f else 0f)
                val cy = r * bubbleSize + bubbleSize / 2f
                val dx = fb.x - cx; val dy = fb.y - cy
                val dist = dx * dx + dy * dy
                if (dist < bestDist) { bestDist = dist; bestR = r; bestC = c }
            }
        }
        // Ajustar columna por offset de fila impar
        val offset = if (bestR % 2 == 1) bubbleSize / 2f else 0f
        val snapC = ((fb.x - offset) / bubbleSize).toInt().coerceIn(0, cols - 1)
        val snapR = (fb.y / bubbleSize).toInt().coerceIn(0, rows - 1)

        if (snapR in 0 until rows && snapC in 0 until cols && grid[snapR][snapC] == -1) {
            grid[snapR][snapC] = fb.color
            // Verificar matches
            val connected = findConnected(snapR, snapC, fb.color)
            if (connected.size >= 3) {
                for ((r, c) in connected) {
                    grid[r][c] = -1
                    popPositions.add(c * bubbleSize + bubbleSize / 2f + (if (r % 2 == 1) bubbleSize / 2f else 0f) to
                        r * bubbleSize + bubbleSize / 2f)
                }
                bubblesPopped += connected.size
                val pts = connected.size * 10 + (connected.size - 3) * 5
                scoreVal += pts
                if (scoreVal > best) best = scoreVal
                popAnim = 1f
                // Eliminar burbujas flotantes (no conectadas al techo)
                removeFloating()
            }
        }

        flyingBubble = null
        // Verificar game over (burbujas llegan al fondo)
        if (grid[rows - 1].any { it >= 0 }) {
            state = State.GAME_OVER
        }
        // Verificar nivel completo
        if (grid.flatMap { it.toList() }.all { it == -1 }) {
            state = State.LEVEL_COMPLETE
        }
    }

    private fun findConnected(r: Int, c: Int, color: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        val visited = Array(rows) { BooleanArray(cols) }
        val queue = mutableListOf<Pair<Int, Int>>()
        queue.add(r to c)
        visited[r][c] = true

        while (queue.isNotEmpty()) {
            val (cr, cc) = queue.removeAt(0)
            result.add(cr to cc)
            // Vecinos (6 direcciones en hex grid)
            val neighbors = getNeighbors(cr, cc)
            for ((nr, nc) in neighbors) {
                if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc] && grid[nr][nc] == color) {
                    visited[nr][nc] = true
                    queue.add(nr to nc)
                }
            }
        }
        return result
    }

    private fun getNeighbors(r: Int, c: Int): List<Pair<Int, Int>> {
        val neighbors = mutableListOf<Pair<Int, Int>>()
        // Arriba
        neighbors.add(r - 1 to c)
        // Abajo
        neighbors.add(r + 1 to c)
        // Izquierda
        neighbors.add(r to c - 1)
        // Derecha
        neighbors.add(r to c + 1)
        // Diagonales (dependiendo de la fila)
        if (r % 2 == 0) {
            neighbors.add(r - 1 to c - 1)
            neighbors.add(r + 1 to c - 1)
        } else {
            neighbors.add(r - 1 to c + 1)
            neighbors.add(r + 1 to c + 1)
        }
        return neighbors
    }

    private fun removeFloating() {
        val connected = Array(rows) { BooleanArray(cols) }
        // BFS desde el techo
        val queue = mutableListOf<Pair<Int, Int>>()
        for (c in 0 until cols) {
            if (grid[0][c] >= 0) {
                connected[0][c] = true
                queue.add(0 to c)
            }
        }
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeAt(0)
            for ((nr, nc) in getNeighbors(r, c)) {
                if (nr in 0 until rows && nc in 0 until cols && !connected[nr][nc] && grid[nr][nc] >= 0) {
                    connected[nr][nc] = true
                    queue.add(nr to nc)
                }
            }
        }
        // Eliminar no conectadas
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c] >= 0 && !connected[r][c]) {
                grid[r][c] = -1
            }
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (popAnim > 0f) popAnim -= dt * 2f

        when (state) {
            State.PLAYING -> {
                // Apuntar
                if (input.left) { aimAngle = (aimAngle - 2f).coerceAtLeast(-170f); input.left = false }
                if (input.right) { aimAngle = (aimAngle + 2f).coerceAtMost(-10f); input.right = false }
                if (input.up) { aimAngle = (aimAngle - 1f).coerceAtLeast(-170f); input.up = false }
                if (input.down) { aimAngle = (aimAngle + 1f).coerceAtMost(-10f); input.down = false }

                // Disparar
                if ((input.a || input.start) && flyingBubble == null) {
                    shoot()
                    input.a = false; input.start = false
                }

                // Mover burbuja en vuelo
                flyingBubble?.let { fb ->
                    fb.x += fb.vx * dt
                    fb.y += fb.vy * dt
                    // Rebote en paredes
                    if (fb.x < bubbleSize / 2f) { fb.x = bubbleSize / 2f; fb.vx = -fb.vx }
                    if (fb.x > w - bubbleSize / 2f) { fb.x = w - bubbleSize / 2f; fb.vx = -fb.vx }
                    // Llegó al techo o a una burbuja
                    if (fb.y < bubbleSize / 2f) {
                        snapBubble(fb)
                    }
                    // Colisión con burbujas existentes
                    for (r in 0 until rows) for (c in 0 until cols) {
                        if (grid[r][c] >= 0) {
                            val cx = c * bubbleSize + bubbleSize / 2f + (if (r % 2 == 1) bubbleSize / 2f else 0f)
                            val cy = r * bubbleSize + bubbleSize / 2f
                            val dx = fb.x - cx; val dy = fb.y - cy
                            if (dx * dx + dy * dy < bubbleSize * bubbleSize) {
                                snapBubble(fb)
                                return
                            }
                        }
                    }
                }
            }
            State.LEVEL_COMPLETE -> {
                if (input.start || input.a) {
                    level++
                    generateLevel()
                    state = State.PLAYING
                    input.start = false; input.a = false
                }
            }
            State.GAME_OVER -> {
                if (input.start || input.a) reset()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        // Grid de burbujas
        for (r in 0 until rows) for (c in 0 until cols) {
            val offset = if (r % 2 == 1) bubbleSize / 2f else 0f
            val x = c * bubbleSize + offset
            val y = r * bubbleSize
            val colorIdx = grid[r][c]
            if (colorIdx >= 0 && colorIdx < bubbleColors.size) {
                val bubblePaint = paintOf(bubbleColors[colorIdx])
                canvas.drawCircle(x + bubbleSize / 2f, y + bubbleSize / 2f, bubbleSize / 2f - 2f, bubblePaint)
                // Brillo
                val shine = paintOf("#FFFFFF")
                shine.alpha = 120
                canvas.drawCircle(x + bubbleSize / 2f - 5f, y + bubbleSize / 2f - 5f, bubbleSize / 6f, shine)
            }
        }

        // Burbuja en vuelo
        flyingBubble?.let { fb ->
            val bubblePaint = paintOf(bubbleColors[fb.color])
            canvas.drawCircle(fb.x, fb.y, bubbleSize / 2f - 2f, bubblePaint)
            val shine = paintOf("#FFFFFF")
            shine.alpha = 120
            canvas.drawCircle(fb.x - 5f, fb.y - 5f, bubbleSize / 6f, shine)
        }

        // Lanzador
        val shooterPaint = paintOf(bubbleColors[currentColor])
        canvas.drawCircle(shooterX, shooterY, bubbleSize / 2f - 2f, shooterPaint)
        val shine = paintOf("#FFFFFF")
        shine.alpha = 120
        canvas.drawCircle(shooterX - 5f, shooterY - 5f, bubbleSize / 6f, shine)

        // Próxima burbuja
        val nextPaint = paintOf(bubbleColors[nextColor])
        canvas.drawCircle(shooterX + 50f, shooterY, bubbleSize / 3f, nextPaint)

        // Línea de apuntado
        val angleRad = Math.toRadians(aimAngle.toDouble())
        val lineEndX = (shooterX + kotlin.math.cos(angleRad) * 80f).toFloat()
        val lineEndY = (shooterY + kotlin.math.sin(angleRad) * 80f).toFloat()
        val aimPaint = paintOf("#FFFFFF")
        aimPaint.style = Paint.Style.STROKE
        aimPaint.strokeWidth = 2f
        aimPaint.alpha = 100
        canvas.drawLine(shooterX, shooterY, lineEndX, lineEndY, aimPaint)

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("BURBUJAS: $bubblesPopped", w / 2f - 60f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Burbujas: $bubblesPopped", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Burbujas: $bubblesPopped", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("◀▶: apuntar  ·  A/START: disparar  ·  Empareja 3+", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
