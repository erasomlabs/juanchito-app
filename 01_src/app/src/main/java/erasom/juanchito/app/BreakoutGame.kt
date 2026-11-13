package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

class BreakoutGame : Game {
    override val name = "Rompeladrillos"
    override val genre = "Arcade"
    private var w = 0
    private var h = 0

    private val paddleW = 120f
    private val paddleH = 16f
    private val ballSize = 14f
    private var paddleX = 0f
    private var ballX = 0f
    private var ballY = 0f
    private var ballVx = 0f
    private var ballVy = 0f
    private val paddleSpeed = 700f
    private val ballSpeed = 500f
    private val brickRows = 5
    private val brickCols = 8
    private val brickH = 24f
    private val brickGap = 4f
    private var brickW = 0f
    private val bricks = mutableListOf<RectF>()
    private val brickAlive = mutableListOf<Boolean>()

    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false

    private val paddlePaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val ballPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val brickColors = listOf("#D82800", "#FC9838", "#F8D878", "#00A800", "#5C94FC")
    private val brickPaint = Paint().apply { isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 48f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 24f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val trail = mutableListOf<Pair<Float, Float>>()

    override fun init(width: Int, height: Int) {
        w = width; h = height
        brickW = (w - brickGap * (brickCols + 1)) / brickCols
        reset()
    }

    override fun reset() {
        paddleX = w / 2f - paddleW / 2f
        ballX = w / 2f
        ballY = h * 0.7f
        ballVx = ballSpeed * 0.6f
        ballVy = -ballSpeed
        score = 0
        isGameOver = false
        won = false
        bricks.clear()
        brickAlive.clear()
        val topMargin = 80f
        for (r in 0 until brickRows) {
            for (c in 0 until brickCols) {
                val x = brickGap + c * (brickW + brickGap)
                val y = topMargin + r * (brickH + brickGap)
                bricks.add(RectF(x, y, x + brickW, y + brickH))
                brickAlive.add(true)
            }
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) {
            if (input.start) reset()
            return
        }
        if (input.left) paddleX -= paddleSpeed * dt
        if (input.right) paddleX += paddleSpeed * dt
        paddleX = paddleX.coerceIn(0f, w - paddleW)

        ballX += ballVx * dt
        ballY += ballVy * dt
        trail.add(ballX to ballY)
        if (trail.size > 8) trail.removeAt(0)

        // Wall bounces
        if (ballX <= 0f) { ballX = 0f; ballVx = -ballVx }
        if (ballX >= w - ballSize) { ballX = w - ballSize; ballVx = -ballVx }
        if (ballY <= 0f) { ballY = 0f; ballVy = -ballVy }

        // Paddle collision
        if (ballY + ballSize >= h - 60f && ballY + ballSize <= h - 60f + paddleH &&
            ballX + ballSize >= paddleX && ballX <= paddleX + paddleW) {
            ballY = h - 60f - ballSize
            ballVy = -abs(ballVy)
            val hitPos = (ballX + ballSize / 2f - paddleX) / paddleW - 0.5f
            ballVx = ballSpeed * hitPos * 1.5f
        }

        // Brick collisions
        for (i in bricks.indices) {
            if (!brickAlive[i]) continue
            val b = bricks[i]
            if (ballX + ballSize >= b.left && ballX <= b.right &&
                ballY + ballSize >= b.top && ballY <= b.bottom) {
                brickAlive[i] = false
                score += 10
                if (score > highScore) highScore = score
                // Determine bounce direction
                val overlapLeft = ballX + ballSize - b.left
                val overlapRight = b.right - ballX
                val overlapTop = ballY + ballSize - b.top
                val overlapBottom = b.bottom - ballY
                val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                when (minOverlap) {
                    overlapLeft, overlapRight -> ballVx = -ballVx
                    else -> ballVy = -ballVy
                }
                break
            }
        }

        // Game over
        if (ballY > h) {
            isGameOver = true
        }

        // Win
        if (brickAlive.none { it }) {
            won = true
            isGameOver = true
        }
    }

    override fun draw(canvas: Canvas) {
        // Sala arcade: cielo nocturno, estrellas y marco neón
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#0A0A1E", "#1A1A3E", 6)
        NesScenery.drawStars(canvas, w, h, 35)
        NesScenery.drawNeonFrame(canvas, w, h, "#D82800")
        // Ladrillos biselados (borde claro arriba, oscuro abajo, brillo)
        bricks.forEachIndexed { i, b ->
            if (!brickAlive[i]) return@forEachIndexed
            val row = i / brickCols
            val base = Color.parseColor(brickColors[row % brickColors.size])
            brickPaint.color = base
            canvas.drawRect(b, brickPaint)
            // bisel superior claro
            brickPaint.color = Color.argb(255, Math.min(255, Color.red(base) + 60), Math.min(255, Color.green(base) + 60), Math.min(255, Color.blue(base) + 60))
            canvas.drawRect(b.left, b.top, b.right, b.top + 4f, brickPaint)
            // bisel inferior oscuro
            brickPaint.color = Color.argb(255, Color.red(base) * 2 / 3, Color.green(base) * 2 / 3, Color.blue(base) * 2 / 3)
            canvas.drawRect(b.left, b.bottom - 4f, b.right, b.bottom, brickPaint)
        }
        // Estela de la bola
        val trailPaint = Paint().apply { isAntiAlias = false }
        trail.forEachIndexed { i, (tx, ty) ->
            trailPaint.color = Color.parseColor("#F8F8F8")
            trailPaint.alpha = (i * 22).coerceAtMost(140)
            canvas.drawCircle(tx + ballSize / 2f, ty + ballSize / 2f, ballSize * (0.2f + i * 0.04f), trailPaint)
        }
        // Paleta Pixel: ocre con franjas de grip y protagonista montado
        val paddleRect = RectF(paddleX, h - 60f, paddleX + paddleW, h - 60f + paddleH)
        paddlePaint.color = Color.parseColor("#D4A017")
        canvas.drawRoundRect(paddleRect, 6f, 6f, paddlePaint)
        val grip = Paint().apply { color = Color.parseColor("#8C6A10"); isAntiAlias = false }
        var gx = paddleX + 8f
        while (gx < paddleX + paddleW - 6f) {
            canvas.drawRect(gx, h - 58f, gx + 4f, h - 48f, grip)
            gx += 16f
        }
        PixelSprite.draw(canvas, paddleX + paddleW / 2f, h - 74f, 24f, facingRight = true)
        // Bola con brillo
        val glow = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
        canvas.drawCircle(ballX + ballSize / 2f, ballY + ballSize / 2f, ballSize / 2f, glow)
        canvas.drawCircle(ballX + ballSize * 0.4f, ballY + ballSize * 0.4f, ballSize * 0.18f, ballPaint)
        // Score
        canvas.drawText("Score: $score", w / 2f, 40f, textPaint.apply { textSize = 28f; color = Color.parseColor("#F5F5F5") })
        canvas.drawText("Best: $highScore", w / 2f, 70f, smallPaint.apply { textSize = 20f; color = Color.parseColor("#39FF14") })
        if (isGameOver) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val gameOverColor = if (won) Color.parseColor("#39FF14") else Color.parseColor("#E63946")
            canvas.drawText(msg, w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = gameOverColor })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}
