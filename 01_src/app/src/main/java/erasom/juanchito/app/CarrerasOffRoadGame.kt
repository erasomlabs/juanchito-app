package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * CarrerasOffRoad: reconstrucción fiel de 4x4 Bigfoot Racing (Falconmobile, 2008).
 *
 * Mecánica original:
 *  · Carreras de 4x4 off-road con terreno irregular.
 *  · Nitro para boost de velocidad.
 *  · Múltiples pistas.
 *  · IA para rivales.
 *  · High score.
 */
class CarrerasOffRoadGame : Game {
    override val name = "Carreras Off-Road"
    override val genre = "Carreras"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { RACING, FINISHED, GAME_OVER }
    private var state = State.RACING

    // Jugador
    private var playerX = 0f
    private var playerY = 0f
    private var playerSpeed = 0f
    private var playerAngle = 0f
    private var nitro = 100f
    private var maxNitro = 100f
    private var usingNitro = false

    // Rivales
    private data class Rival(var x: Float, var y: Float, var speed: Float, var angle: Float)
    private val rivals = mutableListOf<Rival>()

    // Terreno (obstáculos y rampas)
    private data class Terrain(val x: Float, var y: Float, val w: Float, val h: Float, val type: Int)  // 0=rampa, 1=obstáculo, 2=barro
    private val terrains = mutableListOf<Terrain>()

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var position = 1
    private var lap = 1
    private var maxLaps = 3
    private var raceTime = 0f
    private var distance = 0f
    private var trackLength = 5000f

    // Animación
    private var bgScroll = 0f

