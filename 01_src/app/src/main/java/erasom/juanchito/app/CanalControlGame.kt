package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Canal Control: reconstrucción fiel de Canal Control (mob.ua, 2010).
 *
 * Mecánica original (Pipe Mania style):
 *  · Conectar tuberías para que el agua fluya de la fuente al desagüe.
 *  · Rotar piezas de tubería para crear un camino continuo.
 *  · El agua fluye después de un tiempo; si no hay camino, game over.
 *  · Múltiples niveles con mayor complejidad.
 */
class CanalControlGame : Game {
    override val name = "Canal Control"
    override val genre = "Puzzle"

    private var w = 0
    private var h = 0

    // Grid
    private val gridSize = 6
    private var cellSize = 0f
    private var gridX = 0f
    private var gridY = 0f

    // Tipos de tubería: 0=vacío, 1=recta H, 2=recta V, 3=curva ┌, 4=curva ┐, 5=curva └, 6=curva ┘, 7=fuente, 8=desagüe
    private val grid = Array(gridSize) { IntArray(gridSize) }
    private val solution = Array(gridSize) { IntArray(gridSize) }

    // Cursor
    private var cursorR = 0
    private var cursorC = 0

    // Estado
    private enum class State { PLAYING, LEVEL_COMPLETE, GAME_OVER }
    private var state = State.PLAYING

    // Agua
    private var waterTimer = 0f
    private var waterDelay = 5f  // segundos antes de que fluya el agua
    private var waterFlowing = false
    private var waterPath = mutableListOf<Pair<Int, Int>>()
    private var waterProgress = 0f

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var level = 1
    private var maxLevel = 20

