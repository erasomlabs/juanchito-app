package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Puzzles: reconstrucción fiel de Puzzle Quest Warlords (THQ, 2008).
 *
 * Mecánica original:
 *  · Match-3 puzzle RPG: emparejar runas del mismo color para atacar.
 *  · Hechizos y aventura. 8 personajes, 4 clases.
 *  · Combos, daño por hechizos, campañas con historia.
 */
class PixelPuzzlesGame : Game {
    override val name = "Pixel Puzzles"
    override val genre = "Puzzle"

    private var w = 0
    private var h = 0

    // Grid de runas (8x8)
    private val gridSize = 8
    private val grid = Array(gridSize) { IntArray(gridSize) }
    private val gemColors = listOf("#5C94FC", "#F8D878", "#D82800", "#00A800", "#8814A8", "#FC9838")
    private val gemNames = listOf("AZUL", "AMARILLO", "ROJO", "VERDE", "MORADO", "NARANJA")

    // Cursor
    private var cursorR = 0
    private var cursorC = 0
    private var selected: Pair<Int, Int>? = null

    // Estado
    private enum class State { PLAYING, VICTORY, GAME_OVER }
    private var state = State.PLAYING

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var combo = 0
    private var maxCombo = 0

    // Vida (jugador y enemigo)
    private var playerHp = 100
    private var enemyHp = 100
    private val maxHp = 100
    private var enemyName = "GOBLIN"
    private var enemyLevel = 1

    // Hechizos
    private var mana = 0
    private val maxMana = 50
    private val spells = listOf(
        "FUEGO" to 15, "HIELO" to 12, "RAYO" to 20, "CURA" to 10
    )
    private var selectedSpell = 0

