package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Bounce: reconstrucción fiel de Bounce (Nokia, 2005).
 *
 * Mecánica original:
 *  · Pelota que rebota por niveles con física.
 *  · Recoger items (anillos) para puntos.
 *  · Evitar obstáculos y caer al vacío.
 *  · Múltiples niveles con mayor dificultad.
 *  · High score.
 */
class BounceGame : Game {
    override val name = "Bounce"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Pelota
    private var ballX = 0f
    private var ballY = 0f
    private var ballVx = 0f
    private var ballVy = 0f
    private val ballRadius = 12f
    private val gravity = 400f
    private val bounceForce = -300f
    private var onGround = false

    // Nivel (plataformas)
    private data class Platform(val x: Float, val y: Float, val width: Float, val height: Float, val type: Int)
    private val platforms = mutableListOf<Platform>()
    private data class Ring(val x: Float, var y: Float, var collected: Boolean = false)
    private val rings = mutableListOf<Ring>()
    private data class Obstacle(val x: Float, var y: Float, val width: Float, val height: Float)
    private val obstacles = mutableListOf<Obstacle>()

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var ringsCollected = 0
    private var level = 1
    private var maxLevel = 11

    // Animación
    private var bgScroll = 0f
    private var bounceAnim = 0f
    private var collectFlash = 0f

