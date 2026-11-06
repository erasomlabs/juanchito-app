package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

class BreakoutGame : Game {
    override val name = "Breakout"
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
        canvas.drawColor(Color.parseColor("#000000"))
        // Bricks
        bricks.forEachIndexed { i, b ->
            if (!brickAlive[i]) return@forEachIndexed
            val row = i / brickCols
            brickPaint.color = Color.parseColor(brickColors[row % brickColors.size])
            canvas.drawRoundRect(b, 4f, 4f, brickPaint)
        }
        // Paddle
        canvas.drawRoundRect(RectF(paddleX, h - 60f, paddleX + paddleW, h - 60f + paddleH), 6f, 6f, paddlePaint)
        // Ball
        canvas.drawCircle(ballX + ballSize / 2f, ballY + ballSize / 2f, ballSize / 2f, ballPaint)
        // Score
        canvas.drawText("Score: $score", w / 2f, 40f, textPaint.apply { textSize = 28f; color = Color.parseColor("#F5F5F5") })
        canvas.drawText("Best: $highScore", w / 2f, 70f, smallPaint.apply { textSize = 20f; color = Color.parseColor("#39FF14") })
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val gameOverColor = if (won) Color.parseColor("#39FF14") else Color.parseColor("#E63946")
            canvas.drawText(msg, w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = gameOverColor })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}
