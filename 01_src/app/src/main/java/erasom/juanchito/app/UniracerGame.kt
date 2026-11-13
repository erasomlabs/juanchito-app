package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class UniracerGame : Game {
    override val name = "Cycle Dash"
    override val genre = "Carreras"
    private var w = 0; private var h = 0
    private var px = 100f; private var py = 0f
    private var vy = 0f; private var onGround = true
    private var speed = 300f
    override var score = 0
    private var distance = 0f
    private val target = 2000f
    private var tricks = 0
    private var boostTimer = 0f
    private val obstacles = mutableListOf<Obstacle>()
    private var spawnTimer = 0f
    override var isGameOver = false
    private var highScore = 0
    private var won = false

    private data class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float, val type: Int) // 0 ramp, 1 barrier

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val rampPaint = Paint().apply { color = Color.parseColor("#55EFC4"); isAntiAlias = true }
    private val barrierPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val groundY get() = h * 0.8f

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = 100f; py = groundY - 30f; vy = 0f; onGround = true; speed = 300f
        score = 0; distance = 0f; tricks = 0; boostTimer = 0f
        obstacles.clear(); spawnTimer = 0f; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        // accelerate
        if (input.right) speed = (speed + 200f * dt).coerceAtMost(600f)
        if (input.left) speed = (speed - 200f * dt).coerceAtLeast(150f)
        boostTimer = (boostTimer - dt).coerceAtLeast(0f)
        val curSpeed = if (boostTimer > 0f) speed * 1.5f else speed
        distance += curSpeed * dt
        // scroll obstacles
        for (i in obstacles.indices) obstacles[i] = obstacles[i].copy(x = obstacles[i].x - curSpeed * dt)
        obstacles.removeAll { it.x + it.w < 0f }
        // spawn
        spawnTimer += dt
        if (spawnTimer > 1.0f) {
            spawnTimer = 0f
            val type = if (Random.nextFloat() < 0.6f) 0 else 1
            val oh = if (type == 0) 20f else 30f
            obstacles.add(Obstacle(w + 20f, groundY - oh, 40f, oh, type))
        }
        // physics
        if (input.a && onGround) { vy = -500f; onGround = false; input.a = false }
        // trick in air
        if (!onGround && input.b) {
            tricks++; score += 25; boostTimer = 1.5f; input.b = false
        }
        vy += 1200f * dt
        py += vy * dt
        if (py >= groundY - 30f) { py = groundY - 30f; vy = 0f; onGround = true }
        // collisions
        for (o in obstacles) {
            val r = RectF(o.x, o.y - o.h, o.x + o.w, o.y)
            val pr = RectF(px - 12f, py - 15f, px + 12f, py + 15f)
            if (RectF.intersects(pr, r)) {
                if (o.type == 0) {
                    // ramp: boost
                    if (!onGround) { tricks++; score += 25; boostTimer = 1.5f }
                } else {
                    isGameOver = true
                }
            }
        }
        score = (distance / 10).toInt() + tricks * 25
        if (score > highScore) highScore = score
        if (distance >= target) { won = true; isGameOver = true; score += 500 }
    }

    override fun draw(canvas: Canvas) {
        // Atardecer NES con colinas y nubes
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#F8A860", "#5C94FC", 6)
        NesScenery.drawSun(canvas, w - 80f, 60f, 30f)
        NesScenery.drawClouds(canvas, w, h * 0.28f)
        NesScenery.drawHills(canvas, w, groundY, "#00A800", "#008800")
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        for (o in obstacles) {
            val r = RectF(o.x, o.y - o.h, o.x + o.w, o.y)
            if (o.type == 0) canvas.drawRoundRect(r, 4f, 4f, rampPaint) else canvas.drawRoundRect(r, 4f, 4f, barrierPaint)
        }
        // Pixel sobre su monociclo
        canvas.drawCircle(px, py, 14f, playerPaint)
        canvas.drawRect(px - 4f, py - 28f, px + 4f, py - 14f, playerPaint)
        PixelSprite.draw(canvas, px, py - 42f, 26f, facingRight = true)
        // HUD
        textPaint.textSize = 22f
        canvas.drawText("Score: $score  Trucos: $tricks", w/2f, 30f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Dist: ${distance.toInt()}/${target.toInt()}", 20f, 30f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 48f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}