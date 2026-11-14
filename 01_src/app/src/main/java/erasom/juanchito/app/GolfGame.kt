package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.random.Random

/**
 * Golf: reconstrucción fiel de Nokia Golf (Nokia, 2005).
 *
 * Mecánica original:
 *  · Juego de golf con swing meter (potencia + precisión).
 *  · Viento afecta la trayectoria de la bola.
 *  · 18 hoyos × 2 cursos.
 *  · Múltiples clubs (driver, iron, putter).
 *  · Puntuación por golpes (par, birdie, eagle, etc.).
 */
class GolfGame : Game {
    override val name = "Golf"
    override val genre = "Deportes"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { AIMING, SWINGING, BALL_FLYING, HOLE_COMPLETE, GAME_OVER }
    private var state = State.AIMING

    // Bola
    private var ballX = 0f
    private var ballY = 0f
    private var ballVx = 0f
    private var ballVy = 0f
    private var ballOnGround = true

    // Hoyo
    private var holeX = 0f
    private var holeY = 0f
    private var holeRadius = 12f

    // Terreno
    private var terrain: Array<FloatArray> = arrayOf()
    private val terrainCols = 20
    private val terrainRows = 15

    // Swing meter
    private var swingPower = 0f
    private var swingDir = 1
    private var swingPhase = 0  // 0=power, 1=accuracy, 2=done
    private var swingAccuracy = 0f
    private var swingTimer = 0f

    // Viento
    private var windX = 0f
    private var windY = 0f

    // Club
    private var selectedClub = 1  // 0=driver, 1=iron, 2=putter
    private val clubs = listOf("DRIVER", "IRON", "PUTTER")
    private val clubPower = listOf(300f, 200f, 80f)
    private val clubAccuracy = listOf(0.7f, 0.85f, 0.95f)

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var strokes = 0
    private var totalStrokes = 0
    private var par = 4
    private var hole = 1
    private val maxHoles = 9

    // Animación
    private var ballTrail = mutableListOf<Pair<Float, Float>>()
    private var holeCompleteTimer = 0f

