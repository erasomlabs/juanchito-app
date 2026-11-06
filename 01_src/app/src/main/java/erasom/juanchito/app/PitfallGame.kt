package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class PitfallGame : Game {
    override val name = "Pitfall"
    override val genre = "Plataformas"
    private var w = 0; private var h = 0
    private val groundY get() = h * 0.75f
    private var playerX = 0f; private var playerY = 0f
    private var vy = 0f; private var onGround = true
    private var scrollX = 0f; private val scrollSpeed = 120f
    private var timeLeft = 120f // 2 min simplified
    private val gravity = 1400f; private val jumpV = -650f
    private val playerW = 24f; private val playerH = 36f
    private val obstacles = mutableListOf<Obstacle>()
    private var spawnTimer = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var treasures = 0

    private data class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float, val type: Int) // 0 pit, 1 log, 2 snake, 3 treasure

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val pitPaint = Paint().apply { color = Color.parseColor("#1A1A2E"); isAntiAlias = true }
    private val logPaint = Paint().apply { color = Color.parseColor("#A29BFE"); isAntiAlias = true }
    private val snakePaint = Paint().apply { color = Color.parseColor("#55EFC4"); isAntiAlias = true }
    private val treasurePaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        playerX = w * 0.2f; playerY = groundY - playerH
        vy = 0f; onGround = true; scrollX = 0f; timeLeft = 120f
        obstacles.clear(); spawnTimer = 0f; score = 0; treasures = 0; isGameOver = false
    }

    private fun spawnObstacle() {
        val type = Random.nextInt(4)
        val x = w + 40f
        when (type) {
            0 -> obstacles.add(Obstacle(x, groundY, 60f, 40f, 0)) // pit (gap)
            1 -> obstacles.add(Obstacle(x, groundY - 20f, 40f, 20f, 1)) // log
            2 -> obstacles.add(Obstacle(x, groundY - 24f, 30f, 24f, 2)) // snake
            3 -> obstacles.add(Obstacle(x, groundY - 40f, 20f, 20f, 3)) // treasure
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        timeLeft -= dt
        if (timeLeft <= 0f) { isGameOver = true; return }
        // move player
        if (input.left) playerX -= 200f * dt
        if (input.right) playerX += 200f * dt
        playerX = playerX.coerceIn(40f, w * 0.4f)
        // jump
        if (input.a && onGround) { vy = jumpV; onGround = false; input.a = false }
        vy += gravity * dt
        playerY += vy * dt
        if (playerY >= groundY - playerH) { playerY = groundY - playerH; vy = 0f; onGround = true }
        // scroll obstacles
        scrollX += scrollSpeed * dt
        val it = obstacles.iterator()
        while (it.hasNext()) {
            val o = it.next()
            // update position by scroll
        }
        // move obstacles left
        for (i in obstacles.indices) {
            val o = obstacles[i]
            obstacles[i] = o.copy(x = o.x - scrollSpeed * dt)
        }
        obstacles.removeAll { it.x + it.w < 0f }
        // spawn
        spawnTimer += dt
        if (spawnTimer > 1.2f) { spawnObstacle(); spawnTimer = 0f }
        // collisions
        val playerRect = RectF(playerX, playerY, playerX + playerW, playerY + playerH)
        for (o in obstacles) {
            val r = RectF(o.x, o.y - o.h, o.x + o.w, o.y)
            if (RectF.intersects(playerRect, r)) {
                when (o.type) {
                    0 -> { isGameOver = true } // pit
                    1, 2 -> { isGameOver = true } // log/snake
                    3 -> { score += 50; treasures++; obstacles.remove(o) }
                }
            }
        }
        score = treasures * 50 + (120f - timeLeft).toInt() * 2
        if (score > highScore) highScore = score
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        // ground
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        // obstacles
        for (o in obstacles) {
            val r = RectF(o.x, o.y - o.h, o.x + o.w, o.y)
            when (o.type) {
                0 -> canvas.drawRect(o.x, groundY, o.x + o.w, h.toFloat(), pitPaint)
                1 -> canvas.drawRoundRect(r, 4f, 4f, logPaint)
                2 -> canvas.drawOval(r, snakePaint)
                3 -> canvas.drawCircle(o.x + o.w/2, o.y - o.h/2, o.w/2, treasurePaint)
            }
        }
        // player
        canvas.drawRoundRect(RectF(playerX, playerY, playerX + playerW, playerY + playerH), 4f, 4f, playerPaint)
        // HUD
        textPaint.textSize = 24f
        canvas.drawText("Score: $score", w/2f, 30f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Tiempo: ${timeLeft.toInt()}s", 20f, 30f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            canvas.drawText("GAME OVER", w/2f, h/2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#FF6B6B") })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}