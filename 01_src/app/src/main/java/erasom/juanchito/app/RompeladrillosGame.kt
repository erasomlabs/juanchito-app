package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Rompeladrillos: reconstrucción fiel de SmashHit (Twistfuture, 2010).
 *
 * Mecánica original (Breakout style):
 *  · Pelota rebota y rompe ladrillos.
 *  · Paddle controlado por el jugador.
 *  · Power-ups: multi-ball, paddle grande, fuego, etc.
 *  · Múltiples niveles con diferentes layouts.
 *  · High score.
 */
class RompeladrillosGame : Game {
    override val name = "Rompeladrillos"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Pelota
    private data class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float)
    private val balls = mutableListOf<Ball>()
    private val ballRadius = 8f
    private var ballSpeed = 400f

    // Paddle
    private var paddleX = 0f
    private var paddleY = 0f
    private var paddleW = 100f
    private var paddleH = 15f
    private var paddleSpeed = 400f

    // Ladrillos
    private data class Brick(val x: Float, val y: Float, val w: Float, val h: Float, var hp: Int, val color: String)
    private val bricks = mutableListOf<Brick>()

    // Power-ups
    private data class PowerUp(val x: Float, var y: Float, val type: Int)  // 0=big paddle, 1=multi-ball, 2=fire, 3=speed
    private val powerUps = mutableListOf<PowerUp>()

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var level = 1
    private var maxLevel = 10
    private var bricksDestroyed = 0
    private var totalBricks = 0

    // Animación
    private var flashTimer = 0f
    private var flashX = 0f
    private var flashY = 0f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val paddlePaint = paintOf("#5C94FC")
    private val paddleDark = paintOf("#3A6AD0")
    private val ballPaint = paintOf("#F8D878")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private val brickColors = listOf("#D82800", "#FC9838", "#F8D878", "#00A800", "#5C94FC", "#8814A8")

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        state = State.PLAYING
        scoreVal = 0
        level = 1
        balls.clear()
        balls.add(Ball(w / 2f, h - 60f, 0f, -ballSpeed))
        paddleX = w / 2f - paddleW / 2f
        paddleY = h - 40f
        paddleW = 100f
        powerUps.clear()
        flashTimer = 0f
        generateLevel()
    }

    private fun generateLevel() {
        bricks.clear()
        val brickW = (w - 20f) / 10f
        val brickH = 20f
        val startY = 60f
        val numRows = 4 + level
        totalBricks = 0
        for (r in 0 until numRows.coerceAtMost(8)) for (c in 0 until 10) {
            if (Random.nextFloat() < 0.85f) {
                val hp = if (r < 2) 2 else 1
                val color = brickColors[r % brickColors.size]
                bricks.add(Brick(10f + c * brickW, startY + r * brickH, brickW - 2f, brickH - 2f, hp, color))
                totalBricks++
            }
        }
        bricksDestroyed = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (flashTimer > 0f) flashTimer -= dt

        when (state) {
            State.PLAYING -> {
                // Mover paddle
                if (input.left) paddleX = (paddleX - paddleSpeed * dt).coerceAtLeast(10f)
                if (input.right) paddleX = (paddleX + paddleSpeed * dt).coerceAtMost(w - paddleW - 10f)

                // Mover bolas
                val ballIt = balls.iterator()
                while (ballIt.hasNext()) {
                    val ball = ballIt.next()
                    ball.x += ball.vx * dt
                    ball.y += ball.vy * dt

                    // Rebote en paredes
                    if (ball.x < ballRadius) { ball.x = ballRadius; ball.vx = Math.abs(ball.vx) }
                    if (ball.x > w - ballRadius) { ball.x = w - ballRadius; ball.vx = -Math.abs(ball.vx) }
                    if (ball.y < ballRadius) { ball.y = ballRadius; ball.vy = Math.abs(ball.vy) }

                    // Rebote en paddle
                    if (ball.y + ballRadius > paddleY && ball.y - ballRadius < paddleY + paddleH &&
                        ball.x > paddleX && ball.x < paddleX + paddleW && ball.vy > 0) {
                        ball.vy = -Math.abs(ball.vy)
                        // Ajustar ángulo según posición en el paddle
                        val hitPos = (ball.x - paddleX) / paddleW
                        ball.vx = (hitPos - 0.5f) * ballSpeed * 1.5f
                        ball.vy = -Math.sqrt((ballSpeed * ballSpeed - ball.vx * ball.vx).toDouble()).toFloat()
                    }

                    // Colisión con ladrillos
                    val brickIt = bricks.iterator()
                    while (brickIt.hasNext()) {
                        val brick = brickIt.next()
                        if (ball.x + ballRadius > brick.x && ball.x - ballRadius < brick.x + brick.w &&
                            ball.y + ballRadius > brick.y && ball.y - ballRadius < brick.y + brick.h) {
                            // Rebote
                            val overlapLeft = ball.x + ballRadius - brick.x
                            val overlapRight = brick.x + brick.w - (ball.x - ballRadius)
                            val overlapTop = ball.y + ballRadius - brick.y
                            val overlapBottom = brick.y + brick.h - (ball.y - ballRadius)
                            val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                            when (minOverlap) {
                                overlapLeft -> ball.vx = -Math.abs(ball.vx)
                                overlapRight -> ball.vx = Math.abs(ball.vx)
                                overlapTop -> ball.vy = -Math.abs(ball.vy)
                                overlapBottom -> ball.vy = Math.abs(ball.vy)
                            }
                            brick.hp--
                            if (brick.hp <= 0) {
                                brickIt.remove()
                                bricksDestroyed++
                                scoreVal += 10
                                if (scoreVal > best) best = scoreVal
                                flashTimer = 0.2f
                                flashX = brick.x + brick.w / 2f
                                flashY = brick.y + brick.h / 2f
                                // Power-up drop (15% chance)
                                if (Random.nextFloat() < 0.15f) {
                                    powerUps.add(PowerUp(flashX, flashY, Random.nextInt(4)))
                                }
                            } else {
                                scoreVal += 5
                            }
                            break
                        }
                    }

                    // Bola cae
                    if (ball.y > h) {
                        ballIt.remove()
                    }
                }

                // Si no quedan bolas, game over
                if (balls.isEmpty()) {
                    state = State.GAME_OVER
                    if (scoreVal > best) best = scoreVal
                }

                // Power-ups
                val puIt = powerUps.iterator()
                while (puIt.hasNext()) {
                    val pu = puIt.next()
                    pu.y += 100f * dt
                    if (pu.y > h) { puIt.remove(); continue }
                    // Colisión con paddle
                    if (pu.x > paddleX && pu.x < paddleX + paddleW && pu.y > paddleY && pu.y < paddleY + paddleH) {
                        when (pu.type) {
                            0 -> paddleW = (paddleW + 30f).coerceAtMost(180f)
                            1 -> { for (i in 0 until 2) balls.add(Ball(paddleX + paddleW / 2f, paddleY - 10f, (Random.nextFloat() - 0.5f) * 200f, -ballSpeed)) }
                            2 -> ballSpeed = (ballSpeed + 50f).coerceAtMost(600f)
                            3 -> { /* fire mode - not implemented */ }
                        }
                        puIt.remove()
                        scoreVal += 25
                    }
                }

                // Verificar nivel completo
                if (bricks.isEmpty() && totalBricks > 0) {
                    state = State.LEVEL_COMPLETE
                    scoreVal += 200
                    if (scoreVal > best) best = scoreVal
                }

                // Input
                if (input.a || input.start) {
                    // Lanzar bola si está quieta
                    for (ball in balls) {
                        if (ball.vx == 0f && ball.vy == 0f) {
                            ball.vx = (Random.nextFloat() - 0.5f) * 100f
                            ball.vy = -ballSpeed
                        }
                    }
                    input.a = false; input.start = false
                }
            }
            State.LEVEL_COMPLETE -> {
                if (input.start || input.a) {
                    level++
                    if (level > maxLevel) state = State.GAME_OVER
                    else {
                        balls.clear()
                        balls.add(Ball(w / 2f, h - 60f, 0f, 0f))
                        paddleX = w / 2f - paddleW / 2f
                        powerUps.clear()
                        generateLevel()
                        state = State.PLAYING
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
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        // Ladrillos
        for (brick in bricks) {
            val brickPaint = paintOf(brick.color)
            canvas.drawRoundRect(RectF(brick.x, brick.y, brick.x + brick.w, brick.y + brick.h), 4f, 4f, brickPaint)
            // Brillo superior
            val shine = paintOf("#FFFFFF")
            shine.alpha = 80
            canvas.drawRect(brick.x + 2f, brick.y + 1f, brick.x + brick.w - 2f, brick.y + 4f, shine)
            // HP indicator
            if (brick.hp > 1) {
                val hpPaint = paintOf("#FFFFFF")
                hpPaint.textSize = 10f
                hpPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(brick.hp.toString(), brick.x + brick.w / 2f, brick.y + brick.h / 2f + 4f, hpPaint)
            }
        }

        // Power-ups
        for (pu in powerUps) {
            val puColor = when (pu.type) {
                0 -> "#5C94FC"  // big paddle
                1 -> "#F8D878"  // multi-ball
                2 -> "#D82800"  // speed
                else -> "#00E676"  // fire
            }
            val puPaint = paintOf(puColor)
            canvas.drawCircle(pu.x, pu.y, 10f, puPaint)
            val label = when (pu.type) { 0 -> "P"; 1 -> "M"; 2 -> "V"; else -> "F" }
            val labelPaint = paintOf("#FFFFFF")
            labelPaint.textSize = 10f
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, pu.x, pu.y + 4f, labelPaint)
        }

        // Paddle
        canvas.drawRoundRect(RectF(paddleX, paddleY, paddleX + paddleW, paddleY + paddleH), 6f, 6f, paddlePaint)
        val paddleShine = paintOf("#FFFFFF")
        paddleShine.alpha = 80
        canvas.drawRect(paddleX + 4f, paddleY + 2f, paddleX + paddleW - 4f, paddleY + 5f, paddleShine)

        // Bolas
        for (ball in balls) {
            canvas.drawCircle(ball.x, ball.y, ballRadius, ballPaint)
            val shine = paintOf("#FFFFFF")
            shine.alpha = 200
            canvas.drawCircle(ball.x - 2f, ball.y - 2f, 3f, shine)
        }

        // Flash
        if (flashTimer > 0f) {
            val alpha = (flashTimer / 0.2f * 150).toInt().coerceIn(0, 150)
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawCircle(flashX, flashY, 20f, flashPaint)
        }

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("LADRILLOS: $bricksDestroyed/$totalBricks", w / 2f - 60f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
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
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 40f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("◀▶: mover paddle  ·  A/START: lanzar bola", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
