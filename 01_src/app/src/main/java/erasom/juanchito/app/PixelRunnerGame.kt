package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Runner: reconstrucción fiel de Temple Flee (mob.ua, 2010).
 *
 * Mecánica original (Temple Run style):
 *  · Correr automáticamente, esquivar obstáculos.
 *  · Saltar y deslizarse.
 *  · Recoger monedas y power-ups.
 *  · Velocidad creciente.
 *  · High score (distancia).
 */
class PixelRunnerGame : Game {
    override val name = "Pixel Runner"
    override val genre = "Runner"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { READY, RUNNING, DEAD }
    private var state = State.READY

    // Jugador
    private var playerX = 0f
    private var playerY = 0f
    private var playerVy = 0f
    private val gravity = 1200f
    private val jumpForce = -600f
    private var onGround = true
    private var sliding = false
    private var slideTimer = 0f

    // Obstáculos
    private data class Obstacle(var x: Float, var y: Float, val w: Float, val hgt: Float, val type: Int)  // 0=low (jump), 1=high (slide), 2=coin
    private val obstacles = mutableListOf<Obstacle>()
    private var spawnTimer = 0f
    private var spawnInterval = 1.5f
    private var gameSpeed = 300f

    // Monedas
    private var coins = 0

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var distance = 0f

    // Animación
    private var bgScroll = 0f
    private var runAnim = 0f

