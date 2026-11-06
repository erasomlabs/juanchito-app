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
    private val groundY get() = h * 0.82f
    private var px = 80f; private var py = 0f
    private var vy = 0f; private var onGround = true
    private var facing = 1
    private var crouching = false
    private var lives = 3
    private var continues = 3
    private var invuln = 0f
    private var fireCooldown = 0f
    private var weapon = 0 // 0 machine,1 spread,2 laser,3 homing,4 flame
    private var bullets = mutableListOf<Bullet>()
    private var enemies = mutableListOf<Enemy>()
    private var pickups = mutableListOf<Pickup>()
    private var boss: Boss? = null
    private var level = 1
    private var scrollX = 0f
    private var distance = 0f
    private var levelLen = 4000f
    private var spawnTimer = 0f
    private var bossActive = false
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false

    private data class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float, val type: Int, var life: Float)
    private data class Enemy(var x: Float, var y: Float, var vx: Float, val type: Int, var hp: Int, var shootTimer: Float)
    private data class Pickup(var x: Float, var y: Float, val type: Int)
    private data class Boss(var x: Float, var y: Float, var hp: Int, var maxHp: Int, var shootTimer: Float, val type: Int)

    private val playerPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val bulletPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val spreadPaint = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
    private val laserPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val turretPaint = Paint().apply { color = Color.parseColor("#8814A8"); isAntiAlias = false }
    private val bossPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val pickupPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 14f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = 80f; py = groundY - 40f; vy = 0f; onGround = true; facing = 1; crouching = false
        lives = 3; continues = 3; invuln = 0f; fireCooldown = 0f; weapon = 0
        bullets.clear(); enemies.clear(); pickups.clear(); boss = null; bossActive = false
        level = 1; scrollX = 0f; distance = 0f; levelLen = 4000f; spawnTimer = 0f
        isGameOver = false; won = false; score = 0
    }

    private fun resetLevel() {
        bullets.clear(); enemies.clear(); pickups.clear(); boss = null; bossActive = false
        distance = 0f; spawnTimer = 0f; px = 80f; py = groundY - 40f
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        crouching = input.down && onGround

        // movement
        if (input.left) { px -= 200f * dt; facing = -1 }
        if (input.right) { px += 200f * dt; facing = 1 }
        px = px.coerceIn(20f, w * 0.45f)
        if (input.b && onGround) { vy = -650f; onGround = false; input.b = false }
        vy += 1600f * dt
        py += vy * dt
        if (py >= groundY - 40f) { py = groundY - 40f; vy = 0f; onGround = true }

        // fire
        if (input.a && fireCooldown <= 0f) {
            fireCooldown = 0.12f
            val by = if (crouching) py + 20f else py + 10f
            when (weapon) {
                0 -> bullets.add(Bullet(px + facing * 16f, by, facing * 600f, 0f, 0, 1f))
                1 -> for (ang in listOf(-0.3f, 0f, 0.3f)) bullets.add(Bullet(px + facing * 16f, by, facing * 500f * kotlin.math.cos(ang), 500f * kotlin.math.sin(ang), 1, 1f))
                2 -> bullets.add(Bullet(px + facing * 16f, by, facing * 900f, 0f, 2, 0.4f))
                3 -> bullets.add(Bullet(px + facing * 16f, by, facing * 400f, 0f, 3, 2f))
                4 -> bullets.add(Bullet(px + facing * 16f, by, facing * 300f, 0f, 4, 0.6f))
            }
        }

        // scroll
        if (!bossActive) {
            scrollX += 80f * dt; distance += 80f * dt
            if (distance >= levelLen) { bossActive = true; spawnBoss() }
        }

        // update bullets
        bullets.removeAll { it.x < -20f || it.x > w + 20f || it.life <= 0f }
        for (b in bullets) { b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt }

        // spawn enemies
        if (!bossActive) {
            spawnTimer += dt
            if (spawnTimer > 1.8f) {
                spawnTimer = 0f
                val ey = if (Random.nextFloat() < 0.5f) groundY - 30f else groundY - 70f
                enemies.add(Enemy(w + 20f, ey, -100f, Random.nextInt(3), 1, Random.nextFloat() * 2f + 1f))
                // pickup chance
                if (Random.nextFloat() < 0.25f) {
                    val pt = Random.nextInt(5)
                    pickups.add(Pickup(w + 20f, ey - 20f, pt))
                }
            }
        }

        // update enemies
        val ei = enemies.iterator()
        while (ei.hasNext()) {
            val e = ei.next()
            e.x += e.vx * dt
            e.shootTimer -= dt
            if (e.x < -40f) { ei.remove(); continue }
            // shoot at player
            if (e.shootTimer <= 0f && e.type == 2) {
                e.shootTimer = 2f
                val dx = px - e.x; val dy = py - e.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                if (len > 0f) bullets.add(Bullet(e.x, e.y, dx / len * 300f, dy / len * 300f, 5, 3f))
            }
            // bullet hit
            val hit = bullets.any { b -> b.type < 5 && kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
            if (hit) {
                bullets.removeAll { b -> b.type < 5 && kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
                e.hp--
                if (e.hp <= 0) {
                    score += 10 * (e.type + 1); if (score > highScore) highScore = score; ei.remove()
                    // weapon pickup chance
                    if (Random.nextFloat() < 0.3f) pickups.add(Pickup(e.x, e.y, Random.nextInt(5)))
                }
            }
            // player collision
            if (invuln <= 0f && kotlin.math.abs(e.x - px) < 24f && kotlin.math.abs(e.y - py) < 30f) {
                lives--; invuln = 2f
                if (lives <= 0) {
                    if (continues > 0) { continues--; lives = 3; resetLevel() } else isGameOver = true
                }
            }
        }

        // update pickups
        val pi = pickups.iterator()
        while (pi.hasNext()) {
            val p = pi.next()
            p.x -= 80f * dt
            if (p.x < -20f) { pi.remove(); continue }
            if (kotlin.math.abs(p.x - px) < 24f && kotlin.math.abs(p.y - py) < 30f) {
                weapon = p.type; score += 50; pi.remove()
            }
        }

        // boss
        if (bossActive) {
            val b = boss
            if (b != null) {
                b.shootTimer -= dt
                if (b.shootTimer <= 0f) {
                    b.shootTimer = 1.5f
                    for (i in -2..2) bullets.add(Bullet(b.x, b.y + 20f, (i * 0.3f) * 200f - 100f, 300f, 5, 3f))
                }
                // bullet hit boss
                val hit = bullets.count { bb -> bb.type < 5 && kotlin.math.abs(bb.x - b.x) < 40f && kotlin.math.abs(bb.y - b.y) < 40f }
                if (hit > 0) {
                    bullets.removeAll { bb -> bb.type < 5 && kotlin.math.abs(bb.x - b.x) < 40f && kotlin.math.abs(bb.y - b.y) < 40f }
                    b.hp -= hit
                    if (b.hp <= 0) {
                        score += 1000; if (score > highScore) highScore = score
                        if (level >= 4) { won = true; isGameOver = true }
                        else { level++; resetLevel() }
                    }
                }
                // boss collision with player
                if (invuln <= 0f && kotlin.math.abs(b.x - px) < 40f && kotlin.math.abs(b.y - py) < 40f) {
                    lives--; invuln = 2f
                    if (lives <= 0) {
                        if (continues > 0) { continues--; lives = 3; resetLevel() } else isGameOver = true
                    }
                }
            }
        }
    }

    private fun spawnBoss() {
        bossActive = true
        boss = Boss(w / 2f, 60f, 40 + level * 10, 40 + level * 10, 1.5f, level)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#000000"))
        // ground
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)
        // player
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            val ph = if (crouching) 24f else 40f
            val pyd = if (crouching) py + 16f else py
            canvas.drawRoundRect(RectF(px - 12f, pyd, px + 12f, pyd + ph), 3f, 3f, playerPaint)
            // facing indicator
            if (facing > 0) canvas.drawRect(px + 10f, pyd + 8f, px + 16f, pyd + 12f, playerPaint)
            else canvas.drawRect(px - 16f, pyd + 8f, px - 10f, pyd + 12f, playerPaint)
        }
        // bullets
        for (b in bullets) {
            val p = when (b.type) { 1 -> spreadPaint; 2 -> laserPaint; else -> bulletPaint }
            canvas.drawCircle(b.x, b.y, if (b.type == 2) 3f else 4f, p)
        }
        // pickups
        for (p in pickups) {
            pickupPaint.color = when (p.type) { 0 -> Color.parseColor("#D82800"); 1 -> Color.parseColor("#FC9838"); 2 -> Color.parseColor("#5C94FC"); 3 -> Color.parseColor("#00A800"); else -> Color.parseColor("#F8D878") }
            canvas.drawCircle(p.x, p.y, 8f, pickupPaint)
        }
        // enemies
        for (e in enemies) {
            val p = if (e.type == 1) turretPaint else enemyPaint
            canvas.drawRoundRect(RectF(e.x - 12f, e.y - 12f, e.x + 12f, e.y + 12f), 3f, 3f, p)
        }
        // boss
        val b = boss
        if (b != null && bossActive) {
            canvas.drawRoundRect(RectF(b.x - 40f, b.y - 30f, b.x + 40f, b.y + 30f), 6f, 6f, bossPaint)
            // hp bar
            val hpPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
            canvas.drawRect(b.x - 40f, b.y - 40f, b.x - 40f + 80f * (b.hp.toFloat() / b.maxHp), b.y - 34f, hpPaint)
        }
        // weapon indicator
        val wname = arrayOf("M", "S", "L", "H", "F")[weapon]
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Arma: $wname", 8f, h - 8f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        // HUD
        textPaint.textSize = 12f
        canvas.drawText("Score: $score  Vidas: $lives  Continues: $continues  Nivel: $level", w / 2f, 14f, textPaint)
        if (isGameOver) {
            val msg = if (won) "¡MISIÓN CUMPLIDA!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 32f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 12f; textPaint.color = Color.parseColor("#F8F8F8")
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 24f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}