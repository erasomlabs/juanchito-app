package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Bouncy Bird: reconstrucción fiel de Bouncy Bird (Migital, 2015).
 *
 * Mecánica original (Flappy Bird style):
 *  · Tocar para que el pájaro suba, gravedad lo baja.
 *  · Esquivar tuberías (obstáculos) que se acercan.
 *  · Puntuación por cada tubería pasada.
 *  · Game over al chocar con tubería o suelo/techo.
 *  · High score, múltiples backgrounds.
 */
class BouncyBirdGame : Game {
    override val name = "Bouncy Bird"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Pájaro
    private var birdX = 0f
    private var birdY = 0f
    private var birdVy = 0f
    private val gravity = 800f
    private val flapForce = -350f
    private val birdSize = 40f

    // Obstáculos (tuberías)
    private data class Pipe(var x: Float, var gapY: Float, val gapSize: Float, var scored: Boolean = false)
    private val pipes = mutableListOf<Pipe>()
    private var pipeTimer = 0f
    private var pipeInterval = 2.0f
    private var pipeSpeed = 200f
    private val pipeWidth = 60f

    // Estado
    private enum class State { READY, PLAYING, GAME_OVER }
    private var state = State.READY

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0

    // Animación
    private var bgScroll = 0f
    private var birdRotation = 0f
    private var flashTimer = 0f

