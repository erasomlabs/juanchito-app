package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.random.Random

/**
 * Serpiente Retro: reconstrucción fiel del clásico J2ME/Nokia Snake (Snake II).
 *
 * Mecánica original:
 *  · Serpiente en rejilla que crece al comer manzanas (+10).
 *  · 5 manzanas por nivel: sube la velocidad y aparecen muros de maze.
 *  · Escarabajo dorado de bonus tras subir de nivel (+50, tiempo limitado).
 *  · Muerte contra muros, obstáculos o el propio cuerpo. START reinicia.
 */
class SnakeGame : Game {
    override val name = "Serpiente Retro"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    private val cell = 24f
    private var cols = 0
    private var rows = 0
    private var ox = 0f
    private var oy = 0f

    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    private val snake = ArrayDeque<Pair<Int, Int>>()   // first = cabeza
    private var dir = Direction.RIGHT
    private var nextDir = Direction.RIGHT
    private var moveTimer = 0f
    private var moveInterval = 0.16f

    private var level = 1
    private var applesInLevel = 0
    private val applesPerLevel = 5

    private var food = Pair(0, 0)
    private var bonus: Pair<Int, Int>? = null
    private var bonusTimer = 0f
    private var obstacles = mutableSetOf<Pair<Int, Int>>()

    private var started = false
    private var flash = 0f
    override var isGameOver = false
    override var score = 0
    private var best = 0
    private var newRecord = false

    // ---------- Paints (pre-creados, sin allocs por frame) ----------
    private val grass1 = paintOf("#0A6A0A")
    private val grass2 = paintOf("#0E7A0E")
    private val wallPaint = paintOf("#7C7C7C")
    private val wallDark = paintOf("#4A4A4A")
    private val wallHi = paintOf("#B8B8B8")
    private val snakePaint = paintOf("#00A800")
    private val snakeDark = paintOf("#007000")
    private val scalePaint = paintOf("#30D030")
    private val headPaint = paintOf("#F8D878")
    private val capPaint = paintOf("#D82800")
    private val eyePaint = paintOf("#101010")
    private val applePaint = paintOf("#D82800")
    private val shinePaint = paintOf("#FC9838")
    private val stemPaint = paintOf("#8B4513")
    private val leafPaint = paintOf("#00E000")
    private val beetlePaint = paintOf("#F8B800")
    private val beetleDark = paintOf("#A86800")
    private val antennaPaint = paintOf("#F8B800").apply { strokeWidth = 2f }
    private val tonguePaint = paintOf("#D82800").apply { strokeWidth = 2f }
    private val hudPaint = paintOf("#F8F8F8").apply { textSize = 22f; textAlign = Paint.Align.LEFT }
    private val hudRight = paintOf("#F8F8F8").apply { textSize = 22f; textAlign = Paint.Align.RIGHT }
    private val hudGold = paintOf("#F5C542").apply { textSize = 22f; textAlign = Paint.Align.CENTER }
    private val bigPaint = paintOf("#E63946").apply { textSize = 52f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val overlayPaint = paintOf("#000000").apply { alpha = 165 }
    private val dimBar = paintOf("#080812").apply { alpha = 170 }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = false
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        cols = ((w - 12) / cell).toInt()
        rows = ((h - 70) / cell).toInt()
        ox = (w - cols * cell) / 2f
        oy = 34f + ((h - 70) - rows * cell) / 2f
        reset()
    }

    override fun reset() {
        snake.clear()
        val cx = cols / 2
        val cy = rows / 2
        snake.addLast(Pair(cx, cy))
        snake.addLast(Pair(cx - 1, cy))
        snake.addLast(Pair(cx - 2, cy))
        snake.addLast(Pair(cx - 3, cy))
        dir = Direction.RIGHT
        nextDir = Direction.RIGHT
        moveInterval = 0.16f
        moveTimer = 0f
        level = 1
        applesInLevel = 0
        obstacles.clear()
        bonus = null
        bonusTimer = 0f
        started = false
        flash = 0f
        newRecord = false
        score = 0
        isGameOver = false
        spawnFood()
    }

    private fun speedFor(lv: Int): Float = (0.16f - (lv - 1) * 0.013f).coerceAtLeast(0.065f)