    // Paints
    private val bgPaint = paintOf("#5C94FC")
    private val platformPaint = paintOf("#8B4513")
    private val platformTop = paintOf("#228B22")
    private val ballPaint = paintOf("#F8D878")
    private val ballShine = paintOf("#FFFFFF")
    private val ringPaint = paintOf("#F5C542")
    private val obstaclePaint = paintOf("#D82800")
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
        ballX = w / 2f
        ballY = h - 100f
        ballVx = 0f
        ballVy = 0f
        onGround = false
        platforms.clear()
        rings.clear()
        obstacles.clear()
        scoreVal = 0
        ringsCollected = 0
        level = 1
        state = State.PLAYING
        bgScroll = 0f
        bounceAnim = 0f
        collectFlash = 0f
        generateLevel()
    }

    private fun generateLevel() {
        platforms.clear()
        rings.clear()
        obstacles.clear()

        // Suelo
        platforms.add(Platform(0f, h - 40f, w.toFloat(), 40f, 0))

        // Plataformas aleatorias
        var y = h - 120f
        while (y > 60f) {
            val x = Random.nextFloat() * (w - 120f)
            val width = 80f + Random.nextFloat() * 60f
            platforms.add(Platform(x, y, width, 12f, 1))
            // Anillo en algunas plataformas
            if (Random.nextFloat() < 0.6f) {
                rings.add(Ring(x + width / 2f, y - 20f))
            }
            // Obstáculo ocasional
            if (Random.nextFloat() < 0.3f) {
                obstacles.add(Obstacle(x + width / 2f - 10f, y - 30f, 20f, 20f))
            }
            y -= 60f + Random.nextFloat() * 40f
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.PLAYING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.LEVEL_COMPLETE) {
                    level++
                    if (level > maxLevel) {
                        state = State.GAME_OVER
                    } else {
                        generateLevel()
                        ballX = w / 2f
                        ballY = h - 100f
                        ballVx = 0f
                        ballVy = 0f
                        state = State.PLAYING
                    }
                }
            }
            return
        }

        // Gravedad
        ballVy += gravity * dt
        ballX += ballVx * dt
        ballY += ballVy * dt
        bounceAnim = (bounceAnim + dt * 10f)

        // Rebote en plataformas
        onGround = false
        for (p in platforms) {
            if (ballX + ballRadius > p.x && ballX - ballRadius < p.x + p.width &&
                ballY + ballRadius > p.y && ballY - ballRadius < p.y + p.height &&
                ballVy > 0) {
                ballY = p.y - ballRadius
                ballVy = bounceForce
                onGround = true
            }
        }

        // Recoger anillos
        val ringIt = rings.iterator()
        while (ringIt.hasNext()) {
            val ring = ringIt.next()
            if (!ring.collected) {
                val dx = ballX - ring.x
                val dy = ballY - ring.y
                if (kotlin.math.sqrt(dx * dx + dy * dy) < ballRadius + 12f) {
                    ring.collected = true
                    ringsCollected++
                    scoreVal += 50
                    if (scoreVal > best) best = scoreVal
                    collectFlash = 0.5f
                    ringIt.remove()
                }
            }
        }
        if (collectFlash > 0f) collectFlash -= dt

        // Colisión con obstáculos
        for (obs in obstacles) {
            if (ballX + ballRadius > obs.x && ballX - ballRadius < obs.x + obs.width &&
                ballY + ballRadius > obs.y && ballY - ballRadius < obs.y + obs.height) {
                state = State.GAME_OVER
                if (scoreVal > best) best = scoreVal
                return
            }
        }

        // Caer al vacío
        if (ballY > h + 40f) {
            state = State.GAME_OVER
            if (scoreVal > best) best = scoreVal
            return
        }

        // Rebote en paredes
        if (ballX < ballRadius) { ballX = ballRadius; ballVx = Math.abs(ballVx) }
        if (ballX > w - ballRadius) { ballX = w - ballRadius; ballVx = -Math.abs(ballVx) }

        // Input: mover izquierda/derecha
        if (input.left) ballVx = -150f
        else if (input.right) ballVx = 150f
        else ballVx *= 0.95f

        // Salto
        if (input.a || input.start) {
            if (onGround) {
                ballVy = bounceForce * 1.2f
                onGround = false
            }
            input.a = false; input.start = false
        }

        // Verificar si llegó a la cima (nivel completo)
        if (ballY < 40f) {
            state = State.LEVEL_COMPLETE
            scoreVal += 200
            if (scoreVal > best) best = scoreVal
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#5C94FC"))

        // Nubes
        val cloudPaint = paintOf("#FFFFFF")
        cloudPaint.alpha = 150
        for (i in 0..4) {
            val cx = (i * 120f + bgScroll) % (w + 100f) - 50f
            canvas.drawOval(RectF(cx, 30f + i * 40f, cx + 70f, 55f + i * 40f), cloudPaint)
        }

        // Plataformas
        for (p in platforms) {
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, platformPaint)
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + 4f, platformTop)
        }

        // Anillos
        for (ring in rings) {
            if (!ring.collected) {
                val ringPaintLocal = paintOf("#F5C542")
                ringPaintLocal.style = Paint.Style.STROKE
                ringPaintLocal.strokeWidth = 3f
                canvas.drawCircle(ring.x, ring.y, 10f, ringPaintLocal)
                // Brillo
                val shinePaint = paintOf("#FFFFFF")
                shinePaint.alpha = 150
                canvas.drawCircle(ring.x - 3f, ring.y - 3f, 3f, shinePaint)
            }
        }

        // Obstáculos
        for (obs in obstacles) {
            canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height, obstaclePaint)
            val obsDark = paintOf("#A01800")
            canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + 4f, obsDark)
        }

        // Pelota
        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
        // Brillo
        ballShine.alpha = 200
        canvas.drawCircle(ballX - 4f, ballY - 4f, 4f, ballShine)
        // Sombra
        val shadowPaint = paintOf("#000000")
        shadowPaint.alpha = 60
        canvas.drawOval(RectF(ballX - ballRadius, ballY + ballRadius - 2f, ballX + ballRadius, ballY + ballRadius + 4f), shadowPaint)

        // Flash de recolección
        if (collectFlash > 0f) {
            val alpha = (collectFlash / 0.5f * 200).toInt().coerceIn(0, 200)
            val flashPaint = Paint().apply { color = Color.parseColor("#F5C542"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawCircle(ballX, ballY, 30f, flashPaint)
        }

        // HUD
        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ANILLOS: $ringsCollected", w / 2f - 50f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 16f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Anillos: $ringsCollected", w / 2f, h / 2f + 10f, smallPaint)
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
            canvas.drawText("Anillos: $ringsCollected", w / 2f, h / 2f + 40f, smallPaint)
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
            smallPaint.color = Color.parseColor("#FFFFFF")
            canvas.drawText("◀▶: mover  ·  A/START: saltar  ·  Recoge anillos", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
