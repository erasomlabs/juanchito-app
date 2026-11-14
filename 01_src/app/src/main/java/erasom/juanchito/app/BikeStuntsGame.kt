package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * BikeStunts: reconstrucción fiel de XBikeStunts (Xendex, 2006).
 *
 * Mecánica original:
 *  · Acrobacias en bici en rampas y obstáculos.
 *  · Tricks: backflip, frontflip, spin, etc.
 *  · Puntuación por tricks y combos.
 *  · Múltiples niveles con diferentes obstáculos.
 *  · High score.
 */
class BikeStuntsGame : Game {
    override val name = "Bike Stunts"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { RIDING, CRASHED, LEVEL_COMPLETE }
    private var state = State.RIDING

    // Bici y jugador
    private var bikeX = 0f
    private var bikeY = 0f
    private var bikeSpeed = 200f
    private var bikeAngle = 0f
    private var onRamp = false
    private var inAir = false
    private var airTimer = 0f
    private var rotation = 0f
    private var rotationSpeed = 0f

    // Obstáculos y rampas
    private data class Ramp(val x: Float, val y: Float, val w: Float, val h: Float, val angle: Float)
    private val ramps = mutableListOf<Ramp>()
    private data class Obstacle(val x: Float, var y: Float, val w: Float, val h: Float, val type: Int)  // 0=barrel, 1=trash, 2=lamp
    private val obstacles = mutableListOf<Obstacle>()

    // Tricks
    private var currentTrick = ""
    private var trickTimer = 0f
    private var combo = 0
    private var maxCombo = 0

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var level = 1
    private var maxLevel = 5
    private var distance = 0f
    private var trackLength = 3000f

    // Animación
    private var bgScroll = 0f