    /** Muros de maze determinísticos (estilo Snake II: más niveles, más muros). */
    private fun buildObstacles(lv: Int) {
        obstacles.clear()
        if (lv >= 3) {
            val r = rows / 2
            for (c in 4 until cols - 4) {
                if (c % 7 in 3..4) continue
                obstacles.add(c to r)
            }
        }
        if (lv >= 5) {
            val r = rows / 4
            for (c in 3 until cols - 3) {
                if (c % 5 in 1..2) continue
                obstacles.add(c to r)
            }
        }
        if (lv >= 7) {
            val r = rows - rows / 4
            for (c in 3 until cols - 3) {
                if (c % 5 in 3..4) continue
                obstacles.add(c to r)
            }
        }
    }

    private fun cellFree(c: Int, r: Int): Boolean =
        !snake.contains(c to r) && !obstacles.contains(c to r) &&
            food != (c to r) && bonus != (c to r)

    private fun spawnFood() {
        var tries = 0
        do {
            food = Pair(Random.nextInt(cols), Random.nextInt(rows))
            tries++
        } while (!cellFree(food.first, food.second) && tries < 400)
    }

    private fun spawnBonus() {
        var tries = 0
        var p: Pair<Int, Int>
        do {
            p = Pair(Random.nextInt(cols), Random.nextInt(rows))
            tries++
        } while (!cellFree(p.first, p.second) && tries < 400)
        bonus = p
        bonusTimer = 8f
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) {
            if (input.start || input.a) reset()
            return
        }
        flash = max(0f, flash - dt * 4f)

        // Bonus expira
        if (bonus != null) {
            bonusTimer -= dt
            if (bonusTimer <= 0f) bonus = null
        }

        // Espera la primera dirección del jugador (la serpiente arranca al mover)
        if (!started) {
            if (input.up && dir != Direction.DOWN) { nextDir = Direction.UP; started = true }
            if (input.down && dir != Direction.UP) { nextDir = Direction.DOWN; started = true }
            if (input.left && dir != Direction.RIGHT) { nextDir = Direction.LEFT; started = true }
            if (input.right && dir != Direction.LEFT) { nextDir = Direction.RIGHT; started = true }
            if (started) moveTimer = 0f
            return
        }

        // Giro (no permite reversa)
        if (input.up && dir != Direction.DOWN) nextDir = Direction.UP
        if (input.down && dir != Direction.UP) nextDir = Direction.DOWN
        if (input.left && dir != Direction.RIGHT) nextDir = Direction.LEFT
        if (input.right && dir != Direction.LEFT) nextDir = Direction.RIGHT

        moveInterval = speedFor(level)
        moveTimer += dt
        if (moveTimer < moveInterval) return
        moveTimer = 0f
        dir = nextDir

        val head = snake.first()
        val nh = when (dir) {
            Direction.UP -> Pair(head.first, head.second - 1)
            Direction.DOWN -> Pair(head.first, head.second + 1)
            Direction.LEFT -> Pair(head.first - 1, head.second)
            Direction.RIGHT -> Pair(head.first + 1, head.second)
        }

        // Muro perimetral, maze o cuerpo
        if (nh.first < 0 || nh.first >= cols || nh.second < 0 || nh.second >= rows ||
            obstacles.contains(nh) || snake.contains(nh)
        ) {
            isGameOver = true
            newRecord = score > best
            if (score > best) best = score
            return
        }

        snake.addFirst(nh)

