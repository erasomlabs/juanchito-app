package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Serpiente3D: reconstrucción fiel de Snake III (Nokia, 2007).
 *
 * Mecánica original (Snake 3D):
 *  · Serpiente en un tablero 3D isométrico.
 *  · Come manzanas, crece, evita obstáculos.
 *  · Múltiples arenas con diferentes layouts.
 *  · Power-ups: velocidad, invencibilidad, puntos extra.
 *  · High score.
 */
class Serpiente3DGame : Game {
    override val name = "Serpiente 3D"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Grid 3D (isométrico)
    private val gridSize = 12
    private val board = Array(gridSize) { IntArray(gridSize) }  // 0=empty, 1=snake, 2=food, 3=obstacle, 4=powerup

    // Serpiente
    private val snake = mutableListOf<Pair<Int, Int>>()
    private var dir = 0  // 0=right, 1=down, 2=left, 3=up
    private var nextDir = 0
    private var moveTimer = 0f
    private var moveInterval = 0.2f

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var level = 1
    private var maxLevel = 8
    private var applesEaten = 0
    private var applesPerLevel = 10

    // Power-ups
    private var speedBoost = false
    private var speedBoostTimer = 0f
    private var invincible = false
    private var invincibleTimer = 0f

    // Animación
    private var flashTimer = 0f
    private var bgPhase = 0f