    // Paints
    private val skyPaint = paintOf("#5C94FC")
    private val groundPaint = paintOf("#8B4513")
    private val grassPaint = paintOf("#228B22")
    private val pipePaint = paintOf("#00A800")
    private val pipeDark = paintOf("#007000")
    private val pipeLight = paintOf("#30D030")
    private val birdPaint = paintOf("#F8D878")
    private val birdWing = paintOf("#D82800")
    private val birdEye = paintOf("#000000")
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
        birdX = w * 0.25f
        birdY = h / 2f
        birdVy = 0f
        birdRotation = 0f
        pipes.clear()
        pipeTimer = 0f
        pipeInterval = 2.0f
        pipeSpeed = 200f
        scoreVal = 0
        state = State.READY
        bgScroll = 0f
        flashTimer = 0f
    }

    private fun flap() {
        if (state == State.READY) state = State.PLAYING
        if (state == State.PLAYING) {
            birdVy = flapForce
            birdRotation = -30f
        }
        if (state == State.GAME_OVER) {
            reset()
        }
    }

    override fun update(dt: Float, input: GameInput) {
        bgScroll += dt * 60f
        if (bgScroll > w) bgScroll -= w

        when (state) {
            State.READY -> {
                // Pájaro flota suavemente
                birdY = h / 2f + 10f * kotlin.math.sin(System.nanoTime() / 500000000.0).toFloat()
                if (input.a || input.start) {
                    flap()
                    input.a = false; input.start = false
                }
            }
            State.PLAYING -> {
                // Gravedad
                birdVy += gravity * dt
                birdY += birdVy * dt
                birdRotation = (birdRotation + birdVy * 0.05f).coerceIn(-30f, 90f)

                // Spawn pipes
                pipeTimer += dt
                if (pipeTimer >= pipeInterval) {
                    pipeTimer = 0f
                    val gapY = Random.nextFloat() * (h * 0.4f) + h * 0.25f
                    val gapSize = (h * 0.25f).coerceAtLeast(120f)
                    pipes.add(Pipe(w + pipeWidth, gapY, gapSize))
                }

                // Mover pipes
                val it = pipes.iterator()
                while (it.hasNext()) {
                    val pipe = it.next()
                    pipe.x -= pipeSpeed * dt
                    if (pipe.x + pipeWidth < 0) {
                        it.remove()
                        continue
                    }
                    // Scoring
                    if (!pipe.scored && pipe.x + pipeWidth < birdX) {
                        pipe.scored = true
                        scoreVal++
                        if (scoreVal > best) best = scoreVal
                    }
                }

                // Colisión
                // Suelo y techo
                if (birdY + birdSize / 2f > h - 60f || birdY - birdSize / 2f < 0f) {
                    gameOver()
                    return
                }
                // Tuberías
                for (pipe in pipes) {
                    if (birdX + birdSize / 2f > pipe.x && birdX - birdSize / 2f < pipe.x + pipeWidth) {
                        val gapTop = pipe.gapY
                        val gapBottom = pipe.gapY + pipe.gapSize
                        if (birdY - birdSize / 2f < gapTop || birdY + birdSize / 2f > gapBottom) {
                            gameOver()
                            return
                        }
                    }
                }

                // Input
                if (input.a || input.start) {
                    flap()
                    input.a = false; input.start = false
                }
            }
            State.GAME_OVER -> {
                if (input.a || input.start) {
                    reset()
                    input.a = false; input.start = false
                }
            }
        }
    }

    private fun gameOver() {
        state = State.GAME_OVER
        flashTimer = 0.5f
        if (scoreVal > best) best = scoreVal
    }

    override fun draw(canvas: Canvas) {
        // Cielo
        canvas.drawColor(Color.parseColor("#5C94FC"))

        // Nubes (scrolling)
        val cloudPaint = paintOf("#FFFFFF")
        cloudPaint.alpha = 180
        for (i in 0..3) {
            val cx = (bgScroll + i * w / 3f) % (w + 100f) - 50f
            canvas.drawOval(RectF(cx, 40f + i * 30f, cx + 80f, 70f + i * 30f), cloudPaint)
            canvas.drawOval(RectF(cx + 30f, 30f + i * 30f, cx + 100f, 60f + i * 30f), cloudPaint)
        }

        // Suelo
        canvas.drawRect(0f, h - 60f, w.toFloat(), h.toFloat(), groundPaint)
        canvas.drawRect(0f, h - 70f, w.toFloat(), h - 60f, grassPaint)
        // Patrón de césped
        val grassDetail = paintOf("#1A7A1A")
        var gx = (bgScroll * 2) % 20f
        while (gx < w) {
            canvas.drawRect(gx, h - 70f, gx + 2f, h - 62f, grassDetail)
            gx += 20f
        }

        // Tuberías
        for (pipe in pipes) {
            // Tubería superior
            canvas.drawRect(pipe.x, 0f, pipe.x + pipeWidth, pipe.gapY, pipePaint)
            canvas.drawRect(pipe.x - 4f, pipe.gapY - 20f, pipe.x + pipeWidth + 4f, pipe.gapY, pipePaint)
            // Borde oscuro
            canvas.drawRect(pipe.x, 0f, pipe.x + 4f, pipe.gapY, pipeDark)
            // Brillo
            canvas.drawRect(pipe.x + 4f, 0f, pipe.x + 8f, pipe.gapY, pipeLight)
            // Tubería inferior
            val bottomTop = pipe.gapY + pipe.gapSize
            canvas.drawRect(pipe.x, bottomTop, pipe.x + pipeWidth, h - 60f, pipePaint)
            canvas.drawRect(pipe.x - 4f, bottomTop, pipe.x + pipeWidth + 4f, bottomTop + 20f, pipePaint)
            canvas.drawRect(pipe.x, bottomTop, pipe.x + 4f, h - 60f, pipeDark)
            canvas.drawRect(pipe.x + 4f, bottomTop, pipe.x + 8f, h - 60f, pipeLight)
        }

        // Pájaro
        canvas.save()
        canvas.translate(birdX, birdY)
        canvas.rotate(birdRotation)
        // Cuerpo
        canvas.drawOval(RectF(-birdSize / 2f, -birdSize / 2f, birdSize / 2f, birdSize / 2f), birdPaint)
        // Ala
        val wingPaint = paintOf("#D82800")
        canvas.drawOval(RectF(-birdSize / 4f, 0f, birdSize / 4f, birdSize / 3f), wingPaint)
        // Ojo
        canvas.drawCircle(birdSize / 4f, -birdSize / 6f, 6f, paintOf("#FFFFFF"))
        canvas.drawCircle(birdSize / 4f + 2f, -birdSize / 6f, 3f, birdEye)
        // Pico
        val beakPaint = paintOf("#FC9838")
        val beakPath = android.graphics.Path()
        beakPath.moveTo(birdSize / 2f, 0f)
        beakPath.lineTo(birdSize / 2f + 12f, -4f)
        beakPath.lineTo(birdSize / 2f + 12f, 4f)
        beakPath.close()
        canvas.drawPath(beakPath, beakPaint)
        canvas.restore()

        // Flash al morir
        if (flashTimer > 0f) {
            val alpha = (flashTimer / 0.5f * 200).toInt().coerceIn(0, 200)
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), flashPaint)
        }

        // HUD
        hudPaint.textSize = 32f
        canvas.drawText("$scoreVal", w / 2f, 50f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        // Overlay: Ready
        if (state == State.READY) {
            titlePaint.textSize = 42f
            canvas.drawText("BOUNCY BIRD", w / 2f, h / 2f - 40f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Toca para volar", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Esquiva las tuberías", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA A O START PARA COMENZAR", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Overlay: Game Over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 24f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 40f, titlePaint)
            }
            smallPaint.textSize = 18f
            canvas.drawText("Mejor: $best", w / 2f, h / 2f + 70f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#FFFFFF")
            canvas.drawText("A/START: volar  ·  Esquiva las tuberías", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