    // Animación
    private var flowAnim = 0f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val gridBg = paintOf("#2A2A3E")
    private val cellBg = paintOf("#3A3A5E")
    private val pipePaint = paintOf("#5C94FC")
    private val waterPaint = paintOf("#00BFFF")
    private val sourcePaint = paintOf("#00E676")
    private val drainPaint = paintOf("#FF5252")
    private val cursorPaint = paintOf("#F5C542")
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
        cellSize = (minOf(w, h) * 0.7f / gridSize).coerceAtMost(60f)
        val gridW = cellSize * gridSize
        val gridH = cellSize * gridSize
        gridX = (w - gridW) / 2f
        gridY = (h - gridH) / 2f + 30f
        reset()
    }

    override fun reset() {
        state = State.PLAYING
        scoreVal = 0
        level = 1
        waterTimer = 0f
        waterFlowing = false
        waterPath.clear()
        waterProgress = 0f
        flowAnim = 0f
        cursorR = 0
        cursorC = 0
        generateLevel()
    }

    private fun generateLevel() {
        // Limpiar grid
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            grid[r][c] = 0
        }

        // Fuente en la esquina superior izquierda
        grid[0][0] = 7  // fuente
        // Desagüe en la esquina inferior derecha
        grid[gridSize - 1][gridSize - 1] = 8  // desagüe

        // Generar tuberías aleatorias entre fuente y desagüe
        // Crear un camino base
        var r = 0; var c = 0
        val path = mutableListOf<Pair<Int, Int>>()
        path.add(0 to 0)
        while (r < gridSize - 1 || c < gridSize - 1) {
            if (r < gridSize - 1 && (c == gridSize - 1 || Random.nextBoolean())) {
                r++
            } else if (c < gridSize - 1) {
                c++
            }
            if (r < gridSize && c < gridSize && grid[r][c] == 0) {
                path.add(r to c)
            }
        }
        // Colocar tuberías en el camino
        for (i in 1 until path.size - 1) {
            val (pr, pc) = path[i]
            val prev = path[i - 1]
            val next = path.getOrElse(i + 1) { path[i] }
            // Determinar tipo de tubería según dirección
            val fromUp = prev.first == pr - 1
            val fromDown = prev.first == pr + 1
            val fromLeft = prev.second == pc - 1
            val fromRight = prev.second == pc + 1
            val toUp = next.first == pr - 1
            val toDown = next.first == pr + 1
            val toLeft = next.second == pc - 1
            val toRight = next.second == pc + 1

            grid[pr][pc] = when {
                (fromLeft || toRight) && (fromRight || toLeft) && !(fromUp || toUp || fromDown || toDown) -> 1  // horizontal
                (fromUp || toDown) && (fromDown || toUp) && !(fromLeft || toLeft || fromRight || toRight) -> 2  // vertical
                (fromLeft || toDown) && (fromDown || toLeft) -> 3  // ┌
                (fromRight || toDown) && (fromDown || toRight) -> 4  // ┐
                (fromUp || toRight) && (fromRight || toUp) -> 5  // └
                (fromUp || toLeft) && (fromLeft || toUp) -> 6  // ┘
                else -> Random.nextInt(1, 7)
            }
        }

        // Añadir tuberías extra aleatorias para dificultad
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            if (grid[r][c] == 0 && Random.nextFloat() < 0.3f) {
                grid[r][c] = Random.nextInt(1, 7)
            }
        }

        waterTimer = 0f
        waterFlowing = false
        waterPath.clear()
        waterProgress = 0f
        flowAnim = 0f
    }

    private fun rotatePipe(r: Int, c: Int) {
        if (r !in 0 until gridSize || c !in 0 until gridSize) return
        if (grid[r][c] in 7..8) return  // no rotar fuente/desagüe
        if (grid[r][c] == 0) return
        grid[r][c] = when (grid[r][c]) {
            1 -> 2  // H -> V
            2 -> 1  // V -> H
            3 -> 4  // ┌ -> ┐
            4 -> 6  // ┐ -> ┘
            5 -> 3  // └ -> ┌
            6 -> 5  // ┘ -> └
            else -> grid[r][c]
        }
    }

    private fun checkWaterPath(): Boolean {
        // BFS desde la fuente
        val visited = Array(gridSize) { BooleanArray(gridSize) }
        val queue = mutableListOf<Pair<Int, Int>>()
        queue.add(0 to 0)
        visited[0][0] = true
        var found = false

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeAt(0)
            if (r == gridSize - 1 && c == gridSize - 1) { found = true; break }

            // Verificar conexiones según tipo de tubería
            val neighbors = getConnections(r, c)
            for ((nr, nc) in neighbors) {
                if (nr in 0 until gridSize && nc in 0 until gridSize && !visited[nr][nc] && grid[nr][nc] != 0) {
                    visited[nr][nc] = true
                    queue.add(nr to nc)
                }
            }
        }
        return found
    }

    private fun getConnections(r: Int, c: Int): List<Pair<Int, Int>> {
        val connections = mutableListOf<Pair<Int, Int>>()
        val type = grid[r][c]
        // Cada tipo de tubería conecta en ciertas direcciones
        when (type) {
            1 -> { // horizontal: izq-der
                connections.add(r to c - 1)
                connections.add(r to c + 1)
            }
            2 -> { // vertical: arriba-abajo
                connections.add(r - 1 to c)
                connections.add(r + 1 to c)
            }
            3 -> { // ┌: arriba-der
                connections.add(r - 1 to c)
                connections.add(r to c + 1)
            }
            4 -> { // ┐: arriba-izq
                connections.add(r - 1 to c)
                connections.add(r to c - 1)
            }
            5 -> { // └: abajo-der
                connections.add(r + 1 to c)
                connections.add(r to c + 1)
            }
            6 -> { // ┘: abajo-izq
                connections.add(r + 1 to c)
                connections.add(r to c - 1)
            }
            7 -> { // fuente: todas las direcciones
                connections.add(r - 1 to c)
                connections.add(r + 1 to c)
                connections.add(r to c - 1)
                connections.add(r to c + 1)
            }
            8 -> { // desagüe: todas las direcciones
                connections.add(r - 1 to c)
                connections.add(r + 1 to c)
                connections.add(r to c - 1)
                connections.add(r to c + 1)
            }
        }
        return connections
    }

    override fun update(dt: Float, input: GameInput) {
        flowAnim += dt

        when (state) {
            State.PLAYING -> {
                waterTimer += dt
                if (waterTimer >= waterDelay && !waterFlowing) {
                    waterFlowing = true
                    waterProgress = 0f
                    // Verificar si hay camino
                    if (checkWaterPath()) {
                        // El agua fluye exitosamente
                        waterProgress += dt * 0.5f
                        if (waterProgress >= 1f) {
                            state = State.LEVEL_COMPLETE
                            scoreVal += 100 + (waterDelay - waterTimer + waterDelay).toInt() * 10
                            if (scoreVal > best) best = scoreVal
                        }
                    } else {
                        // No hay camino - game over
                        state = State.GAME_OVER
                    }
                }

                // Navegación del cursor
                if (input.up) { cursorR = (cursorR - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { cursorR = (cursorR + 1).coerceAtMost(gridSize - 1); input.down = false }
                if (input.left) { cursorC = (cursorC - 1).coerceAtLeast(0); input.left = false }
                if (input.right) { cursorC = (cursorC + 1).coerceAtMost(gridSize - 1); input.right = false }

                // Rotar tubería (A)
                if (input.a) {
                    rotatePipe(cursorR, cursorC)
                    input.a = false
                }

                // Reiniciar nivel (B)
                if (input.b) {
                    generateLevel()
                    input.b = false
                }

                // Reiniciar timer (START)
                if (input.start) {
                    waterTimer = 0f
                    waterFlowing = false
                    input.start = false
                }
            }
            State.LEVEL_COMPLETE -> {
                if (input.start || input.a) {
                    level++
                    if (level > maxLevel) {
                        state = State.GAME_OVER
                    } else {
                        generateLevel()
                        state = State.PLAYING
                    }
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

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        // Timer
        if (!waterFlowing) {
            val timeLeft = (waterDelay - waterTimer).coerceAtLeast(0f)
            titlePaint.textSize = 20f
            titlePaint.color = if (timeLeft < 2f) Color.parseColor("#FF5252") else Color.parseColor("#F5C542")
            canvas.drawText("AGUA EN: ${timeLeft.toInt()}s", w / 2f - 40f, 50f, titlePaint)
            titlePaint.color = Color.parseColor("#F5C542")
        } else {
            titlePaint.textSize = 20f
            titlePaint.color = Color.parseColor("#00BFFF")
            canvas.drawText("¡AGUA FLUYENDO!", w / 2f - 40f, 50f, titlePaint)
            titlePaint.color = Color.parseColor("#F5C542")
        }

        // Grid
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            val x = gridX + c * cellSize
            val y = gridY + r * cellSize
            val pad = 3f

            // Fondo de celda
            canvas.drawRoundRect(RectF(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad), 4f, 4f, cellBg)

            // Cursor
            if (r == cursorR && c == cursorC) {
                val cursorP = paintOf("#F5C542")
                cursorP.style = Paint.Style.STROKE
                cursorP.strokeWidth = 3f
                canvas.drawRoundRect(RectF(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad), 4f, 4f, cursorP)
            }

            // Tubería
            val type = grid[r][c]
            if (type != 0) {
                drawPipe(canvas, type, x + cellSize / 2f, y + cellSize / 2f, cellSize * 0.7f)
            }
        }

        // Marco del grid
        val framePaint = paintOf("#F8F8F8")
        framePaint.style = Paint.Style.STROKE
        framePaint.strokeWidth = 2f
        canvas.drawRect(gridX, gridY, gridX + cellSize * gridSize, gridY + cellSize * gridSize, framePaint)

        // Instrucciones
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("D-pad: mover  ·  A: rotar tubería  ·  B: reiniciar nivel  ·  START: reset timer", w / 2f, h - 10f, smallPaint)

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 50f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Sin camino para el agua", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }
    }

    private fun drawPipe(canvas: Canvas, type: Int, cx: Float, cy: Float, size: Float) {
        val half = size / 2f
        val paint = pipePaint
        val waterPaint = this.waterPaint

        when (type) {
            1 -> { // Horizontal
                canvas.drawRect(cx - half, cy - 4f, cx + half, cy + 4f, paint)
            }
            2 -> { // Vertical
                canvas.drawRect(cx - 4f, cy - half, cx + 4f, cy + half, paint)
            }
            3 -> { // ┌ (arriba-derecha)
                canvas.drawRect(cx - 4f, cy - half, cx + 4f, cy + 4f, paint)
                canvas.drawRect(cx - 4f, cy - 4f, cx + half, cy + 4f, paint)
            }
            4 -> { // ┐ (arriba-izquierda)
                canvas.drawRect(cx - 4f, cy - half, cx + 4f, cy + 4f, paint)
                canvas.drawRect(cx - half, cy - 4f, cx + 4f, cy + 4f, paint)
            }
            5 -> { // └ (abajo-derecha)
                canvas.drawRect(cx - 4f, cy - 4f, cx + 4f, cy + half, paint)
                canvas.drawRect(cx - 4f, cy - 4f, cx + half, cy + 4f, paint)
            }
            6 -> { // ┘ (abajo-izquierda)
                canvas.drawRect(cx - 4f, cy - 4f, cx + 4f, cy + half, paint)
                canvas.drawRect(cx - half, cy - 4f, cx + 4f, cy + 4f, paint)
            }
            7 -> { // Fuente
                val srcPaint = sourcePaint
                canvas.drawCircle(cx, cy, half * 0.6f, srcPaint)
                canvas.drawText("F", cx, cy + 6f, textPaint.apply { textSize = 14f })
            }
            8 -> { // Desagüe
                val drnPaint = drainPaint
                canvas.drawCircle(cx, cy, half * 0.6f, drnPaint)
                canvas.drawText("D", cx, cy + 6f, textPaint.apply { textSize = 14f })
            }
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
