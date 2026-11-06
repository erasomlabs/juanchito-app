package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class ContraGame : Game {
    override val name = "Contra 3"
    override val genre = "Run & Gun"
    private var w = 0; private var h = 0
    private val groundY get() = h * 0.8f
    private var px = 80f; private var py = 0f
    private var vy = 0f; private var onGround = true
    private var facing = 1 // 1 right, -1 left
    private var scrollX = 0f
    private val bullets = mutableListOf<Bullet>()
    private val enemies = mutableListOf<Enemy>()
    private var spawnTimer = 0f
    private var lives = 3
    private var invuln = 0f
    private var fireCooldown = 0f
    private var crouching = false
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false
    private var distance = 0f
    private val targetDistance = 3000f

    private data class Bullet(var x: Float, var y: Float, val vx: Float)
    private data class Enemy(var x: Float, var y: Float, var vx: Float, val type: Int)

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val bulletPaint = Paint().apply { color = Color.parseColor("#00D9FF"); isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = 80f; py = groundY - 40f; vy = 0f; onGround = true; facing = 1
        scrollX = 0f; bullets.clear(); enemies.clear(); spawnTimer = 0f
        lives = 3; invuln = 0f; fireCooldown = 0f; score = 0; distance = 0f; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        // movement
        if (input.left) { px -= 200f * dt; facing = -1 }
        if (input.right) { px += 200f * dt; facing = 1 }
        px = px.coerceIn(20f, w * 0.5f)
        // crouch
        crouching = input.down && onGround
        // jump
        if (input.b && onGround) { vy = -700f; onGround = false; input.b = false }
        vy += 1600f * dt
        py += vy * dt
        if (py >= groundY - 40f) { py = groundY - 40f; vy = 0f; onGround = true }
        // fire
        if (input.a && fireCooldown <= 0f) {
            val by = if (crouching) py + 20f else py + 10f
            bullets.add(Bullet(px + facing * 20f, by, facing * 700f))
            fireCooldown = 0.15f
        }
        // scroll
        scrollX += 60f * dt; distance += 60f * dt
        // update bullets
        bullets.removeAll { it.x < -20f || it.x > w + 20f }
        for (b in bullets) b.x += b.vx * dt
        // spawn enemies
        spawnTimer += dt
        if (spawnTimer > 1.5f) {
            spawnTimer = 0f
            val ey = if (Random.nextFloat() < 0.5f) groundY - 30f else groundY - 80f
            enemies.add(Enemy(w + 20f, ey, -80f, Random.nextInt(2)))
        }
        // update enemies
        val ei = enemies.iterator()
        while (ei.hasNext()) {
            val e = ei.next()
            e.x += e.vx * dt
            if (e.x < -40f) { ei.remove(); continue }
            // bullet collision
            val hit = bullets.any { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
            if (hit) {
                bullets.removeAll { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
                score += 10; if (score > highScore) highScore = score; ei.remove(); continue
            }
            // player collision
            if (invuln <= 0f && kotlin.math.abs(e.x - px) < 24f && kotlin.math.abs(e.y - py) < 30f) {
                lives--; invuln = 2f
                if (lives <= 0) isGameOver = true
            }
        }
        if (distance >= targetDistance) { won = true; isGameOver = true; score += 500 }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        // ground
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        // player
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            val ph = if (crouching) 24f else 40f
            val pyDraw = if (crouching) py + 16f else py
            canvas.drawRoundRect(RectF(px - 12f, pyDraw, px + 12f, pyDraw + ph), 4f, 4f, playerPaint)
        }
        // bullets
        for (b in bullets) canvas.drawCircle(b.x, b.y, 4f, bulletPaint)
        // enemies
        for (e in enemies) canvas.drawRoundRect(RectF(e.x - 12f, e.y - 12f, e.x + 12f, e.y + 12f), 4f, 4f, enemyPaint)
        // HUD
        textPaint.textSize = 22f
        canvas.drawText("Score: $score  Vidas: $lives", w/2f, 30f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Dist: ${distance.toInt()}/${targetDistance.toInt()}", 20f, 30f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡MISIÓN CUMPLIDA!" else "GAME OVER"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 48f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}