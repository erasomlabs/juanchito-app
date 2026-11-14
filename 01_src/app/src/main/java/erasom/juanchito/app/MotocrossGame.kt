package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Motocross: reconstrucción fiel de OffRoadDirtMotocross (mob.ua, 2008).
 *
 * Mecánica original:
 *  · Carreras de motocross en pistas de tierra.
 *  · Saltos, derrapes, aceleración.
 *  · Múltiples pistas.
 *  · IA para rivales.
 *  · High score.
 */
class MotocrossGame : Game {
    override val name = "Motocross"
    override val genre = "Carreras"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { RACING, FINISHED, GAME_OVER }
    private var state = State.RACING

    // Jugador (moto)
    private var playerX = 0f
    private var playerY = 0f
    private var playerSpeed = 0f
    private var playerAngle = 0f
    private var onGround = true
    private var jumping = false
    private var jumpTimer = 0f
    private var leanAngle = 0f

    // Rivales
    private data class Rival(var x: Float, var y: Float, var speed: Float, var angle: Float, var onGround: Boolean = true)
    private val rivals = mutableListOf<Rival>()

    // Pista (obstáculos y rampas)
    private data class TrackPiece(val x: Float, var y: Float, val w: Float, val h: Float, val type: Int)  // 0=rampa, 1=obstáculo, 2=barro, 3=boost
    private val track = mutableListOf<TrackPiece>()

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var position = 1
    private var lap = 1
    private var maxLaps = 3
    private var raceTime = 0f
    private var distance = 0f
    private var trackLength = 4000f

    // Animación
    private var bgScroll = 0f

