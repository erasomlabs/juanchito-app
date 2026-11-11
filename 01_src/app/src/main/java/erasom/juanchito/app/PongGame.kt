package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

class PongGame : Game {
    override val name = "Duelo Raqueta"
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

    private val paddlePaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val ballPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 48f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 24f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val linePaint = Paint().apply { color = Color.parseColor("#F8F8F8"); strokeWidth = 2f }
    private val trail = mutableListOf<Pair<Float, Float>>()

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
        trail.add(ballX to ballY)
        if (trail.size > 8) trail.removeAt(0)

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
        // Cancha: fondo azul profundo + marco neón + línea central punteada
        canvas.drawColor(Color.parseColor("#101030"))
        NesScenery.drawNeonFrame(canvas, w, h, "#5C94FC")
        var y = 0f
        while (y < h) {
            canvas.drawLine(w / 2f, y, w / 2f, y + 10f, linePaint)
            y += 20f
        }
        // Estela de la bola (efecto velocidad)
        val trailPaint = Paint().apply { isAntiAlias = false }
        trail.forEachIndexed { i, (tx, ty) ->
            trailPaint.color = Color.parseColor("#F8F8F8")
            trailPaint.alpha = (i * 22).coerceAtMost(140)
            canvas.drawCircle(tx + ballSize / 2f, ty + ballSize / 2f, ballSize * (0.2f + i * 0.04f), trailPaint)
        }
        // Raqueta del jugador (Juanchito, ocre) con grip
        val gripPaint = Paint().apply { color = Color.parseColor("#D4A017"); isAntiAlias = false }
        canvas.drawRoundRect(RectF(0f, playerY, paddleW, playerY + paddleH), 4f, 4f, paddlePaint)
        var gy = playerY + 12f
        while (gy < playerY + paddleH - 8f) {
            canvas.drawRect(2f, gy, paddleW - 2f, gy + 4f, gripPaint)
            gy += 18f
        }
        // Raqueta IA (roja) con grip
        val grip2 = Paint().apply { color = Color.parseColor("#8C1414"); isAntiAlias = false }
        canvas.drawRoundRect(RectF(w - paddleW, aiY, w.toFloat(), aiY + paddleH), 4f, 4f, paddlePaint)
        gy = aiY + 12f
        while (gy < aiY + paddleH - 8f) {
            canvas.drawRect(w - paddleW + 2f, gy, w - 2f, gy + 4f, grip2)
            gy += 18f
        }
        // Bola con brillo
        val glow = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
        canvas.drawCircle(ballX + ballSize / 2f, ballY + ballSize / 2f, ballSize / 2f, glow)
        canvas.drawCircle(ballX + ballSize * 0.4f, ballY + ballSize * 0.4f, ballSize * 0.18f, ballPaint)
        // Marcador tipo tablero
        val boardPaint = Paint().apply { color = Color.parseColor("#000000"); alpha = 140; isAntiAlias = false }
        canvas.drawRect(w * 0.15f, 12f, w * 0.35f, 66f, boardPaint)
        canvas.drawRect(w * 0.65f, 12f, w * 0.85f, 66f, boardPaint)
        canvas.drawText("$score", w / 4f, 60f, textPaint)
        canvas.drawText("$aiScore", 3 * w / 4f, 60f, textPaint)
        if (isGameOver) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            val winner = if (score >= 5) "¡GANASTE!" else "PERDISTE"
            canvas.drawText(winner, w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = if (score >= 5) Color.parseColor("#39FF14") else Color.parseColor("#E63946") })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}
