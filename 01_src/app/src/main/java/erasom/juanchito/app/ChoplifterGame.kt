package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class ChoplifterGame : Game {
    override val name = "Helirescate"
    override val genre = "Acción"
    private var w = 0; private var h = 0
    private var hx = 100f; private var hy = 100f
    private var vx = 0f; private var vy = 0f
    private val bullets = mutableListOf<Bullet>()
    private val bombs = mutableListOf<Bomb>()
    private val enemies = mutableListOf<Enemy>()
    private val hostages = mutableListOf<Hostage>()
    private var rescued = 0
    private val targetRescue = 16
    private var fuel = 100f
    private var fireCooldown = 0f; private var bombCooldown = 0f
    private var spawnTimer = 0f
    private var level = 1
    private var lives = 3
    private var invuln = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false

    private data class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float)
    private data class Bomb(var x: Float, var y: Float, var vy: Float)
    private data class Enemy(var x: Float, var y: Float, var type: Int, var vx: Float, var shootTimer: Float)
    private data class Hostage(var x: Float, var y: Float, var state: Int) // 0 on ground,1 aboard

    private val heliPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val bulletPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val bombPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val tankPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val jetPaint = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
    private val hostagePaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val basePaint = Paint().apply { color = Color.parseColor("#8814A8"); isAntiAlias = false }
    private val groundPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        hx = w / 2f; hy = h / 3f; vx = 0f; vy = 0f
        bullets.clear(); bombs.clear(); enemies.clear(); hostages.clear()
        for (i in 0 until 8) hostages.add(Hostage(60f + i * 40f, h - 30f, 0))
        rescued = 0; fuel = 100f; fireCooldown = 0f; bombCooldown = 0f; spawnTimer = 0f
        level = 1; lives = 3; invuln = 0f; score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        fuel = (fuel - 2f * dt).coerceAtLeast(0f)
        if (fuel <= 0f) { lives--; fuel = 100f; if (lives <= 0) isGameOver = true }
        // movement
        if (input.left) vx -= 300f * dt
        if (input.right) vx += 300f * dt
        if (input.up) vy -= 300f * dt
        if (input.down) vy += 300f * dt
        vx *= 0.92f; vy *= 0.92f
        hx += vx * dt; hy += vy * dt
        hx = hx.coerceIn(30f, w - 30f); hy = hy.coerceIn(30f, h - 60f)
        fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        bombCooldown = (bombCooldown - dt).coerceAtLeast(0f)
        // fire
        if (input.a && fireCooldown <= 0f) { bullets.add(Bullet(hx + 20f, hy, 600f, 0f)); fireCooldown = 0.15f }
        // bomb
        if (input.b && bombCooldown <= 0f) { bombs.add(Bomb(hx, hy + 15f, 250f)); bombCooldown = 0.4f }
        // update bullets
        bullets.removeAll { it.x < -20f || it.x > w + 20f }
        for (b in bullets) { b.x += b.vx * dt; b.y += b.vy * dt }
        // update bombs
        bombs.removeAll { it.y > h }
        for (b in bombs) b.y += b.vy * dt
        // spawn enemies
        spawnTimer += dt
        if (spawnTimer > 2f) {
            spawnTimer = 0f
            val type = if (Random.nextFloat() < 0.5f) 0 else 1
            enemies.add(Enemy(if (type == 0) w + 20f else Random.nextFloat() * w, if (type == 0) h - 40f else 40f, type, if (type == 0) -80f else (if (Random.nextFloat() < 0.5f) 100f else -100f), Random.nextFloat() * 2f + 1f))
        }
        // update enemies
        val ei = enemies.iterator()
        while (ei.hasNext()) {
            val e = ei.next()
            e.x += e.vx * dt
            e.shootTimer -= dt
            if (e.x < -40f || e.x > w + 40f) { ei.remove(); continue }
            if (e.type == 1 && e.shootTimer <= 0f) {
                e.shootTimer = 2f
                val dx = hx - e.x; val dy = hy - e.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                if (len > 0f) bullets.add(Bullet(e.x, e.y, dx / len * 250f, dy / len * 250f))
            }
            // bullet hit
            if (bullets.any { b -> kotlin.math.abs(b.x - e.x) < 22f && kotlin.math.abs(b.y - e.y) < 18f }) {
                bullets.removeAll { b -> kotlin.math.abs(b.x - e.x) < 22f && kotlin.math.abs(b.y - e.y) < 18f }
                score += if (e.type == 0) 30 else 50; if (score > highScore) highScore = score; ei.remove(); continue
            }
            // bomb hit
            if (bombs.any { b -> kotlin.math.abs(b.x - e.x) < 35f && kotlin.math.abs(b.y - e.y) < 35f }) {
                score += if (e.type == 0) 30 else 50; if (score > highScore) highScore = score; ei.remove(); continue
            }
            // collision with heli
            if (invuln <= 0f && kotlin.math.abs(e.x - hx) < 28f && kotlin.math.abs(e.y - hy) < 20f) {
                lives--; invuln = 2f; fuel = 100f
                if (lives <= 0) isGameOver = true
            }
        }
        // rescue hostages: when heli near ground left base area
        if (hy > h - 70f && hx < 90f) {
            val aboard = hostages.count { it.state == 1 }
            if (aboard > 0) {
                rescued += aboard
                score += aboard * 100
                if (score > highScore) highScore = score
                hostages.removeAll { it.state == 1 }
                // spawn new hostages if needed
                if (hostages.size < 4 && rescued < targetRescue) {
                    for (i in 0 until 4) hostages.add(Hostage(60f + Random.nextFloat() * (w - 120f), h - 30f, 0))
                }
            }
        }
        // pickup hostages on ground
        for (ho in hostages) {
            if (ho.state == 0 && kotlin.math.abs(ho.x - hx) < 30f && kotlin.math.abs(ho.y - hy) < 40f && hy > h - 80f) {
                ho.state = 1
            }
        }
        // move hostages aboard (follow heli)
        for (ho in hostages) if (ho.state == 1) { ho.x = hx - 10f + Random.nextFloat() * 20f; ho.y = hy + 15f }
        // win
        if (rescued >= targetRescue) { won = true; isGameOver = true; score += 1000 }
    }

    override fun draw(canvas: Canvas) {
        // Atardecer en zona de guerra
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#F8A860", "#F8D878", 5)
        NesScenery.drawClouds(canvas, w, h * 0.22f)
        NesScenery.drawSun(canvas, w - 60f, 50f, 26f)
        // Suelo desierto con textura
        canvas.drawRect(0f, h - 40f, w.toFloat(), h.toFloat(), groundPaint)
        val dirt = Paint().apply { color = Color.parseColor("#7A5A2A"); isAntiAlias = false }
        var dx = 0f
        while (dx < w) {
            canvas.drawRect(dx, h - 22f, dx + 14f, h - 18f, dirt)
            dx += 46f
        }
        // Base militar: edificio con techo, bandera y helipuerto "H"
        canvas.drawRect(20f, h - 60f, 80f, h - 40f, basePaint)
        val roof = Paint().apply { color = Color.parseColor("#5A7D9A"); isAntiAlias = false }
        canvas.drawRect(16f, h - 64f, 84f, h - 58f, roof)
        val flag = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
        canvas.drawRect(46f, h - 76f, 48f, h - 60f, Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false })
        canvas.drawRect(48f, h - 76f, 58f, h - 70f, flag)
        val hpad = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
        canvas.drawRect(90f, h - 30f, 140f, h - 26f, hpad)
        canvas.drawRect(106f, h - 38f, 110f, h - 18f, hpad)
        canvas.drawRect(122f, h - 38f, 126f, h - 18f, hpad)
        canvas.drawRect(108f, h - 30f, 124f, h - 26f, hpad)
        // heli
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            // patines de aterrizaje
            val skid = Paint().apply { color = Color.parseColor("#5A7D9A"); isAntiAlias = false }
            canvas.drawRect(hx - 14f, hy + 10f, hx + 14f, hy + 12f, skid)
            canvas.drawRect(hx - 10f, hy + 6f, hx - 8f, hy + 10f, skid)
            canvas.drawRect(hx + 8f, hy + 6f, hx + 10f, hy + 10f, skid)
            // fuselaje y cola
            canvas.drawRoundRect(RectF(hx - 22f, hy - 10f, hx + 22f, hy + 10f), 4f, 4f, heliPaint)
            canvas.drawRect(hx + 18f, hy - 14f, hx + 28f, hy - 6f, heliPaint)
            // rotor de cola girando
            val tr = Paint().apply { color = Color.parseColor("#F8F8F8"); strokeWidth = 2f; isAntiAlias = false }
            canvas.drawLine(hx + 23f, hy - 16f, hx + 23f, hy - 4f, tr)
            // cabina de cristal
            val glass = Paint().apply { color = Color.parseColor("#A8D8FC"); isAntiAlias = false }
            canvas.drawRect(hx - 20f, hy - 8f, hx - 6f, hy + 2f, glass)
            // Pixel piloto en la cabina
            PixelSprite.draw(canvas, hx - 12f, hy - 10f, 14f, facingRight = false, hatColor = 0x5A7D9A)
            // rotor principal con doble línea (movimiento)
            val rp = Paint().apply { color = Color.parseColor("#F8F8F8"); strokeWidth = 2f; isAntiAlias = false }
            canvas.drawLine(hx - 30f, hy - 12f, hx + 30f, hy - 12f, rp)
            canvas.drawLine(hx - 22f, hy - 14f, hx + 22f, hy - 10f, rp)
            // mástil del rotor
            canvas.drawRect(hx - 2f, hy - 14f, hx + 2f, hy - 10f, Paint().apply { color = Color.parseColor("#1A1A1A"); isAntiAlias = false })
        }
        // bullets: trazadoras
        val tracer = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
        for (b in bullets) {
            canvas.drawCircle(b.x, b.y, 3f, bulletPaint)
            canvas.drawRect(b.x - 8f, b.y - 1f, b.x - 3f, b.y + 1f, tracer)
        }
        // bombs: bomba con aletas
        for (b in bombs) {
            canvas.drawCircle(b.x, b.y, 5f, bombPaint)
            val fin = Paint().apply { color = Color.parseColor("#5A7D9A"); isAntiAlias = false }
            canvas.drawRect(b.x - 5f, b.y + 5f, b.x - 2f, b.y + 10f, fin)
            canvas.drawRect(b.x + 2f, b.y + 5f, b.x + 5f, b.y + 10f, fin)
        }
        // enemies
        for (e in enemies) {
            if (e.type == 0) {
                // Tanque: casco, orugas con ruedas, torreta y cañón
                val treads = Paint().apply { color = Color.parseColor("#1A1A1A"); isAntiAlias = false }
                val wheel = Paint().apply { color = Color.parseColor("#4A4A4A"); isAntiAlias = false }
                val gun = Paint().apply { color = Color.parseColor("#2A2A2A"); isAntiAlias = false }
                canvas.drawRect(e.x - 14f, e.y + 2f, e.x + 14f, e.y + 8f, treads)
                var wxx = e.x - 12f
                while (wxx < e.x + 10f) {
                    canvas.drawCircle(wxx + 2f, e.y + 5f, 2f, wheel)
                    wxx += 6f
                }
                canvas.drawRoundRect(RectF(e.x - 12f, e.y - 4f, e.x + 12f, e.y + 2f), 2f, 2f, tankPaint)
                canvas.drawRect(e.x - 4f, e.y - 8f, e.x + 4f, e.y - 3f, tankPaint)
                canvas.drawRect(e.x + 4f, e.y - 7f, e.x + 16f, e.y - 5f, gun)
            } else {
                // Jet: alas, cabina y postcombustión
                val jet = jetPaint
                val glass = Paint().apply { color = Color.parseColor("#A8D8FC"); isAntiAlias = false }
                val flame = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
                canvas.drawRect(e.x - 14f, e.y - 3f, e.x + 14f, e.y + 3f, jet)
                canvas.drawRect(e.x - 6f, e.y - 8f, e.x - 2f, e.y - 3f, jet)
                canvas.drawRect(e.x + 2f, e.y - 8f, e.x + 6f, e.y - 3f, jet)
                canvas.drawRect(e.x - 3f, e.y + 3f, e.x + 3f, e.y + 8f, jet)
                canvas.drawRect(e.x - 8f, e.y - 1f, e.x - 2f, e.y + 1f, glass)
                canvas.drawRect(e.x + 14f, e.y - 2f, e.x + 20f, e.y + 2f, flame)
            }
        }
        // hostages: personas animadas saludando (o a bordo)
        for (ho in hostages) {
            val aboard = ho.state == 1
            PixelSprite.draw(canvas, ho.x, ho.y, if (aboard) 12f else 18f, facingRight = true, hatColor = if (aboard) 0x5C94FC else 0xFC9838)
            if (!aboard) {
                // brazo saludando
                val arm = Paint().apply { color = Color.parseColor("#F8D878"); strokeWidth = 3f; isAntiAlias = false }
                val wave = ((ho.x / 20f).toInt() % 2 == 0)
                canvas.drawLine(ho.x + 6f, ho.y - 2f, ho.x + 10f, if (wave) ho.y - 12f else ho.y - 8f, arm)
            }
        }
        // HUD
        textPaint.textSize = 11f
        canvas.drawText("Score: $score  Vidas: $lives  Combustible: ${fuel.toInt()}%  Rescatados: $rescued/$targetRescue", w / 2f, 14f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("A: Disparar  B: Bomba", 8f, h - 8f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡MISIÓN CUMPLIDA!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 28f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 11f; textPaint.color = Color.parseColor("#F8F8F8")
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 20f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}