    // Paints
    private val skyPaint = paintOf("#5C94FC")
    private val groundPaint = paintOf("#8B4513")
    private val grassPaint = paintOf("#228B22")
    private val bikePaint = paintOf("#5C94FC")
    private val rivalPaint = paintOf("#D82800")
    private val rampPaint = paintOf("#F5C542")
    private val obstaclePaint = paintOf("#7C7C7C")
    private val mudPaint = paintOf("#5A3A1A")
    private val boostPaint = paintOf("#00E676")
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
        playerX = w / 2f
        playerY = h * 0.7f
        playerSpeed = 0f
        playerAngle = 0f
        onGround = true
        jumping = false
        jumpTimer = 0f
        leanAngle = 0f
        rivals.clear()
        for (i in 0 until 3) {
            rivals.add(Rival(w / 2f + (i - 1) * 50f, h * 0.7f - 40f - i * 25f, 0f, 0f))
        }
        track.clear()
        generateTrack()
        scoreVal = 0
        position = 1
        lap = 1
        raceTime = 0f
        distance = 0f
        state = State.RACING
        bgScroll = 0f
    }

    private fun generateTrack() {
        track.clear()
        // Rampas
        for (i in 0 until 6) {
            track.add(TrackPiece(Random.nextFloat() * (w - 60f), h * 0.4f + Random.nextFloat() * h * 0.3f, 60f, 15f, 0))
        }
        // Obstáculos
        for (i in 0 until 4) {
            track.add(TrackPiece(Random.nextFloat() * (w - 40f), h * 0.3f + Random.nextFloat() * h * 0.4f, 40f, 40f, 1))
        }
        // Barro
        for (i in 0 until 3) {
            track.add(TrackPiece(Random.nextFloat() * (w - 80f), h * 0.5f + Random.nextFloat() * h * 0.3f, 80f, 25f, 2))
        }
        // Boost
        for (i in 0 until 2) {
            track.add(TrackPiece(Random.nextFloat() * (w - 40f), h * 0.4f + Random.nextFloat() * h * 0.3f, 40f, 20f, 3))
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.RACING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.FINISHED) reset()
            }
            return
        }

        raceTime += dt
        bgScroll += dt * 50f

        // Aceleración
        val maxSpeed = 300f
        if (input.up) playerSpeed = (playerSpeed + 200f * dt).coerceAtMost(maxSpeed)
        else playerSpeed = (playerSpeed - 100f * dt).coerceAtLeast(0f)
        if (input.down) playerSpeed = (playerSpeed - 300f * dt).coerceAtLeast(0f)

        // Giro
        if (input.left) playerAngle -= 2.5f * dt
        if (input.right) playerAngle += 2.5f * dt
        // Lean
        leanAngle = if (input.left) -15f else if (input.right) 15f else 0f

        // Salto
        if ((input.a || input.start) && onGround) {
            jumping = true
            jumpTimer = 0.5f
            onGround = false
            input.a = false; input.start = false
        }
        if (jumping) {
            jumpTimer -= dt
            if (jumpTimer <= 0f) jumping = false
        }

        // Movimiento del jugador
        val vx = kotlin.math.sin(playerAngle) * playerSpeed
        val vy = -kotlin.math.cos(playerAngle) * playerSpeed
        playerX = (playerX + vx * dt).coerceIn(20f, w - 20f)
        if (!jumping) {
            playerY = (playerY + vy * dt).coerceIn(20f, h - 20f)
        } else {
            playerY += vy * dt - 200f * dt  // salto
        }

        // Colisión con terreno
        for (t in track) {
            if (playerX > t.x && playerX < t.x + t.w && playerY > t.y && playerY < t.y + t.h) {
                when (t.type) {
                    1 -> { playerSpeed *= 0.5f }  // obstáculo
                    2 -> { playerSpeed *= 0.7f }  // barro
                    3 -> { playerSpeed = (playerSpeed + 100f).coerceAtMost(400f) }  // boost
                }
            }
        }

        // IA de rivales
        for (rival in rivals) {
            val targetSpeed = 220f + Random.nextFloat() * 40f
            rival.speed = (rival.speed + (targetSpeed - rival.speed) * dt).coerceIn(150f, 300f)
            rival.y -= rival.speed * dt * 0.5f
            if (rival.y < 20f) {
                rival.y = h - 20f
                rival.x = Random.nextFloat() * (w - 40f) + 20f
            }
            rival.x += (Random.nextFloat() - 0.5f) * 40f * dt
            rival.x = rival.x.coerceIn(20f, w - 20f)
            if (rival.y < h * 0.3f) rival.onGround = false
            else rival.onGround = true
        }

        // Distancia y vueltas
        distance += playerSpeed * dt
        if (distance >= trackLength) {
            distance -= trackLength
            lap++
            if (lap > maxLaps) {
                state = State.FINISHED
                scoreVal = (maxLaps * 1000 - raceTime.toInt() * 10).coerceAtLeast(0)
                if (scoreVal > best) best = scoreVal
            }
        }

        // Posición
        position = 1
        for (rival in rivals) {
            if (rival.y < playerY) position++
        }

        // Game over por tiempo
        if (raceTime > 300f) {
            state = State.GAME_OVER
            if (scoreVal > best) best = scoreVal
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#5C94FC"))

        // Césped
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), grassPaint)

        // Pista
        val trackPaint = paintOf("#8B4513")
        canvas.drawRect(0f, h * 0.6f, w.toFloat(), h * 0.9f, trackPaint)

        // Terreno
        for (t in track) {
            val paint = when (t.type) {
                0 -> rampPaint
                1 -> obstaclePaint
                2 -> mudPaint
                3 -> boostPaint
                else -> obstaclePaint
            }
            canvas.drawRoundRect(RectF(t.x, t.y, t.x + t.w, t.y + t.h), 4f, 4f, paint)
        }

        // Rivales
        for (rival in rivals) {
            canvas.drawRoundRect(RectF(rival.x - 12f, rival.y - 8f, rival.x + 12f, rival.y + 8f), 3f, 3f, rivalPaint)
            val rDark = paintOf("#A01800")
            canvas.drawRect(rival.x - 12f, rival.y - 8f, rival.x + 12f, rival.y - 3f, rDark)
            if (!rival.onGround) {
                val wheelPaint = paintOf("#2A2A2A")
                canvas.drawCircle(rival.x - 8f, rival.y + 10f, 3f, wheelPaint)
                canvas.drawCircle(rival.x + 8f, rival.y + 10f, 3f, wheelPaint)
            }
        }

        // Jugador (moto)
        canvas.save()
        canvas.translate(playerX, playerY)
        canvas.rotate(leanAngle)
        canvas.drawRoundRect(RectF(-15f, -8f, 15f, 8f), 3f, 3f, bikePaint)
        val bikeDark = paintOf("#3A6AD0")
        canvas.drawRect(-15f, -8f, 15f, -3f, bikeDark)
        // Ruedas
        val wheelPaint = paintOf("#2A2A2A")
        canvas.drawCircle(-10f, 10f, 4f, wheelPaint)
        canvas.drawCircle(10f, 10f, 4f, wheelPaint)
        // Piloto
        val pilotPaint = paintOf("#F8D878")
        canvas.drawCircle(0f, -16f, 6f, pilotPaint)
        canvas.restore()

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("POS: ${position}º  ·  VUELTA: $lap/$maxLaps", 10f, 25f, hudPaint)
        canvas.drawText("SCORE: $scoreVal", 10f, 50f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        smallPaint.textSize = 14f
        canvas.drawText("VEL: ${playerSpeed.toInt()} km/h", w - 10f - 120f, 50f, smallPaint.apply { textAlign = Paint.Align.RIGHT })
        smallPaint.textAlign = Paint.Align.CENTER

        // Overlay de fin de carrera
        if (state == State.FINISHED) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡CARRERA TERMINADA!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 22f
            canvas.drawText("Posición: ${position}º", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Tiempo: ${raceTime.toInt()}s", w / 2f, h / 2f + 70f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 100f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 140f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Tiempo agotado", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Instrucciones
        if (state == State.RACING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#FFFFFF")
            canvas.drawText("◀▶: girar  ·  ▲: acelerar  ·  ▼: frenar  ·  A: saltar", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER || state == State.FINISHED
}
