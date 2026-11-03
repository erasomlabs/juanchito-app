package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

class PongGame : Game {
    override val name = "Pong"
    override val genre = "Arcade"
    private var w = 0
    private var h = 0

    private val paddleW = 20f
    private val paddleH = 120f
    private val ballSize = 16f
    private var playerY = 0f
    private var aiY = 0f
    private var ballX = 0f
    private var ballY = 0f
    private var ballVx = 0f
    private var ballVy = 0f
    private val paddleSpeed = 600f
    private val ballSpeed = 500f
    override var isGameOver = false
    override var score = 0
    private var aiScore = 0
    private var highScore = 0

    private val paddlePaint = Paint().apply { color = Color.parseColor("#FF6B1A"); isAntiAlias = true }
    private val ballPaint = Paint().apply { color = Color.parseColor("#FFD700"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 48f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#39FF14"); textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val linePaint = Paint().apply { color = Color.parseColor("#3A3A4A"); strokeWidth = 2f }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        playerY = h / 2f - paddleH / 2f
        aiY = h / 2f - paddleH / 2f
        ballX = w / 2f
        ballY = h / 2f
        val angle = (Math.random() * 0.6 - 0.3).toFloat()
        ballVx = if (Math.random() > 0.5) ballSpeed else -ballSpeed
        ballVy = ballSpeed * angle
        score = 0
        aiScore = 0
        isGameOver = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) {
            if (input.start) reset()
            return
        }
        // Player paddle
        if (input.up) playerY -= paddleSpeed * dt
        if (input.down) playerY += paddleSpeed * dt
        playerY = playerY.coerceIn(0f, h - paddleH)

        // AI paddle (simple tracking)
        val aiCenter = aiY + paddleH / 2f
        if (ballY < aiCenter - 10f) aiY -= paddleSpeed * 0.7f * dt
        else if (ballY > aiCenter + 10f) aiY += paddleSpeed * 0.7f * dt
        aiY = aiY.coerceIn(0f, h - paddleH)

        // Ball movement
        ballX += ballVx * dt
        ballY += ballVy * dt

        // Top/bottom bounce
        if (ballY <= 0f) { ballY = 0f; ballVy = -ballVy }
        if (ballY >= h - ballSize) { ballY = h - ballSize; ballVy = -ballVy }

        // Player paddle collision
        if (ballX <= paddleW && ballY + ballSize >= playerY && ballY <= playerY + paddleH) {
            ballX = paddleW
            ballVx = -ballVx * 1.05f
            val hitPos = (ballY + ballSize / 2f - playerY) / paddleH - 0.5f
            ballVy = ballSpeed * hitPos * 2f
        }
        // AI paddle collision
        if (ballX + ballSize >= w - paddleW && ballY + ballSize >= aiY && ballY <= aiY + paddleH) {
            ballX = w - paddleW - ballSize
            ballVx = -ballVx * 1.05f
            val hitPos = (ballY + ballSize / 2f - aiY) / paddleH - 0.5f
            ballVy = ballSpeed * hitPos * 2f
        }

        // Score
        if (ballX < 0f) {
            aiScore++
            resetBall()
        }
        if (ballX > w) {
            score++
            if (score > highScore) highScore = score
            resetBall()
        }

        if (score >= 5 || aiScore >= 5) {
            isGameOver = true
        }
    }

    private fun resetBall() {
        ballX = w / 2f
        ballY = h / 2f
        ballVx = if (Math.random() > 0.5) ballSpeed else -ballSpeed
        ballVy = (Math.random() * ballSpeed * 0.4 - ballSpeed * 0.2).toFloat()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A0A0F"))
        // Center line
        var y = 0f
        while (y < h) {
            canvas.drawLine(w / 2f, y, w / 2f, y + 10f, linePaint)
            y += 20f
        }
        // Paddles
        canvas.drawRoundRect(RectF(0f, playerY, paddleW, playerY + paddleH), 4f, 4f, paddlePaint)
        canvas.drawRoundRect(RectF(w - paddleW, aiY, w.toFloat(), aiY + paddleH), 4f, 4f, paddlePaint)
        // Ball
        canvas.drawCircle(ballX + ballSize / 2f, ballY + ballSize / 2f, ballSize / 2f, ballPaint)
        // Scores
        canvas.drawText("$score", w / 4f, 60f, textPaint)
        canvas.drawText("$aiScore", 3 * w / 4f, 60f, textPaint)
        if (isGameOver) {
            val winner = if (score >= 5) "¡GANASTE!" else "PERDISTE"
            canvas.drawText(winner, w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = if (score >= 5) Color.parseColor("#39FF14") else Color.parseColor("#E63946") })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}