    // Paints
    private val skyPaint = paintOf("#5C94FC")
    private val grassPaint = paintOf("#228B22")
    private val grassDark = paintOf("#1A6A1A")
    private val sandPaint = paintOf("#F5DEB3")
    private val waterPaint = paintOf("#00BFFF")
    private val ballPaint = paintOf("#FFFFFF")
    private val ballShadow = paintOf("#000000")
    private val holePaint = paintOf("#000000")
    private val flagPaint = paintOf("#FF0000")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#FFFFFF").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val swingPaint = paintOf("#F5C542")
    private val swingBg = paintOf("#2A2A4E")

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        hole = 1
        totalStrokes = 0
        scoreVal = 0
        best = 0
        state = State.AIMING
        setupHole()
    }

    private fun setupHole() {
        // Terreno simple
        terrain = Array(terrainCols) { FloatArray(terrainRows) }
        for (r in 0 until terrainRows) for (c in 0 until terrainCols) {
            terrain[c][r] = 0f  // 0=grass, 1=sand, 2=water
        }
        // Agregar algunos obstáculos
        for (i in 0 until 3) {
            val c = Random.nextInt(terrainCols)
            val r = Random.nextInt(terrainRows)
            terrain[c][r] = 1f  // sand
        }
        for (i in 0 until 2) {
            val c = Random.nextInt(terrainCols)
            val r = Random.nextInt(terrainRows)
            terrain[c][r] = 2f  // water
        }

        // Posición de la bola (tee)
        ballX = w * 0.15f
        ballY = h * 0.7f
        ballVx = 0f
        ballVy = 0f
        ballOnGround = true
        ballTrail.clear()

        // Posición del hoyo
        holeX = w * 0.85f
        holeY = h * 0.6f

        // Viento
        windX = (Random.nextFloat() * 2f - 1f) * 30f
        windY = (Random.nextFloat() * 2f - 1f) * 20f

        // Par
        par = if (hole <= 3) 3 else if (hole <= 6) 4 else 5

        // Swing
        swingPower = 0f
        swingDir = 1
        swingPhase = 0
        swingAccuracy = 0f
        swingTimer = 0f
        selectedClub = 1
        strokes = 0
        state = State.AIMING
    }

    private fun startSwing() {
        if (state != State.AIMING) return
        state = State.SWINGING
        swingPhase = 0
        swingPower = 0f
        swingDir = 1
        swingTimer = 0f
    }

    private fun hitBall() {
        if (state != State.SWINGING) return
        val power = swingPower / 100f
        val accuracy = swingAccuracy
        val clubPwr = clubPower[selectedClub]
        val clubAcc = clubAccuracy[selectedClub]

        // Calcular trayectoria
        val angle = -Math.PI / 4  // 45 grados hacia arriba
        val speed = clubPwr * power * (0.8f + accuracy * 0.2f)
        ballVx = (speed * kotlin.math.cos(angle)).toFloat() + windX * (1f - accuracy)
        ballVy = (speed * kotlin.math.sin(angle)).toFloat() + windY * (1f - accuracy)
        ballOnGround = false
        state = State.BALL_FLYING
        strokes++
        totalStrokes++
    }

    override fun update(dt: Float, input: GameInput) {
        when (state) {
            State.AIMING -> {
                // Seleccionar club
                if (input.up) { selectedClub = (selectedClub - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { selectedClub = (selectedClub + 1).coerceAtMost(2); input.down = false }
                if (input.left) { /* ajustar ángulo */ input.left = false }
                if (input.right) { /* ajustar ángulo */ input.right = false }
                if (input.a || input.start) {
                    startSwing()
                    input.a = false; input.start = false
                }
            }
            State.SWINGING -> {
                swingTimer += dt
                when (swingPhase) {
                    0 -> {
                        // Fase de potencia
                        swingPower += 150f * swingDir * dt
                        if (swingPower >= 100f) { swingPower = 100f; swingDir = -1 }
                        if (swingPower <= 0f) { swingPower = 0f; swingDir = 1 }
                        if (input.a || input.start) {
                            swingPhase = 1
                            swingAccuracy = swingPower / 100f
                            input.a = false; input.start = false
                        }
                    }
                    1 -> {
                        // Fase de precisión
                        swingAccuracy = (swingAccuracy + 2f * dt).coerceAtMost(1f)
                        if (swingAccuracy >= 1f) swingAccuracy = 0f
                        if (input.a || input.start) {
                            hitBall()
                            input.a = false; input.start = false
                        }
                    }
                }
            }
            State.BALL_FLYING -> {
                // Física de la bola
                ballVy += 200f * dt  // gravedad
                ballVx += windX * 0.1f * dt
                ballVy += windY * 0.1f * dt
                ballX += ballVx * dt
                ballY += ballVy * dt
                ballTrail.add(ballX to ballY)
                if (ballTrail.size > 30) ballTrail.removeAt(0)

                // Colisión con el suelo
                if (ballY >= h * 0.7f) {
                    ballY = h * 0.7f
                    ballVx *= 0.7f
                    ballVy = -ballVy * 0.3f
                    if (Math.abs(ballVy) < 20f) {
                        ballVy = 0f
                        ballOnGround = true
                        // Verificar si llegó al hoyo
                        val distToHole = kotlin.math.sqrt((ballX - holeX) * (ballX - holeX) + (ballY - holeY) * (ballY - holeY))
                        if (distToHole < holeRadius * 2f) {
                            state = State.HOLE_COMPLETE
                            val scorePts = when {
                                strokes == 1 -> 500  // hole in one
                                strokes <= par - 2 -> 300  // eagle
                                strokes == par - 1 -> 200  // birdie
                                strokes == par -> 100  // par
                                strokes == par + 1 -> 50  // bogey
                                else -> 10  // double bogey+
                            }
                            scoreVal += scorePts
                            if (scoreVal > best) best = scoreVal
                        } else {
                            state = State.AIMING
                        }
                    }
                }

                // Fuera de pantalla
                if (ballX < 0 || ballX > w || ballY < 0) {
                    ballX = ballX.coerceIn(0f, w.toFloat())
                    ballY = h * 0.7f
                    ballVx = 0f
                    ballVy = 0f
                    ballOnGround = true
                    state = State.AIMING
                }
            }
            State.HOLE_COMPLETE -> {
                holeCompleteTimer += dt
                if (input.start || input.a) {
                    hole++
                    if (hole > maxHoles) {
                        state = State.GAME_OVER
                    } else {
                        setupHole()
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
        canvas.drawColor(Color.parseColor("#5C94FC"))

        // Césped
        canvas.drawRect(0f, h * 0.65f, w.toFloat(), h.toFloat(), grassPaint)
        // Patrón de césped
        val grassDetail = paintOf("#1A6A1A")
        var gx = 0f
        while (gx < w) {
            canvas.drawRect(gx, h * 0.65f, gx + 3f, h * 0.7f, grassDark)
            gx += 15f
        }

        // Obstáculos de agua
        val waterPaintLocal = paintOf("#00BFFF")
        canvas.drawOval(RectF(w * 0.4f, h * 0.72f, w * 0.6f, h * 0.78f), waterPaintLocal)

        // Obstáculos de arena
        canvas.drawOval(RectF(w * 0.7f, h * 0.68f, w * 0.85f, h * 0.72f), sandPaint)

        // Hoyo
        canvas.drawCircle(holeX, holeY, holeRadius, holePaint)
        // Bandera
        val flagPole = paintOf("#FFFFFF")
        canvas.drawRect(holeX - 1f, holeY - 40f, holeX + 1f, holeY, flagPole)
        val flagPath = android.graphics.Path()
        flagPath.moveTo(holeX + 1f, holeY - 40f)
        flagPath.lineTo(holeX + 20f, holeY - 32f)
        flagPath.lineTo(holeX + 1f, holeY - 24f)
        flagPath.close()
        canvas.drawPath(flagPath, flagPaint)

        // Trail de la bola
        for (i in 0 until ballTrail.size - 1) {
            val alpha = (i.toFloat() / ballTrail.size * 100).toInt()
            val trailPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = alpha; isAntiAlias = true }
            canvas.drawCircle(ballTrail[i].first, ballTrail[i].second, 3f, trailPaint)
        }

        // Bola
        if (state != State.HOLE_COMPLETE) {
            canvas.drawCircle(ballX, ballY, 8f, ballPaint)
            val shine = paintOf("#FFFFFF")
            shine.alpha = 200
            canvas.drawCircle(ballX - 2f, ballY - 2f, 3f, shine)
            // Sombra
            ballShadow.alpha = 60
            canvas.drawOval(RectF(ballX - 8f, ballY + 6f, ballX + 8f, ballY + 10f), ballShadow)
        }

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("HOYO: $hole/$maxHoles", w / 2f - 50f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("GOLPES: $strokes (Par $par)", w / 2f - 60f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Viento
        val windArrow = if (windX > 0) "→" else "←"
        smallPaint.textSize = 14f
        canvas.drawText("Viento: $windArrow ${Math.abs(windX).toInt()}", 20f, h - 20f, smallPaint)

        // Swing meter (solo en SWINGING)
        if (state == State.SWINGING) {
            val meterX = w / 2f - 100f
            val meterY = h - 80f
            val meterW = 200f
            val meterH = 20f
            canvas.drawRect(meterX, meterY, meterX + meterW, meterY + meterH, swingBg)
            // Potencia
            val powerW = meterW * (swingPower / 100f)
            canvas.drawRect(meterX, meterY, meterX + powerW, meterY + meterH, swingPaint)
            // Frame
            val framePaint = paintOf("#F8F8F8")
            framePaint.style = Paint.Style.STROKE
            framePaint.strokeWidth = 2f
            canvas.drawRect(meterX, meterY, meterX + meterW, meterY + meterH, framePaint)
            smallPaint.textSize = 14f
            if (swingPhase == 0) {
                canvas.drawText("POTENCIA: ${swingPower.toInt()}%  (A para bloquear)", w / 2f, meterY - 10f, smallPaint)
            } else {
                canvas.drawText("PRECISIÓN: ${(swingAccuracy * 100).toInt()}%  (A para golpear)", w / 2f, meterY - 10f, smallPaint)
            }
        }

        // Club selector (solo en AIMING)
        if (state == State.AIMING) {
            smallPaint.textSize = 16f
            canvas.drawText("Club: ${clubs[selectedClub]}  (▲▼ para cambiar)", w / 2f, h - 50f, smallPaint)
            greenPaint.textSize = 18f
            canvas.drawText("A/START: iniciar swing", w / 2f, h - 30f, greenPaint)
        }

        // Overlay de hoyo completo
        if (state == State.HOLE_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡HOYO COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            val resultText = when {
                strokes == 1 -> "¡HOLE IN ONE!"
                strokes <= par - 2 -> "¡EAGLE!"
                strokes == par - 1 -> "¡BIRDIE!"
                strokes == par -> "PAR"
                strokes == par + 1 -> "BOGEY"
                else -> "+${strokes - par}"
            }
            canvas.drawText(resultText, w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Golpes: $strokes (Par $par)", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 70f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE HOYO", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("JUEGO TERMINADO", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación final: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Total golpes: $totalStrokes", w / 2f, h / 2f + 40f, smallPaint)
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