    // Animación
    private var matchFlash = 0f
    private var swapAnim = 0f
    private var swapFrom: Pair<Int, Int>? = null
    private var swapTo: Pair<Int, Int>? = null

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val gridBg = paintOf("#2A2A3E")
    private val cellBg = paintOf("#3A3A5E")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val bluePaint = paintOf("#5C94FC").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        state = State.PLAYING
        scoreVal = 0
        combo = 0
        maxCombo = 0
        playerHp = maxHp
        enemyHp = maxHp
        mana = 0
        enemyLevel = 1
        enemyName = "GOBLIN"
        selected = null
        cursorR = 0
        cursorC = 0
        matchFlash = 0f
        fillGrid()
    }

    private fun fillGrid() {
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            grid[r][c] = Random.nextInt(gemColors.size)
        }
        // Eliminar matches iniciales
        removeMatches()
    }

    private fun removeMatches(): Int {
        var totalRemoved = 0
        val toRemove = mutableSetOf<Pair<Int, Int>>()

        // Buscar matches horizontales
        for (r in 0 until gridSize) {
            var c = 0
            while (c < gridSize - 2) {
                if (grid[r][c] >= 0 && grid[r][c] == grid[r][c + 1] && grid[r][c] == grid[r][c + 2]) {
                    var end = c + 2
                    while (end + 1 < gridSize && grid[r][end + 1] == grid[r][c]) end++
                    for (cc in c..end) toRemove.add(r to cc)
                    c = end + 1
                } else c++
            }
        }

        // Buscar matches verticales
        for (c in 0 until gridSize) {
            var r = 0
            while (r < gridSize - 2) {
                if (grid[r][c] >= 0 && grid[r][c] == grid[r + 1][c] && grid[r][c] == grid[r + 2][c]) {
                    var end = r + 2
                    while (end + 1 < gridSize && grid[end + 1][c] == grid[r][c]) end++
                    for (rr in r..end) toRemove.add(rr to c)
                    r = end + 1
                } else r++
            }
        }

        // Eliminar y contar
        for ((r, c) in toRemove) {
            val gemType = grid[r][c]
            grid[r][c] = -1  // marcar para eliminar
            totalRemoved++
            // Dar mana según el color
            mana = (mana + 1).coerceAtMost(maxMana)
            // Daño al enemigo
            enemyHp = (enemyHp - 2).coerceAtLeast(0)
        }

        // Gravedad: las gemas caen
        for (c in 0 until gridSize) {
            var writeRow = gridSize - 1
            for (r in gridSize - 1 downTo 0) {
                if (grid[r][c] >= 0) {
                    grid[writeRow][c] = grid[r][c]
                    if (writeRow != r) grid[r][c] = -1
                    writeRow--
                }
            }
            // Rellenar espacios vacíos
            for (r in 0..writeRow) {
                grid[r][c] = Random.nextInt(gemColors.size)
            }
        }

        // Combo
        if (totalRemoved > 0) {
            combo++
            if (combo > maxCombo) maxCombo = combo
            val pts = totalRemoved * 10 * combo
            scoreVal += pts
            if (scoreVal > best) best = scoreVal
            matchFlash = 1f
        } else {
            combo = 0
        }

        // Verificar victoria/derrota
        if (enemyHp <= 0) {
            state = State.VICTORY
            scoreVal += 200
            if (scoreVal > best) best = scoreVal
        }
        if (playerHp <= 0) {
            state = State.GAME_OVER
        }

        return totalRemoved
    }

    private fun swap(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        if (r1 !in 0 until gridSize || c1 !in 0 until gridSize || r2 !in 0 until gridSize || c2 !in 0 until gridSize) return false
        if (grid[r1][c1] < 0 || grid[r2][c2] < 0) return false

        // Intercambiar
        val temp = grid[r1][c1]
        grid[r1][c1] = grid[r2][c2]
        grid[r2][c2] = temp

        // Verificar si hay match
        val hadMatch = hasMatch(r1, c1) || hasMatch(r2, c2)
        if (!hadMatch) {
            // Revertir
            val temp2 = grid[r1][c1]
            grid[r1][c1] = grid[r2][c2]
            grid[r2][c2] = temp2
            return false
        }
        return true
    }

    private fun hasMatch(r: Int, c: Int): Boolean {
        if (grid[r][c] < 0) return false
        val color = grid[r][c]
        // Horizontal
        var count = 1
        var cc = c - 1
        while (cc >= 0 && grid[r][cc] == color) { count++; cc-- }
        cc = c + 1
        while (cc < gridSize && grid[r][cc] == color) { count++; cc++ }
        if (count >= 3) return true
        // Vertical
        count = 1
        var rr = r - 1
        while (rr >= 0 && grid[rr][c] == color) { count++; rr-- }
        rr = r + 1
        while (rr < gridSize && grid[rr][c] == color) { count++; rr++ }
        return count >= 3
    }

    override fun update(dt: Float, input: GameInput) {
        matchFlash = (matchFlash - dt * 2f).coerceAtLeast(0f)

        when (state) {
            State.PLAYING -> {
                if (input.up) { cursorR = (cursorR - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { cursorR = (cursorR + 1).coerceAtMost(gridSize - 1); input.down = false }
                if (input.left) { cursorC = (cursorC - 1).coerceAtLeast(0); input.left = false }
                if (input.right) { cursorC = (cursorC + 1).coerceAtMost(gridSize - 1); input.right = false }

                if (input.a) {
                    if (selected == null) {
                        selected = cursorR to cursorC
                    } else {
                        val (sr, sc) = selected!!
                        // Solo intercambiar si son adyacentes
                        if (Math.abs(sr - cursorR) + Math.abs(sc - cursorC) == 1) {
                            if (swap(sr, sc, cursorR, cursorC)) {
                                // Eliminar matches en cadena
                                var removed = removeMatches()
                                while (removed > 0) {
                                    removed = removeMatches()
                                }
                            }
                        }
                        selected = null
                    }
                    input.a = false
                }

                if (input.b) {
                    selected = null
                    input.b = false
                }

                // Hechizos (START)
                if (input.start) {
                    if (mana >= spells[selectedSpell].second) {
                        mana -= spells[selectedSpell].second
                        when (selectedSpell) {
                            0 -> enemyHp = (enemyHp - 20).coerceAtLeast(0)  // Fuego
                            1 -> enemyHp = (enemyHp - 15).coerceAtLeast(0)  // Hielo
                            2 -> enemyHp = (enemyHp - 30).coerceAtLeast(0)  // Rayo
                            3 -> playerHp = (playerHp + 20).coerceAtMost(maxHp)  // Cura
                        }
                        if (enemyHp <= 0) {
                            state = State.VICTORY
                            scoreVal += 300
                            if (scoreVal > best) best = scoreVal
                        }
                    }
                    input.start = false
                }
            }
            State.VICTORY -> {
                if (input.start || input.a) {
                    enemyLevel++
                    enemyHp = maxHp + enemyLevel * 10
                    enemyName = when (enemyLevel) {
                        1 -> "GOBLIN"
                        2 -> "ORCO"
                        3 -> "TROLL"
                        4 -> "DRAGON"
                        else -> "JEFE FINAL"
                    }
                    playerHp = maxHp
                    mana = 0
                    fillGrid()
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

        // HUD
        hudPaint.textSize = 16f
        canvas.drawText("SCORE: $scoreVal", 10f, 20f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 20f, hudPaint)
        canvas.drawText("MANA: $mana/$maxMana", 10f, 40f, hudPaint)

        // Barras de vida
        val barW = w * 0.4f
        val barH = 12f
        // Jugador
        canvas.drawText("JUGADOR", 10f, 60f, smallPaint.apply { textAlign = Paint.Align.LEFT; textSize = 12f })
        val playerBarBg = paintOf("#2A2A3E")
        canvas.drawRect(10f, 65f, 10f + barW, 65f + barH, playerBarBg)
        val playerBarFill = paintOf("#00E676")
        canvas.drawRect(10f, 65f, 10f + barW * (playerHp.toFloat() / maxHp), 65f + barH, playerBarFill)
        // Enemigo
        canvas.drawText("$enemyName", w - 10f - 80f, 60f, smallPaint.apply { textAlign = Paint.Align.RIGHT; textSize = 12f })
        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawRect(w - 10f - barW, 65f, w - 10f, 65f + barH, playerBarBg)
        val enemyBarFill = paintOf("#FF5252")
        canvas.drawRect(w - 10f - barW, 65f, w - 10f - barW + barW * (enemyHp.toFloat() / maxHp), 65f + barH, enemyBarFill)

        // Hechizo seleccionado
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#F5C542")
        canvas.drawText("Hechizo: ${spells[selectedSpell].first} (${spells[selectedSpell].second} mana)  ·  START: lanzar", w / 2f, 82f, smallPaint)

        // Grid
        val cellSize = (minOf(w, h) * 0.6f / gridSize).coerceAtMost(45f)
        val gridW = cellSize * gridSize
        val gridH = cellSize * gridSize
        val gridX = (w - gridW) / 2f
        val gridY = (h - gridH) / 2f + 20f

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

            // Selección
            if (selected == (r to c)) {
                val selP = paintOf("#FFFFFF")
                selP.style = Paint.Style.STROKE
                selP.strokeWidth = 2f
                selP.alpha = 150
                canvas.drawRoundRect(RectF(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad), 4f, 4f, selP)
            }

            // Gema
            val gemType = grid[r][c]
            if (gemType >= 0 && gemType < gemColors.size) {
                val gemPaint = paintOf(gemColors[gemType])
                canvas.drawCircle(x + cellSize / 2f, y + cellSize / 2f, (cellSize - pad * 2) / 2f - 2f, gemPaint)
                // Brillo
                val shine = paintOf("#FFFFFF")
                shine.alpha = 120
                canvas.drawCircle(x + cellSize / 2f - 4f, y + cellSize / 2f - 4f, (cellSize - pad * 2) / 6f, shine)
            }
        }

        // Marco
        val framePaint = paintOf("#F8F8F8")
        framePaint.style = Paint.Style.STROKE
        framePaint.strokeWidth = 2f
        canvas.drawRect(gridX, gridY, gridX + gridW, gridY + gridH, framePaint)

        // Combo
        if (combo > 1) {
            smallPaint.textSize = 18f
            smallPaint.color = Color.parseColor("#F5C542")
            canvas.drawText("COMBO x$combo", w / 2f, gridY - 15f, smallPaint)
        }

        // Flash de match
        if (matchFlash > 0f) {
            val alpha = (matchFlash * 100).toInt().coerceIn(0, 100)
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawRect(gridX, gridY, gridX + gridW, gridY + gridH, flashPaint)
        }

        // Instrucciones
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("D-pad: mover cursor  ·  A: seleccionar/intercambiar  ·  B: cancelar  ·  START: hechizo", w / 2f, h - 10f, smallPaint)

        // Overlay de victoria
        if (state == State.VICTORY) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡VICTORIA!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Derrotaste a $enemyName", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Combo máximo: $maxCombo", w / 2f, h / 2f + 70f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Derrotado por $enemyName", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
