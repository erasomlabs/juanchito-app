package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Draw: Picross/Nonogram (paint by numbers) - juego de revelar imágenes.
 *
 * Mecánica original (Picross/Nonogram):
 *  · Cuadrícula donde cada fila/columna tiene números que indican
 *    celdas consecutivas a rellenar.
 *  · El jugador marca celdas para revelar una imagen oculta.
 *  · Múltiples niveles de dificultad (5x5, 10x10, 15x15).
 *  · Timer por nivel. Puntuación por velocidad y precisión.
 *  · Errores restan tiempo. Pistas disponibles (limitadas).
 */
class PixelDrawGame : Game {
    override val name = "Pixel Draw"
    override val genre = "Puzzle"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { SELECTING, PLAYING, COMPLETE, GAME_OVER }
    private var state = State.SELECTING

    // Nivel
    private var gridSize = 5
    private var level = 1
    private val maxLevel = 30

    // Grid
    private val grid = Array(15) { IntArray(15) }  // 0=empty, 1=filled, 2=marked (X)
    private val solution = Array(15) { IntArray(15) }
    private val rowHints = Array(15) { mutableListOf<Int>() }
    private val colHints = Array(15) { mutableListOf<Int>() }

    // Cursor
    private var cursorR = 0
    private var cursorC = 0

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var errors = 0
    private var maxErrors = 3

    // Timer
    private var timeLeft = 0f
    private var timeLimit = 300f  // 5 minutos por nivel

    // Pistas
    private var hintsLeft = 3

