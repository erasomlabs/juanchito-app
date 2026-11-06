package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class KidClownGame : Game {
    override val name = "Kid the Clown"
    override val genre = "Plataformas"
    private var w = 0; private var h = 0
    private val groundY get() = h * 0.8f
    private var px = 80f; private var py = 0f
    private var vy = 0f; private var onGround = true
    private var facing = 1
    private val balls = mutableListOf<Ball>()
    private val enemies = mutableListOf<Enemy>()
    private val items = mutableListOf<Item>()
    private var spawnTimer = 0f
    private var lives = 3
    private var fireCooldown = 0f
    private var invuln = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false
    private var distance = 0f
    private val target = 2500f

    private data class Ball(var x: Float, var y: Float, val vx: Float)
    private data class Enemy(var x: Float, var y: Float, var vx: Float)
    private data class Item(var x: Float, var y: Float)

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val ballPaint = Paint().apply { color = Color.parseColor("#00D9FF"); isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val itemPaint = Paint().apply { color = Color.parseColor("#55EFC4"); isAntiAlias = true }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = 80f; py = groundY - 30f; vy = 0f; onGround = true; facing = 1
        balls.clear(); enemies.clear(); items.clear(); spawnTimer = 0f
        lives = 3; fireCooldown = 0f; invuln = 0f; score = 0; distance = 0f; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        if (input.left) { px -= 200f * dt; facing = -1 }
        if (input.right) { px += 200f * dt; facing = 1 }
        px = px.coerceIn(20f, w * 0.5f)
        if (input.b && onGround) { vy = -600f; onGround = false; input.b = false }
        vy += 1400f * dt
        py += vy * dt
        if (py >= groundY - 30f) { py = groundY - 30f; vy = 0f; onGround = true }
        if (input.a && fireCooldown <= 0f) {
            balls.add(Ball(px + facing * 16f, py, facing * 500f)); fireCooldown = 0.3f
        }
        balls.removeAll { it.x < -20f || it.x > w + 20f }
        for (b in balls) b.x += b.vx * dt
        // scroll
        val scroll = 80f * dt
        distance += scroll
        for (e in enemies) e.x -= scroll
        for (it in items) it.x -= scroll
        enemies.removeAll { it.x < -40f }
        items.removeAll { it.x < -20f }
        // spawn
        spawnTimer += dt
        if (spawnTimer > 1.5f) {
            spawnTimer = 0f
            enemies.add(Enemy(w + 20f, groundY - 30f, -60f))
            if (Random.nextFloat() < 0.4f) items.add(Item(w + 20f, groundY - 50f))
        }
        // enemy update
        val ei = enemies.iterator()
        while (ei.hasNext()) {
            val e = ei.next()
            e.x += e.vx * dt
            // ball hit
            if (balls.any { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }) {
                balls.removeAll { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
                score += 10; if (score > highScore) highScore = score; ei.remove(); continue
            }
            // player collision
            if (invuln <= 0f && kotlin.math.abs(e.x - px) < 24f && kotlin.math.abs(e.y - py) < 30f) {
                lives--; invuln = 2f
                if (lives <= 0) isGameOver = true
            }
        }
        // items
        items.removeAll { it.x < -20f }
        val ii = items.iterator()
        while (ii.hasNext()) {
            val it = ii.next()
            if (kotlin.math.abs(it.x - px) < 20f && kotlin.math.abs(it.y - py) < 30f) {
                score += 20; if (score > highScore) highScore = score; ii.remove()
            }
        }
        score = score.coerceAtLeast((distance / 10).toInt())
        if (distance >= target) { won = true; isGameOver = true; score += 500 }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        for (it in items) canvas.drawCircle(it.x, it.y, 8f, itemPaint)
        for (e in enemies) canvas.drawRoundRect(RectF(e.x - 12f, e.y - 14f, e.x + 12f, e.y + 14f), 4f, 4f, enemyPaint)
        for (b in balls) canvas.drawCircle(b.x, b.y, 5f, ballPaint)
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            canvas.drawRoundRect(RectF(px - 12f, py - 16f, px + 12f, py + 16f), 4f, 4f, playerPaint)
        }
        textPaint.textSize = 22f
        canvas.drawText("Score: $score  Vidas: $lives", w/2f, 30f, textPaint)
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