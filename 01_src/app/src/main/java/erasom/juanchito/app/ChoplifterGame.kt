package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class ChoplifterGame : Game {
    override val name = "Choplifter 3"
    override val genre = "Acción"
    private var w = 0; private var h = 0
    private var hx = 100f; private var hy = 100f
    private var vx = 0f; private var vy = 0f
    private val bullets = mutableListOf<Bullet>()
    private val bombs = mutableListOf<Bomb>()
    private val enemies = mutableListOf<Enemy>()
    private var hostages = 0; private var rescued = 0
    private var fireCooldown = 0f; private var bombCooldown = 0f
    private var spawnTimer = 0f
    private var lives = 3
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false

    private data class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float)
    private data class Bomb(var x: Float, var y: Float, var vy: Float)
    private data class Enemy(var x: Float, var y: Float, var vx: Float)

    private val heliPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val bulletPaint = Paint().apply { color = Color.parseColor("#00D9FF"); isAntiAlias = true }
    private val bombPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#55EFC4"); isAntiAlias = true }
    private val hostagePaint = Paint().apply { color = Color.parseColor("#48DBFB"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        hx = w/2f; hy = h/3f; vx = 0f; vy = 0f
        bullets.clear(); bombs.clear(); enemies.clear(); hostages = 5; rescued = 0
        fireCooldown = 0f; bombCooldown = 0f; spawnTimer = 0f; lives = 3; score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        // movement
        if (input.left) vx -= 400f * dt
        if (input.right) vx += 400f * dt
        if (input.up) vy -= 400f * dt
        if (input.down) vy += 400f * dt
        vx *= 0.9f; vy *= 0.9f
        hx += vx * dt; hy += vy * dt
        hx = hx.coerceIn(30f, w - 30f); hy = hy.coerceIn(30f, h - 60f)
        fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        bombCooldown = (bombCooldown - dt).coerceAtLeast(0f)
        // fire
        if (input.a && fireCooldown <= 0f) {
            bullets.add(Bullet(hx + 20f, hy, 500f, 0f)); fireCooldown = 0.2f
        }
        // bomb
        if (input.b && bombCooldown <= 0f) {
            bombs.add(Bomb(hx, hy + 20f, 200f)); bombCooldown = 0.5f
        }
        // update bullets
        bullets.removeAll { it.x < -20f || it.x > w + 20f }
        for (b in bullets) { b.x += b.vx*dt; b.y += b.vy*dt }
        // update bombs
        bombs.removeAll { it.y > h }
        for (b in bombs) b.y += b.vy*dt
        // spawn enemies
        spawnTimer += dt
        if (spawnTimer > 2f) {
            spawnTimer = 0f
            enemies.add(Enemy(w + 20f, Random.nextFloat() * (h - 100f) + 50f, -100f))
        }
        // update enemies
        val ei = enemies.iterator()
        while (ei.hasNext()) {
            val e = ei.next()
            e.x += e.vx * dt
            if (e.x < -40f) { ei.remove(); continue }
            // bullet hit
            if (bullets.any { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }) {
                bullets.removeAll { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
                score += 20; if (score > highScore) highScore = score; ei.remove(); continue
            }
            // bomb hit
            if (bombs.any { b -> kotlin.math.abs(b.x - e.x) < 30f && kotlin.math.abs(b.y - e.y) < 30f }) {
                score += 20; if (score > highScore) highScore = score; ei.remove(); continue
            }
        }
        // rescue hostages (simplified: auto when heli near ground left area)
        if (hy > h - 80f && hx < 80f && hostages > 0) {
            hostages--; rescued++; score += 50; if (score > highScore) highScore = score
        }
        if (rescued >= 5) { won = true; isGameOver = true; score += 500 }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        // ground
        val groundPaint = Paint().apply { color = Color.parseColor("#2D3748") }
        canvas.drawRect(0f, h - 40f, w.toFloat(), h.toFloat(), groundPaint)
        // heli
        canvas.drawRoundRect(RectF(hx - 20f, hy - 10f, hx + 20f, hy + 10f), 4f, 4f, heliPaint)
        canvas.drawRect(hx + 15f, hy - 14f, hx + 25f, hy - 6f, heliPaint)
        // bullets
        for (b in bullets) canvas.drawCircle(b.x, b.y, 3f, bulletPaint)
        // bombs
        for (b in bombs) canvas.drawCircle(b.x, b.y, 5f, bombPaint)
        // enemies
        for (e in enemies) canvas.drawRoundRect(RectF(e.x - 12f, e.y - 8f, e.x + 12f, e.y + 8f), 3f, 3f, enemyPaint)
        // hostages indicator
        for (i in 0 until hostages) canvas.drawCircle(40f + i * 16f, h - 20f, 5f, hostagePaint)
        // HUD
        textPaint.textSize = 22f
        canvas.drawText("Score: $score  Vidas: $lives  Rescatados: $rescued/5", w/2f, 30f, textPaint)
        if (isGameOver) {
            val msg = if (won) "¡MISIÓN CUMPLIDA!" else "GAME OVER"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 48f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}