        var grew = false
        if (nh == food) {
            score += 10
            grew = true
            flash = 1f
            applesInLevel++
            if (applesInLevel >= applesPerLevel) {
                // Nivel nuevo: más velocidad y nuevos muros, aparece el escarabajo
                level++
                applesInLevel = 0
                buildObstacles(level)
                moveInterval = speedFor(level)
                spawnFood()
                spawnBonus()
            } else {
                spawnFood()
            }
        } else if (bonus != null && nh == bonus) {
            score += 50
            grew = true
            flash = 1f
            bonus = null
        }
        if (!grew) snake.removeLast()
    }

    override fun draw(canvas: Canvas) {
        // Césped ajedrezado
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = ox + c * cell
                val y = oy + r * cell
                canvas.drawRect(x, y, x + cell, y + cell, if ((r + c) % 2 == 0) grass1 else grass2)
            }
        }

        // Muro perimetral de piedra con patrón de sillares
        val bw = cols * cell
        val bh = rows * cell
        canvas.drawRect(ox - 6f, oy - 6f, ox + bw + 6f, oy, wallPaint)
        canvas.drawRect(ox - 6f, oy + bh, ox + bw + 6f, oy + bh + 6f, wallPaint)
        canvas.drawRect(ox - 6f, oy - 6f, ox, oy + bh + 6f, wallPaint)
        canvas.drawRect(ox + bw, oy - 6f, ox + bw + 6f, oy + bh + 6f, wallPaint)
        var wx = 0f
        while (wx < bw + 12f) {
            canvas.drawRect(ox - 6f + wx, oy - 3f, ox - 2f + wx, oy, wallDark)
            canvas.drawRect(ox - 6f + wx, oy + bh + 3f, ox - 2f + wx, oy + bh + 6f, wallDark)
            wx += 16f
        }
        var wy = 0f
        while (wy < bh + 12f) {
            canvas.drawRect(ox - 3f, oy - 6f + wy, ox, oy - 2f + wy, wallDark)
            canvas.drawRect(ox + bw + 2f, oy - 6f + wy, ox + bw + 6f, oy - 2f + wy, wallDark)
            wy += 16f
        }

        // Obstáculos del maze: sillares de piedra con brillo superior
        for ((c, r) in obstacles) {
            val x = ox + c * cell
            val y = oy + r * cell
            canvas.drawRect(x + 1f, y + 1f, x + cell - 1f, y + cell - 1f, wallPaint)
            canvas.drawRect(x + 1f, y + 1f, x + cell - 1f, y + cell * 0.3f, wallHi)
            canvas.drawRect(x + cell * 0.45f, y + cell * 0.3f, x + cell - 1f, y + cell * 0.55f, wallDark)
            canvas.drawRect(x + 1f, y + cell * 0.75f, x + cell * 0.55f, y + cell - 1f, wallDark)
        }

        // Manzana
        drawApple(canvas, ox + food.first * cell, oy + food.second * cell)

        // Escarabajo bonus dorado
        bonus?.let { drawBeetle(canvas, ox + it.first * cell, oy + it.second * cell) }

        // Serpiente
        snake.forEachIndexed { i, seg ->
            val x = ox + seg.first * cell
            val y = oy + seg.second * cell
            if (i == 0) drawHead(canvas, x, y)
            else {
                canvas.drawRoundRect(
                    RectF(x + 2f, y + 2f, x + cell - 2f, y + cell - 2f),
                    5f, 5f, if (i % 2 == 0) snakePaint else snakeDark
                )
                canvas.drawCircle(x + cell / 2f, y + cell / 2f, 2.5f, scalePaint)
            }
        }

        drawHud(canvas)

        if (isGameOver) {
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlayPaint)
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 20f, bigPaint)
            hudGold.textSize = 26f
            canvas.drawText(
                if (newRecord) "¡NUEVO RÉCORD!  $score PTS" else "PUNTOS: $score   MEJOR: $best",
                w / 2f, h / 2f + 22f, hudGold
            )
            hudGold.textSize = 20f
            canvas.drawText("NIVEL ALCANZADO: $level", w / 2f, h / 2f + 50f, hudGold)
            if ((System.nanoTime() / 500_000_000L) % 2L == 0L) {
                hudPaint.textSize = 22f
                canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 92f, hudPaint)
            }
        }
    }

    private fun drawApple(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x + cell / 2f, y + cell / 2f + 1f, cell * 0.4f, applePaint)
        canvas.drawCircle(x + cell * 0.35f, y + cell * 0.38f, cell * 0.12f, shinePaint)
        canvas.drawRect(x + cell / 2f - 1.5f, y + 2f, x + cell / 2f + 1.5f, y + cell * 0.4f, stemPaint)
        canvas.drawRect(x + cell / 2f + 2f, y + 2f, x + cell * 0.8f, y + cell * 0.25f, leafPaint)
    }

    private fun drawBeetle(canvas: Canvas, x: Float, y: Float) {
        val cx = x + cell / 2f
        val cy = y + cell / 2f
        // patas
        canvas.drawRect(x + 3f, cy - 1f, x + 7f, cy + 1f, beetleDark)
        canvas.drawRect(x + cell - 7f, cy - 1f, x + cell - 3f, cy + 1f, beetleDark)
        // cuerpo segmentado
        canvas.drawRoundRect(RectF(x + 6f, y + 6f, x + cell - 6f, y + cell - 4f), 8f, 8f, beetlePaint)
        canvas.drawRect(x + 8f, cy - 1.5f, x + cell - 8f, cy + 1.5f, beetleDark)
        // cabeza y antenas
        canvas.drawCircle(cx, y + 7f, 3.5f, beetleDark)
        canvas.drawLine(cx - 3f, y + 6f, cx - 7f, y + 2f, antennaPaint)
        canvas.drawLine(cx + 3f, y + 6f, cx + 7f, y + 2f, antennaPaint)
        // parpadeo cuando está por expirar
        if (bonusTimer < 3f && (System.nanoTime() / 150_000_000L) % 2L == 0L) {
            canvas.drawRect(x + 6f, y + 6f, x + cell - 6f, y + cell - 4f, shinePaint)
        }
    }

    private fun drawHead(canvas: Canvas, x: Float, y: Float) {
        canvas.drawRoundRect(RectF(x + 1f, y + 3f, x + cell - 1f, y + cell - 1f), 7f, 7f, headPaint)
        canvas.drawRect(x + 1f, y + 3f, x + cell - 1f, y + cell * 0.35f, capPaint)
        canvas.drawRect(x + 1f, y + cell * 0.35f, x + cell - 1f, y + cell * 0.42f, capPaint)
        val cxp = x + cell / 2f
        val cyp = y + cell * 0.6f
        canvas.drawCircle(cxp + cell * 0.22f, cyp, 2.5f, eyePaint)
        canvas.drawCircle(cxp - cell * 0.22f, cyp, 2.5f, eyePaint)
        when (dir) {
            Direction.RIGHT -> canvas.drawLine(x + cell - 1f, cyp, x + cell + 5f, cyp, tonguePaint)
            Direction.LEFT -> canvas.drawLine(x + 1f, cyp, x - 5f, cyp, tonguePaint)
            Direction.UP -> canvas.drawLine(cxp, y + 3f, cxp, y - 5f, tonguePaint)
            Direction.DOWN -> canvas.drawLine(cxp, y + cell - 1f, cxp, y + cell + 5f, tonguePaint)
        }
    }

    private fun drawHud(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w.toFloat(), 32f, dimBar)
        hudPaint.textSize = 20f
        hudRight.textSize = 20f
        canvas.drawText("SCORE $score", 10f, 23f, hudPaint)
        canvas.drawText("HI $best  ·  NIVEL $level", w - 10f, 23f, hudRight)
        // Progreso de manzanas del nivel
        val apples = "●".repeat(applesInLevel) + "○".repeat(applesPerLevel - applesInLevel)
        hudGold.textSize = 16f
        canvas.drawText("MANZANAS $apples", w / 2f, 22f, hudGold)
        // Barra de tiempo del escarabajo bonus
        bonus?.let {
            val frac = (bonusTimer / 8f).coerceIn(0f, 1f)
            canvas.drawRect(ox, oy - 12f, ox + cols * cell * frac, oy - 6f, beetlePaint)
        }
        // Flash al comer
        if (flash > 0f) {
            val fl = Paint().apply { setColor(Color.argb((40 * flash).toInt(), 255, 255, 200)); isAntiAlias = false }
            canvas.drawRect(ox - 6f, oy - 6f, ox + cols * cell + 6f, oy + rows * cell + 6f, fl)
        }
        // Arranque en pausa
        if (!started && !isGameOver) {
            hudGold.textSize = 24f
            canvas.drawText("SERPIENTE RETRO", w / 2f, oy + rows * cell / 2f - 8f, hudGold)
            hudPaint.textSize = 18f
            canvas.drawText("PULSA UNA DIRECCIÓN PARA MOVER", w / 2f, oy + rows * cell / 2f + 20f, hudPaint)
        }
    }
}
