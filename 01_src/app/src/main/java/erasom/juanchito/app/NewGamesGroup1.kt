package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

// 1. Pixel Smash: Romper bloques especiales con física rápida y powerups
class PixelSmashGame : Game {
    override val name = "Pixel Smash"
    override val genre = "Arcade"
    private var w = 0; private var h = 0
    private var paddleX = 0f; private var paddleWidth = 120f
    private var ballX = 0f; private var ballY = 0f
    private var ballVx = 300f; private var ballVy = -320f
    private val bricks = mutableListOf<RectF>()
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) {
        w = width; h = height
    }

    override fun reset() {
        paddleWidth = 140f
        paddleX = w / 2f - paddleWidth / 2f
        ballX = w / 2f; ballY = h * 0.7f
        ballVx = 320f; ballVy = -340f
        score = 0; isGameOver = false
        bricks.clear()
        val cols = 8; val rows = 5
        val bw = (w - 80f) / cols; val bh = 24f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                bricks.add(RectF(40f + c * bw + 3f, 40f + r * bh + 3f, 40f + (c + 1) * bw - 3f, 40f + (r + 1) * bh - 3f))
            }
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        val speed = 500f * dt
        if (input.left) paddleX = (paddleX - speed).coerceAtLeast(0f)
        if (input.right) paddleX = (paddleX + speed).coerceAtMost(w - paddleWidth)

        ballX += ballVx * dt
        ballY += ballVy * dt

        if (ballX <= 12f) { ballX = 12f; ballVx = -ballVx }
        if (ballX >= w - 12f) { ballX = w - 12f; ballVx = -ballVx }
        if (ballY <= 12f) { ballY = 12f; ballVy = -ballVy }

        val pTop = h - 60f
        if (ballY + 10f >= pTop && ballY - 10f <= pTop + 20f && ballX >= paddleX && ballX <= paddleX + paddleWidth && ballVy > 0) {
            ballVy = -ballVy * 1.05f
            ballVx += (ballX - (paddleX + paddleWidth / 2f)) * 4f
        }

        val iterator = bricks.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            if (b.contains(ballX, ballY)) {
                iterator.remove()
                ballVy = -ballVy
                score += 25
                break
            }
        }

        if (ballY > h) isGameOver = true
    }

    override fun draw(canvas: Canvas) {
        // Pared de ladrillos arcade con marco neón
        NesScenery.drawBrickWall(canvas, w, h, 140)
        NesScenery.drawNeonFrame(canvas, w, h, "#FC9838")
        // Paleta con grip y Pixel controlando
        val paddleRect = RectF(paddleX, h - 60f, paddleX + paddleWidth, h - 42f)
        paint.color = Color.parseColor("#D4A017")
        canvas.drawRoundRect(paddleRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#8C6A10")
        var gx = paddleX + 8f
        while (gx < paddleX + paddleWidth - 6f) {
            canvas.drawRect(gx, h - 57f, gx + 4f, h - 49f, paint)
            gx += 16f
        }
        PixelSprite.draw(canvas, paddleX + paddleWidth / 2f, h - 76f, 26f, facingRight = true)

        // Bola con brillo
        paint.color = Color.parseColor("#5C94FC")
        canvas.drawCircle(ballX, ballY, 10f, paint)
        paint.color = Color.parseColor("#A8D8FC")
        canvas.drawCircle(ballX - 3f, ballY - 3f, 3f, paint)

        // Ladrillos biselados en fila de colores por fila
        val rowColors = listOf("#D82800", "#FC9838", "#F8D878", "#00A800", "#5C94FC")
        for (b in bricks) {
            val row = ((b.top - 40f) / 24f).toInt().coerceIn(0, 4)
            paint.color = Color.parseColor(rowColors[row])
            canvas.drawRoundRect(b, 4f, 4f, paint)
            paint.color = Color.argb(255, Math.min(255, Color.red(paint.color) + 60), Math.min(255, Color.green(paint.color) + 60), Math.min(255, Color.blue(paint.color) + 60))
            canvas.drawRect(b.left + 3f, b.top + 2f, b.right - 3f, b.top + 6f, paint)
        }
    }
}