    // Paints
    private val bgPaint = paintOf("#28104A")
    private val groundPaint = paintOf("#5A3A1A")
    private val grassPaint = paintOf("#228B22")
    private val playerPaint = paintOf("#F8D878")
    private val playerDark = paintOf("#D82800")
    private val obstaclePaint = paintOf("#7C7C7C")
    private val coinPaint = paintOf("#F5C542")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#FFFFFF").apply { textAlign = Paint.Align.CENTER }
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
        playerX = w * 0.2f
        playerY = h - 100f
        playerVy = 0f
        onGround = true
        sliding = false
        slideTimer = 0f
        obstacles.clear()
        spawnTimer = 0f
        spawnInterval = 1.5f
        gameSpeed = 300f
        coins = 0
        scoreVal = 0
        distance = 0f
        state = State.READY
        bgScroll = 0f
        runAnim = 0f
    }

    override fun update(dt: Float, input: GameInput) {
        bgScroll += gameSpeed * dt * 0.5f
        if (bgScroll > w) bgScroll -= w
        runAnim += dt * 8f

        when (state) {
            State.READY -> {
                if (input.a || input.start) {
                    state = State.RUNNING
                    input.a = false; input.start = false
                }
            }
            State.RUNNING -> {
                distance += gameSpeed * dt
                scoreVal = (distance / 10f).toInt()
                if (scoreVal > best) best = scoreVal

                // Velocidad creciente
                gameSpeed = (300f + distance / 50f).coerceAtMost(600f)
                spawnInterval = (1.5f - distance / 5000f).coerceAtLeast(0.6f)

                // Física del jugador
                if (!onGround) {
                    playerVy += gravity * dt
                    playerY += playerVy * dt
                    if (playerY >= h - 100f) {
                        playerY = h - 100f
                        playerVy = 0f
                        onGround = true
                    }
                }

                // Deslizar
                if (sliding) {
                    slideTimer -= dt
                    if (slideTimer <= 0f) sliding = false
                }

                // Spawn obstáculos
                spawnTimer += dt
                if (spawnTimer >= spawnInterval) {
                    spawnTimer = 0f
                    val type = when (Random.nextInt(3)) {
                        0 -> 0  // obstáculo bajo (saltar)
                        1 -> 1  // obstáculo alto (deslizar)
                        else -> 2  // moneda
                    }
                    val obsW = if (type == 2) 20f else 30f + Random.nextFloat() * 20f
                    val obsH = if (type == 0) 40f + Random.nextFloat() * 20f else if (type == 1) 60f else 20f
                    val obsY = if (type == 1) h - 140f else h - 80f - obsH
                    obstacles.add(Obstacle(w + 20f, obsY, obsW, obsH, type))
                }

                // Mover obstáculos
                val it = obstacles.iterator()
                while (it.hasNext()) {
                    val obs = it.next()
                    obs.x -= gameSpeed * dt
                    if (obs.x + obs.w < 0) it.remove()
                }

                // Colisión
                val playerH = if (sliding) 30f else 50f
                val playerTop = if (sliding) playerY + 20f else playerY - 50f
                for (obs in obstacles) {
                    if (playerX + 20f > obs.x && playerX - 20f < obs.x + obs.w &&
                        playerTop + playerH > obs.y && playerTop < obs.y + obs.hgt) {
                        if (obs.type == 2) {
                            // Moneda
                            obstacles.remove(obs)
                            coins++
                            scoreVal += 10
                        } else {
                            // Colisión con obstáculo
                            state = State.DEAD
                            if (scoreVal > best) best = scoreVal
                        }
                    }
                }

                // Input
                if (input.a || input.up) {
                    if (onGround && !sliding) {
                        playerVy = jumpForce
                        onGround = false
                    }
                    input.a = false; input.up = false
                }
                if (input.down || input.b) {
                    if (onGround && !sliding) {
                        sliding = true
                        slideTimer = 0.5f
                    }
                    input.down = false; input.b = false
                }
            }
            State.DEAD -> {
                if (input.start || input.a) reset()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#28104A"))

        // Fondo (cielo nocturno con estrellas)
        val starPaint = paintOf("#FFFFFF")
        for (i in 0 until 30) {
            val sx = ((i * 137 + bgScroll.toInt()) % w).toFloat()
            val sy = (i * 97 % (h / 2)).toFloat()
            canvas.drawCircle(sx, sy, 1.5f, starPaint)
        }

        // Luna
        val moonPaint = paintOf("#F8F8F8")
        canvas.drawCircle(w - 60f, 50f, 25f, moonPaint)
        val moonShadow = paintOf("#28104A")
        canvas.drawCircle(w - 50f, 45f, 20f, moonShadow)

        // Suelo
        val groundPaint = paintOf("#5A3A1A")
        canvas.drawRect(0f, h - 80f, w.toFloat(), h.toFloat(), groundPaint)
        val grassPaint = paintOf("#228B22")
        canvas.drawRect(0f, h - 90f, w.toFloat(), h - 80f, grassPaint)

        // Obstáculos
        for (obs in obstacles) {
            val paint = when (obs.type) {
                0 -> obstaclePaint
                1 -> obstaclePaint
                2 -> coinPaint
                else -> obstaclePaint
            }
            if (obs.type == 2) {
                // Moneda (círculo dorado)
                canvas.drawCircle(obs.x + obs.w / 2f, obs.y + obs.hgt / 2f, obs.w / 2f, coinPaint)
                val shine = paintOf("#FFFFFF")
                shine.alpha = 150
                canvas.drawCircle(obs.x + obs.w / 2f - 3f, obs.y + obs.hgt / 2f - 3f, obs.w / 4f, shine)
            } else {
                canvas.drawRoundRect(RectF(obs.x, obs.y, obs.x + obs.w, obs.y + obs.hgt), 4f, 4f, paint)
                // Detalle
                val detail = paintOf("#4A4A4A")
                canvas.drawRect(obs.x + 3f, obs.y + 3f, obs.x + obs.w - 3f, obs.y + 6f, detail)
            }
        }

        // Jugador
        val pColor = if (sliding) "#D82800" else "#F8D878"
        val pPaint = paintOf(pColor)
        val pH = if (sliding) 30f else 50f
        val pY = if (sliding) playerY + 20f else playerY - 50f
        canvas.drawRoundRect(RectF(playerX - 20f, pY, playerX + 20f, pY + pH), 6f, 6f, pPaint)
        // Ojos
        val eyePaint = paintOf("#FFFFFF")
        canvas.drawCircle(playerX - 5f, pY + 10f, 4f, eyePaint)
        canvas.drawCircle(playerX + 5f, pY + 10f, 4f, eyePaint)
        val pupil = paintOf("#000000")
        canvas.drawCircle(playerX - 5f, pY + 10f, 2f, pupil)
        canvas.drawCircle(playerX + 5f, pY + 10f, 2f, pupil)
        // Piernas (animación de carrera)
        if (!sliding && onGround) {
            val legPaint = paintOf("#D82800")
            val legOffset = kotlin.math.sin(runAnim) * 5f
            canvas.drawRect(playerX - 12f, pY + pH, playerX - 4f, pY + pH + 12f + legOffset, legPaint)
            canvas.drawRect(playerX + 4f, pY + pH, playerX + 12f, pY + pH + 12f - legOffset, legPaint)
        }

        // HUD
        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("MONEDAS: $coins", w / 2f - 50f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("DISTANCIA: ${distance.toInt()}m", w / 2f - 60f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Overlay: Ready
        if (state == State.READY) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 120; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("PIXEL RUNNER", w / 2f, h / 2f - 40f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Esquiva obstáculos y recoge monedas", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Salta y deslízate", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA A O START PARA COMENZAR", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Overlay: Dead
        if (state == State.DEAD) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Distancia: ${distance.toInt()}m", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Monedas: $coins", w / 2f, h / 2f + 70f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 100f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 140f, greenPaint)
        }

        // Instrucciones
        if (state == State.RUNNING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#FFFFFF")
            canvas.drawText("A/▲: saltar  ·  B/▼: deslizar  ·  Esquiva obstáculos", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.DEAD
}