    // Paints
    private val bgPaint = paintOf("#5C94FC")
    private val groundPaint = paintOf("#8B4513")
    private val grassPaint = paintOf("#228B22")
    private val carPaint = paintOf("#5C94FC")
    private val rivalPaint = paintOf("#D82800")
    private val rampPaint = paintOf("#F5C542")
    private val obstaclePaint = paintOf("#7C7C7C")
    private val mudPaint = paintOf("#5A3A1A")
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
        nitro = maxNitro
        usingNitro = false
        rivals.clear()
        for (i in 0 until 3) {
            rivals.add(Rival(w / 2f + (i - 1) * 60f, h * 0.7f - 50f - i * 30f, 0f, 0f))
        }
        terrains.clear()
        generateTerrain()
        scoreVal = 0
        position = 1
        lap = 1
        raceTime = 0f
        distance = 0f
        state = State.RACING
        bgScroll = 0f
    }

    private fun generateTerrain() {
        terrains.clear()
        // Rampas
        for (i in 0 until 5) {
            terrains.add(Terrain(Random.nextFloat() * (w - 60f), h * 0.4f + Random.nextFloat() * h * 0.3f, 60f, 20f, 0))
        }
        // Obstáculos
        for (i in 0 until 3) {
            terrains.add(Terrain(Random.nextFloat() * (w - 40f), h * 0.3f + Random.nextFloat() * h * 0.4f, 40f, 40f, 1))
        }
        // Barro
        for (i in 0 until 2) {
            terrains.add(Terrain(Random.nextFloat() * (w - 80f), h * 0.5f + Random.nextFloat() * h * 0.3f, 80f, 30f, 2))
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
        val maxSpeed = if (usingNitro) 400f else 250f
        if (input.up) playerSpeed = (playerSpeed + 200f * dt).coerceAtMost(maxSpeed)
        else playerSpeed = (playerSpeed - 100f * dt).coerceAtLeast(0f)
        if (input.down) playerSpeed = (playerSpeed - 300f * dt).coerceAtLeast(0f)

        // Giro
        if (input.left) playerAngle -= 2f * dt
        if (input.right) playerAngle += 2f * dt

        // Nitro
        if (input.a || input.start) {
            if (nitro > 0f) {
                usingNitro = true
                nitro = (nitro - 30f * dt).coerceAtLeast(0f)
            }
        } else {
            usingNitro = false
            nitro = (nitro + 10f * dt).coerceAtMost(maxNitro)
        }

        // Movimiento del jugador
        val vx = kotlin.math.sin(playerAngle) * playerSpeed
        val vy = -kotlin.math.cos(playerAngle) * playerSpeed
        playerX = (playerX + vx * dt).coerceIn(20f, w - 20f)
        playerY = (playerY + vy * dt).coerceIn(20f, h - 20f)

        // Colisión con terreno
        for (t in terrains) {
            if (playerX > t.x && playerX < t.x + t.w && playerY > t.y && playerY < t.y + t.h) {
                when (t.type) {
                    1 -> { playerSpeed *= 0.5f }  // obstáculo
                    2 -> { playerSpeed *= 0.7f }  // barro
                }
            }
        }

        // IA de rivales
        for (rival in rivals) {
            val targetSpeed = 200f + Random.nextFloat() * 50f
            rival.speed = (rival.speed + (targetSpeed - rival.speed) * dt).coerceIn(100f, 300f)
            rival.y -= rival.speed * dt * 0.5f
            if (rival.y < 20f) {
                rival.y = h - 20f
                rival.x = Random.nextFloat() * (w - 40f) + 20f
            }
            rival.x += (Random.nextFloat() - 0.5f) * 50f * dt
            rival.x = rival.x.coerceIn(20f, w - 20f)
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

        // Game over por tiempo (5 minutos max)
        if (raceTime > 300f) {
            state = State.GAME_OVER
            if (scoreVal > best) best = scoreVal
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#5C94FC"))

        // Césped
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), grassPaint)

        // Terreno
        for (t in terrains) {
            val paint = when (t.type) {
                0 -> rampPaint
                1 -> obstaclePaint
                2 -> mudPaint
                else -> obstaclePaint
            }
            canvas.drawRoundRect(RectF(t.x, t.y, t.x + t.w, t.y + t.h), 4f, 4f, paint)
        }

        // Rivales
        for (rival in rivals) {
            canvas.drawRoundRect(RectF(rival.x - 15f, rival.y - 10f, rival.x + 15f, rival.y + 10f), 4f, 4f, rivalPaint)
            val rDark = paintOf("#A01800")
            canvas.drawRect(rival.x - 15f, rival.y - 10f, rival.x + 15f, rival.y - 4f, rDark)
        }

        // Jugador
        canvas.save()
        canvas.translate(playerX, playerY)
        canvas.rotate(Math.toDegrees(playerAngle.toDouble()).toFloat())
        canvas.drawRoundRect(RectF(-15f, -10f, 15f, 10f), 4f, 4f, carPaint)
        val carDark = paintOf("#3A6AD0")
        canvas.drawRect(-15f, -10f, 15f, -4f, carDark)
        // Ruedas
        val wheelPaint = paintOf("#2A2A2A")
        canvas.drawCircle(-10f, 8f, 4f, wheelPaint)
        canvas.drawCircle(10f, 8f, 4f, wheelPaint)
        canvas.restore()

        // Nitro effect
        if (usingNitro) {
            val nitroPaint = paintOf("#FF5252")
            nitroPaint.alpha = 100
            canvas.drawCircle(playerX, playerY + 15f, 8f, nitroPaint)
        }

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("POS: ${position}º  ·  VUELTA: $lap/$maxLaps", 10f, 25f, hudPaint)
        canvas.drawText("SCORE: $scoreVal", 10f, 50f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        // Velocímetro
        smallPaint.textSize = 14f
        canvas.drawText("VEL: ${playerSpeed.toInt()} km/h", w - 10f - 120f, 50f, smallPaint.apply { textAlign = Paint.Align.RIGHT })
        smallPaint.textAlign = Paint.Align.CENTER
        // Nitro bar
        val nitroBarX = 10f
        val nitroBarY = 65f
        val nitroBarW = 100f
        val nitroBarH = 8f
        val nitroBg = paintOf("#2A2A3E")
        canvas.drawRect(nitroBarX, nitroBarY, nitroBarX + nitroBarW, nitroBarY + nitroBarH, nitroBg)
        val nitroFill = paintOf("#FF5252")
        canvas.drawRect(nitroBarX, nitroBarY, nitroBarX + nitroBarW * (nitro / maxNitro), nitroBarY + nitroBarH, nitroFill)
        smallPaint.textSize = 10f
        canvas.drawText("NITRO", nitroBarX + nitroBarW / 2f, nitroBarY - 3f, smallPaint)

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
            canvas.drawText("◀▶: girar  ·  ▲: acelerar  ·  ▼: frenar  ·  A: nitro", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER || state == State.FINISHED
}