    // Paints
    private val skyPaint = paintOf("#5C94FC")
    private val groundPaint = paintOf("#8B4513")
    private val grassPaint = paintOf("#228B22")
    private val rampPaint = paintOf("#8B4513")
    private val rampTop = paintOf("#5A3A1A")
    private val bikePaint = paintOf("#5C94FC")
    private val bikeDark = paintOf("#3A6AD0")
    private val wheelPaint = paintOf("#2A2A2A")
    private val pilotPaint = paintOf("#F8D878")
    private val barrelPaint = paintOf("#8B4513")
    private val trashPaint = paintOf("#7C7C7C")
    private val lampPaint = paintOf("#F5C542")
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
        bikeX = w * 0.2f
        bikeY = h * 0.7f
        bikeSpeed = 200f
        bikeAngle = 0f
        onRamp = false
        inAir = false
        airTimer = 0f
        rotation = 0f
        rotationSpeed = 0f
        currentTrick = ""
        trickTimer = 0f
        combo = 0
        maxCombo = 0
        scoreVal = 0
        level = 1
        distance = 0f
        state = State.RIDING
        bgScroll = 0f
        generateLevel()
    }

    private fun generateLevel() {
        ramps.clear()
        obstacles.clear()
        // Rampas
        for (i in 0 until 4) {
            val x = 100f + i * (w / 4f) + Random.nextFloat() * 50f
            ramps.add(Ramp(x, h * 0.65f, 60f, 30f, -20f))
        }
        // Obstáculos
        for (i in 0 until 3) {
            val type = Random.nextInt(3)
            val x = 150f + i * (w / 3f) + Random.nextFloat() * 50f
            obstacles.add(Obstacle(x, h * 0.7f, 30f, 30f, type))
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.RIDING) {
            if (input.start || input.a) {
                if (state == State.CRASHED) reset()
                else if (state == State.LEVEL_COMPLETE) {
                    level++
                    if (level > maxLevel) state = State.CRASHED
                    else {
                        bikeX = w * 0.2f
                        bikeY = h * 0.7f
                        bikeSpeed = 200f
                        distance = 0f
                        generateLevel()
                        state = State.RIDING
                    }
                }
            }
            return
        }

        bgScroll += bikeSpeed * dt * 0.5f
        distance += bikeSpeed * dt
        if (distance >= trackLength) {
            state = State.LEVEL_COMPLETE
            scoreVal += 500
            if (scoreVal > best) best = scoreVal
            return
        }

        // Aceleración
        if (input.up) bikeSpeed = (bikeSpeed + 100f * dt).coerceAtMost(400f)
        else bikeSpeed = (bikeSpeed - 50f * dt).coerceAtLeast(150f)
        if (input.down) bikeSpeed = (bikeSpeed - 200f * dt).coerceAtLeast(100f)

        // Giro
        if (input.left) bikeAngle -= 2f * dt
        if (input.right) bikeAngle += 2f * dt

        // Movimiento
        bikeX += bikeSpeed * dt * 0.5f
        if (bikeX > w - 40f) {
            bikeX = w * 0.2f
            bikeY = h * 0.7f
        }

        // Verificar rampa
        onRamp = false
        for (ramp in ramps) {
            if (bikeX > ramp.x && bikeX < ramp.x + ramp.w && bikeY > ramp.y - 10f && bikeY < ramp.y + 10f) {
                onRamp = true
                if (!inAir) {
                    inAir = true
                    airTimer = 1f
                    rotationSpeed = if (Random.nextBoolean()) 360f else -360f
                }
            }
        }

        // En el aire
        if (inAir) {
            airTimer -= dt
            rotation += rotationSpeed * dt
            bikeY -= 100f * dt  // subir
            if (airTimer <= 0f) {
                inAir = false
                airTimer = 0f
                // Verificar aterrizaje
                if (kotlin.math.abs(rotation % 360f) > 30f && kotlin.math.abs(rotation % 360f) < 330f) {
                    // Crash - no aterrizó bien
                    state = State.CRASHED
                    if (scoreVal > best) best = scoreVal
                } else {
                    // Trick exitoso
                    val trickPts = 100 + combo * 50
                    scoreVal += trickPts
                    if (scoreVal > best) best = scoreVal
                    combo++
                    if (combo > maxCombo) maxCombo = combo
                    currentTrick = when {
                        rotationSpeed > 0 -> "BACKFLIP!"
                        else -> "FRONTFLIP!"
                    }
                    trickTimer = 1.5f
                    rotation = 0f
                }
            }
        } else {
            bikeY = h * 0.7f
        }

        // Colisión con obstáculos
        for (obs in obstacles) {
            if (bikeX > obs.x && bikeX < obs.x + obs.w && bikeY > obs.y - 10f && bikeY < obs.y + obs.h && !inAir) {
                state = State.CRASHED
                if (scoreVal > best) best = scoreVal
            }
        }

        // Timer de trick
        if (trickTimer > 0f) trickTimer -= dt

        // Input para tricks en el aire
        if (inAir) {
            if (input.left) rotationSpeed = -360f
            if (input.right) rotationSpeed = 360f
            if (input.a) {
                // Spin
                rotationSpeed *= 1.5f
                currentTrick = "SPIN!"
                trickTimer = 1f
                input.a = false
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#5C94FC"))

        // Césped
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), grassPaint)

        // Pista
        val trackPaint = paintOf("#8B4513")
        canvas.drawRect(0f, h * 0.6f, w.toFloat(), h * 0.85f, trackPaint)

        // Rampas
        for (ramp in ramps) {
            canvas.drawRoundRect(RectF(ramp.x, ramp.y, ramp.x + ramp.w, ramp.y + ramp.h), 4f, 4f, rampPaint)
            // Superficie de la rampa
            val rampTopPaint = paintOf("#5A3A1A")
            canvas.drawRect(ramp.x, ramp.y, ramp.x + ramp.w, ramp.y + 4f, rampTopPaint)
        }

        // Obstáculos
        for (obs in obstacles) {
            val paint = when (obs.type) {
                0 -> barrelPaint
                1 -> trashPaint
                else -> lampPaint
            }
            canvas.drawRoundRect(RectF(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h), 4f, 4f, paint)
        }

        // Bici y piloto
        canvas.save()
        canvas.translate(bikeX, bikeY)
        canvas.rotate(rotation)
        // Ruedas
        canvas.drawCircle(-12f, 8f, 6f, wheelPaint)
        canvas.drawCircle(12f, 8f, 6f, wheelPaint)
        // Cuadro
        canvas.drawRoundRect(RectF(-15f, -5f, 15f, 5f), 3f, 3f, bikePaint)
        val bikeDark = paintOf("#3A6AD0")
        canvas.drawRect(-15f, -5f, 15f, -1f, bikeDark)
        // Piloto
        canvas.drawCircle(0f, -16f, 8f, pilotPaint)
        // Ojos
        val eyePaint = paintOf("#000000")
        canvas.drawCircle(-3f, -18f, 2f, eyePaint)
        canvas.drawCircle(3f, -18f, 2f, eyePaint)
        canvas.restore()

        // Trick text
        if (trickTimer > 0f) {
            val alpha = (trickTimer / 1.5f * 255).toInt().coerceIn(0, 255)
            val trickPaint = Paint().apply { color = Color.parseColor("#F5C542"); this.alpha = alpha; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 28f; isFakeBoldText = true }
            canvas.drawText(currentTrick, bikeX, bikeY - 40f, trickPaint)
            // Combo
            if (combo > 1) {
                val comboPaint = Paint().apply { color = Color.parseColor("#00E676"); this.alpha = alpha; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 18f; isFakeBoldText = true }
                canvas.drawText("COMBO x$combo", bikeX, bikeY - 65f, comboPaint)
            }
        }

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("DISTANCIA: ${distance.toInt()}m / ${trackLength.toInt()}m", w / 2f - 80f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Max combo: $maxCombo", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Overlay de crash
        if (state == State.CRASHED) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("¡CRASH!", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Max combo: $maxCombo", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Instrucciones
        if (state == State.RIDING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("▲: acelerar  ·  ◀▶: girar/trick  ·  A: spin  ·  Haz tricks en rampas", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.CRASHED
}