    // Isométrico
    private val tileW = 40f
    private val tileH = 20f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val gridPaint = paintOf("#2A2A4E")
    private val snakePaint = paintOf("#00A800")
    private val snakeHead = paintOf("#F8D878")
    private val snakeDark = paintOf("#007000")
    private val foodPaint = paintOf("#D82800")
    private val foodShine = paintOf("#FC9838")
    private val obstaclePaint = paintOf("#7C7C7C")
    private val powerupPaint = paintOf("#00E676")
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
        reset()
    }

    override fun reset() {
        snake.clear()
        val cx = gridSize / 2
        val cy = gridSize / 2
        snake.add(Pair(cx, cy))
        snake.add(Pair(cx - 1, cy))
        snake.add(Pair(cx - 2, cy))
        dir = 0
        nextDir = 0
        moveInterval = 0.2f
        moveTimer = 0f
        scoreVal = 0
        level = 1
        applesEaten = 0
        applesPerLevel = 10
        speedBoost = false
        speedBoostTimer = 0f
        invincible = false
        invincibleTimer = 0f
        state = State.PLAYING
        flashTimer = 0f
        bgPhase = 0f
        generateLevel()
    }

    private fun generateLevel() {
        // Limpiar board
        for (r in 0 until gridSize) for (c in 0 until gridSize) board[r][c] = 0
        // Obstáculos según nivel
        val numObstacles = level * 2
        for (i in 0 until numObstacles) {
            var r: Int; var c: Int
            do {
                r = Random.nextInt(gridSize)
                c = Random.nextInt(gridSize)
            } while (board[r][c] != 0 || snake.contains(r to c))
            board[r][c] = 3
        }
        // Power-up ocasional
        if (level >= 3) {
            var r: Int; var c: Int
            do {
                r = Random.nextInt(gridSize)
                c = Random.nextInt(gridSize)
            } while (board[r][c] != 0 || snake.contains(r to c))
            board[r][c] = 4
        }
        spawnFood()
    }

    private fun spawnFood() {
        var r: Int; var c: Int
        var tries = 0
        do {
            r = Random.nextInt(gridSize)
            c = Random.nextInt(gridSize)
            tries++
        } while ((board[r][c] != 0 || snake.contains(r to c)) && tries < 100)
        if (tries < 100) {
            board[r][c] = 2
        }
    }

    // Isométrico: convertir grid (r,c) a pantalla (x,y)
    private fun isoX(r: Int, c: Int): Float = w / 2f + (c - r) * tileW / 2f
    private fun isoY(r: Int, c: Int): Float = h / 3f + (c + r) * tileH / 2f

    override fun update(dt: Float, input: GameInput) {
        bgPhase += dt
        if (flashTimer > 0f) flashTimer -= dt
        if (speedBoost) { speedBoostTimer -= dt; if (speedBoostTimer <= 0f) speedBoost = false }
        if (invincible) { invincibleTimer -= dt; if (invincibleTimer <= 0f) invincible = false }

        if (state != State.PLAYING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.LEVEL_COMPLETE) {
                    level++
                    if (level > maxLevel) state = State.GAME_OVER
                    else {
                        applesEaten = 0
                        applesPerLevel = 10 + level * 2
                        generateLevel()
                        state = State.PLAYING
                    }
                }
            }
            return
        }

        // Giro
        if (input.up && dir != 1) nextDir = 3
        if (input.down && dir != 3) nextDir = 1
        if (input.left && dir != 0) nextDir = 2
        if (input.right && dir != 2) nextDir = 0

        val interval = if (speedBoost) moveInterval * 0.5f else moveInterval
        moveTimer += dt
        if (moveTimer < interval) return
        moveTimer = 0f
        dir = nextDir

        val head = snake.first()
        val (hr, hc) = head
        val newHead = when (dir) {
            0 -> Pair(hr, hc + 1)  // right
            1 -> Pair(hr + 1, hc)  // down
            2 -> Pair(hr, hc - 1)  // left
            3 -> Pair(hr - 1, hc)  // up
            else -> head
        }

        // Colisión con paredes
        if (newHead.first < 0 || newHead.first >= gridSize || newHead.second < 0 || newHead.second >= gridSize) {
            if (!invincible) { state = State.GAME_OVER; if (scoreVal > best) best = scoreVal; return }
        }

        // Colisión con obstáculos
        if (board[newHead.first][newHead.second] == 3) {
            if (!invincible) { state = State.GAME_OVER; if (scoreVal > best) best = scoreVal; return }
        }

        // Colisión con cuerpo
        if (snake.contains(newHead) && !invincible) {
            state = State.GAME_OVER
            if (scoreVal > best) best = scoreVal
            return
        }

        snake.add(0, newHead)

        // Comer comida
        if (board[newHead.first][newHead.second] == 2) {
            scoreVal += 10
            if (scoreVal > best) best = scoreVal
            applesEaten++
            flashTimer = 0.3f
            spawnFood()
            if (applesEaten >= applesPerLevel) {
                state = State.LEVEL_COMPLETE
            }
        } else if (board[newHead.first][newHead.second] == 4) {
            // Power-up
            board[newHead.first][newHead.second] = 0
            if (Random.nextBoolean()) {
                speedBoost = true
                speedBoostTimer = 5f
            } else {
                invincible = true
                invincibleTimer = 5f
            }
            scoreVal += 25
            snake.removeLast()
        } else {
            snake.removeLast()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        // Grid isométrico
        for (r in 0 until gridSize) for (c in 0 until gridSize) {
            val x = isoX(r, c)
            val y = isoY(r, c)
            val tilePaint = paintOf("#2A2A4E")
            // Dibujar rombo (tile isométrico)
            val path = android.graphics.Path()
            path.moveTo(x, y - tileH / 2f)
            path.lineTo(x + tileW / 2f, y)
            path.lineTo(x, y + tileH / 2f)
            path.lineTo(x - tileW / 2f, y)
            path.close()
            canvas.drawPath(path, tilePaint)
            // Borde
            val borderPaint = paintOf("#3A3A5E")
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 1f
            canvas.drawPath(path, borderPaint)

            // Obstáculos
            if (board[r][c] == 3) {
                val obsPaint = paintOf("#7C7C7C")
                canvas.drawCircle(x, y, 8f, obsPaint)
            }
            // Comida
            if (board[r][c] == 2) {
                canvas.drawCircle(x, y, 6f, foodPaint)
                val shine = paintOf("#FC9838")
                shine.alpha = 150
                canvas.drawCircle(x - 2f, y - 2f, 3f, shine)
            }
            // Power-up
            if (board[r][c] == 4) {
                val puPaint = paintOf("#00E676")
                canvas.drawCircle(x, y, 8f, puPaint)
                val starPaint = paintOf("#FFFFFF")
                starPaint.textSize = 12f
                starPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("★", x, y + 4f, starPaint)
            }
        }

        // Serpiente
        for ((i, seg) in snake.withIndex()) {
            val (r, c) = seg
            val x = isoX(r, c)
            val y = isoY(r, c)
            val paint = if (i == 0) snakeHead else if (i % 2 == 0) snakePaint else snakeDark
            canvas.drawCircle(x, y, if (i == 0) 8f else 6f, paint)
            if (i == 0) {
                // Ojos
                val eyePaint = paintOf("#000000")
                canvas.drawCircle(x - 3f, y - 2f, 2f, eyePaint)
                canvas.drawCircle(x + 3f, y - 2f, 2f, eyePaint)
            }
        }

        // Flash
        if (flashTimer > 0f) {
            val alpha = (flashTimer / 0.3f * 100).toInt().coerceIn(0, 100)
            val flashPaint = Paint().apply { color = Color.parseColor("#F5C542"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), flashPaint)
        }

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("MANZANAS: $applesEaten/$applesPerLevel", w / 2f - 60f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Power-ups activos
        if (speedBoost) {
            smallPaint.textSize = 14f
            smallPaint.color = Color.parseColor("#00E676")
            canvas.drawText("VELOCIDAD x2  (${speedBoostTimer.toInt()}s)", w / 2f, 70f, smallPaint)
        }
        if (invincible) {
            smallPaint.textSize = 14f
            smallPaint.color = Color.parseColor("#5C94FC")
            canvas.drawText("INVENCIBLE  (${invincibleTimer.toInt()}s)", w / 2f, 70f, smallPaint)
        }

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Manzanas: $applesEaten", w / 2f, h / 2f + 10f, smallPaint)
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
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Nivel: $level", w / 2f, h / 2f + 40f, smallPaint)
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
            canvas.drawText("D-pad: mover  ·  Evita obstáculos y come manzanas", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