    // Animación
    private var revealAnim = 0f
    private var cursorBlink = 0f

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val gridBg = paintOf("#2A2A3E")
    private val cellEmpty = paintOf("#3A3A5E")
    private val cellFilled = paintOf("#F5C542")
    private val cellMarked = paintOf("#FF5252")
    private val cursorPaint = paintOf("#00E676")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        state = State.SELECTING
        level = 1
        gridSize = 5
        scoreVal = 0
        errors = 0
        hintsLeft = 3
        cursorR = 0
        cursorC = 0
        clearGrid()
        generatePuzzle()
    }

    private fun clearGrid() {
        for (r in 0 until 15) for (c in 0 until 15) {
            grid[r][c] = 0
            solution[r][c] = 0
        }
    }

    private fun generatePuzzle() {
        // Generar una imagen aleatoria en el grid
        clearGrid()
        val size = gridSize
        // Crear una imagen simétrica simple (patrón)
        for (r in 0 until size) for (c in 0 until size) {
            // Patrón basado en nivel: formas geométricas
            val fill = when (level % 5) {
                0 -> (r + c) % 3 == 0 || r == c || r + c == size - 1
                1 -> r % 2 == 0 || c % 2 == 0
                2 -> (r * c) % 4 == 0
                3 -> r < size / 2 || c < size / 2
                else -> Random.nextFloat() < 0.4f
            }
            solution[r][c] = if (fill) 1 else 0
        }
        // Calcular hints
        for (r in 0 until size) {
            rowHints[r].clear()
            var count = 0
            for (c in 0 until size) {
                if (solution[r][c] == 1) count++
                else if (count > 0) { rowHints[r].add(count); count = 0 }
            }
            if (count > 0) rowHints[r].add(count)
            if (rowHints[r].isEmpty()) rowHints[r].add(0)
        }
        for (c in 0 until size) {
            colHints[c].clear()
            var count = 0
            for (r in 0 until size) {
                if (solution[r][c] == 1) count++
                else if (count > 0) { colHints[c].add(count); count = 0 }
            }
            if (count > 0) colHints[c].add(count)
            if (colHints[c].isEmpty()) colHints[c].add(0)
        }
        timeLeft = timeLimit
        errors = 0
        hintsLeft = 3
        revealAnim = 0f
    }

    private fun checkComplete(): Boolean {
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            if (solution[r][c] == 1 && grid[r][c] != 1) return false
            if (solution[r][c] == 0 && grid[r][c] == 1) return false
        }
        return true
    }

    override fun update(dt: Float, input: GameInput) {
        cursorBlink += dt
        if (revealAnim < 1f) revealAnim = (revealAnim + dt * 2f).coerceAtMost(1f)

        when (state) {
            State.SELECTING -> {
                if (input.up) { gridSize = (gridSize - 5).coerceAtLeast(5); if (gridSize < 5) gridSize = 15; input.up = false }
                if (input.down) { gridSize = (gridSize + 5).coerceAtMost(15); if (gridSize > 15) gridSize = 5; input.down = false }
                if (input.left) { level = (level - 1).coerceAtLeast(1); input.left = false }
                if (input.right) { level = (level + 1).coerceAtMost(maxLevel); input.right = false }
                if (input.a || input.start) {
                    generatePuzzle()
                    state = State.PLAYING
                    input.a = false; input.start = false
                }
            }
            State.PLAYING -> {
                timeLeft -= dt
                if (timeLeft <= 0f) {
                    timeLeft = 0f
                    state = State.GAME_OVER
                    return
                }

                // Navegación del cursor
                if (input.up) { cursorR = (cursorR - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { cursorR = (cursorR + 1).coerceAtMost(gridSize - 1); input.down = false }
                if (input.left) { cursorC = (cursorC - 1).coerceAtLeast(0); input.left = false }
                if (input.right) { cursorC = (cursorC + 1).coerceAtMost(gridSize - 1); input.right = false }

                // Marcar celda (A = rellenar, B = marcar X)
                if (input.a) {
                    if (grid[cursorR][cursorC] == 0) {
                        if (solution[cursorR][cursorC] == 1) {
                            grid[cursorR][cursorC] = 1
                            scoreVal += 10
                            if (scoreVal > best) best = scoreVal
                        } else {
                            errors++
                            if (errors >= maxErrors) {
                                state = State.GAME_OVER
                            }
                        }
                    }
                    input.a = false
                }
                if (input.b) {
                    if (grid[cursorR][cursorC] == 0) {
                        grid[cursorR][cursorC] = 2  // marcar X
                    } else if (grid[cursorR][cursorC] == 2) {
                        grid[cursorR][cursorC] = 0  // desmarcar
                    }
                    input.b = false
                }

                // Pista (START)
                if (input.start) {
                    if (hintsLeft > 0) {
                        // Revelar una celda aleatoria no revelada
                        val empty = mutableListOf<Pair<Int, Int>>()
                        for (r in 0 until gridSize) for (c in 0 until gridSize) {
                            if (solution[r][c] == 1 && grid[r][c] != 1) empty.add(r to c)
                        }
                        if (empty.isNotEmpty()) {
                            val (r, c) = empty.random()
                            grid[r][c] = 1
                            hintsLeft--
                        }
                    }
                    input.start = false
                }

                // Verificar completado
                if (checkComplete()) {
                    state = State.COMPLETE
                    val timeBonus = (timeLeft * 2).toInt()
                    scoreVal += 100 + timeBonus
                    if (scoreVal > best) best = scoreVal
                }
            }
            State.COMPLETE -> {
                if (input.start || input.a) {
                    level++
                    if (level > maxLevel) {
                        state = State.GAME_OVER
                    } else {
                        if (level % 5 == 0) gridSize = (gridSize + 5).coerceAtMost(15)
                        generatePuzzle()
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

        when (state) {
            State.SELECTING -> drawSelecting(canvas)
            State.PLAYING -> drawPlaying(canvas)
            State.COMPLETE -> drawComplete(canvas)
            State.GAME_OVER -> drawGameOver(canvas)
        }
    }

    private fun drawSelecting(canvas: Canvas) {
        titlePaint.textSize = 42f
        canvas.drawText("PIXEL DRAW", w / 2f, 60f, titlePaint)
        smallPaint.textSize = 18f
        canvas.drawText("Picross: revela la imagen", w / 2f, 90f, smallPaint)

        // Tamaño de grid
        titlePaint.textSize = 24f
        canvas.drawText("Tamaño: ${gridSize}x${gridSize}", w / 2f, 150f, titlePaint)
        smallPaint.textSize = 16f
        canvas.drawText("(▲▼ para cambiar)", w / 2f, 175f, smallPaint)

        // Nivel
        canvas.drawText("Nivel: $level / $maxLevel", w / 2f, 210f, smallPaint)

        // Instrucciones
        smallPaint.textSize = 14f
        canvas.drawText("A: rellenar  ·  B: marcar X  ·  START: pista", w / 2f, 260f, smallPaint)
        canvas.drawText("Rellena las celdas correctas para revelar la imagen", w / 2f, 285f, smallPaint)

        greenPaint.textSize = 20f
        canvas.drawText("PULSA A O START PARA COMENZAR", w / 2f, h - 30f, greenPaint)
    }

    private fun drawPlaying(canvas: Canvas) {
        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        // Timer
        val timeColor = if (timeLeft < 30f) "#FF5252" else "#F5C542"
        titlePaint.textSize = 20f
        titlePaint.color = Color.parseColor(timeColor)
        canvas.drawText("TIEMPO: ${timeLeft.toInt()}s", w / 2f - 40f, 50f, titlePaint)
        titlePaint.color = Color.parseColor("#F5C542")

        // Errores
        smallPaint.textSize = 16f
        canvas.drawText("Errores: $errors/$maxErrors  ·  Pistas: $hintsLeft", w / 2f, 75f, smallPaint)

        // Grid
        val cellSize = (minOf(w, h) * 0.7f / gridSize).coerceAtMost(40f)
        val gridW = cellSize * gridSize
        val gridH = cellSize * gridSize
        val gridX = (w - gridW) / 2f
        val gridY = (h - gridH) / 2f + 20f

        // Hints de filas (izquierda)
        smallPaint.textSize = (cellSize * 0.3f).coerceAtLeast(8f)
        for (r in 0 until gridSize) {
            val hintStr = rowHints[r].joinToString(" ")
            smallPaint.color = Color.parseColor("#F5C542")
            canvas.drawText(hintStr, gridX - 8f, gridY + r * cellSize + cellSize / 2f + smallPaint.textSize / 3f, smallPaint)
        }

        // Hints de columnas (arriba)
        for (c in 0 until gridSize) {
            val hintStr = colHints[c].joinToString("\n")
            smallPaint.color = Color.parseColor("#F5C542")
            for ((i, h) in colHints[c].withIndex()) {
                canvas.drawText(h.toString(), gridX + c * cellSize + cellSize / 2f, gridY - 8f - (colHints[c].size - 1 - i) * smallPaint.textSize, smallPaint)
            }
        }

        // Celdas
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            val x = gridX + c * cellSize
            val y = gridY + r * cellSize
            val pad = 2f

            // Fondo de celda
            canvas.drawRect(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad, cellEmpty)

            // Cursor
            if (r == cursorR && c == cursorC) {
                val cursorAlpha = if ((cursorBlink * 2).toInt() % 2 == 0) 200 else 80
                val cursorP = Paint().apply { color = Color.parseColor("#00E676"); alpha = cursorAlpha; isAntiAlias = false }
                canvas.drawRect(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad, cursorP)
            }

            // Contenido
            when (grid[r][c]) {
                1 -> {
                    // Rellenado
                    val fillPaint = paintOf("#F5C542")
                    canvas.drawRect(x + pad + 2f, y + pad + 2f, x + cellSize - pad - 2f, y + cellSize - pad - 2f, fillPaint)
                }
                2 -> {
                    // Marcado con X
                    val xPaint = paintOf("#FF5252")
                    xPaint.strokeWidth = 2f
                    canvas.drawLine(x + pad + 4f, y + pad + 4f, x + cellSize - pad - 4f, y + cellSize - pad - 4f, xPaint)
                    canvas.drawLine(x + cellSize - pad - 4f, y + pad + 4f, x + pad + 4f, y + cellSize - pad - 4f, xPaint)
                }
            }
        }

        // Marco del grid
        val framePaint = paintOf("#F8F8F8")
        framePaint.style = Paint.Style.STROKE
        framePaint.strokeWidth = 2f
        canvas.drawRect(gridX, gridY, gridX + gridW, gridY + gridH, framePaint)

        // Instrucciones
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("D-pad: mover  ·  A: rellenar  ·  B: marcar X  ·  START: pista", w / 2f, h - 10f, smallPaint)
    }

    private fun drawComplete(canvas: Canvas) {
        val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
        titlePaint.textSize = 42f
        canvas.drawText("¡IMAGEN REVELADA!", w / 2f, h / 2f - 40f, titlePaint)
        smallPaint.textSize = 20f
        canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f, smallPaint)
        canvas.drawText("Errores: $errors/$maxErrors", w / 2f, h / 2f + 30f, smallPaint)
        canvas.drawText("Pistas usadas: ${3 - hintsLeft}", w / 2f, h / 2f + 60f, smallPaint)
        if (scoreVal >= best && scoreVal > 0) {
            titlePaint.textSize = 22f
            canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 90f, titlePaint)
        }
        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 130f, greenPaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
        redPaint.textSize = 48f
        canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
        smallPaint.textSize = 22f
        canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
        canvas.drawText("Errores: $errors/$maxErrors", w / 2f, h / 2f + 40f, smallPaint)
        if (scoreVal >= best && scoreVal > 0) {
            titlePaint.textSize = 22f
            canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
        }
        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
