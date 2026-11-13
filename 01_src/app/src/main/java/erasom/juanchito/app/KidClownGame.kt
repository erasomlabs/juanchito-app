package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class KidClownGame : Game {
    override val name = "Payasito"
    override val genre = "Plataformas"
    private var w = 0; private var h = 0
    private val groundY get() = h * 0.82f
    private var px = 80f; private var py = 0f
    private var vy = 0f; private var onGround = true
    private var facing = 1
    private val balls = mutableListOf<Ball>()
    private val enemies = mutableListOf<Enemy>()
    private val items = mutableListOf<Item>()
    private var fuseX = 0f; private var fuseSpeed = 60f
    private var spawnTimer = 0f
    private var lives = 3
    private var fireCooldown = 0f
    private var invuln = 0f
    private var level = 1
    private var levelTimer = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false
    private var distance = 0f
    private val target = 3000f
    // Estados: 0 = Prólogo narrativo, 1 = Gameplay
    private var gameState = 0

    private data class Ball(var x: Float, var y: Float, val vx: Float)
    private data class Enemy(var x: Float, var y: Float, var vx: Float, val type: Int)
    private data class Item(var x: Float, var y: Float, val type: Int) // 0 heart,1 balloon,2 carrot

    private val playerPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val hatPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val ballPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val itemPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val fusePaint = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
    private val groundPaint = Paint().apply { color = Color.parseColor("#228B22"); isAntiAlias = false }
    private val skyPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    override fun reset() {
        px = 80f; py = groundY - 32f; vy = 0f; onGround = true; facing = 1
        balls.clear(); enemies.clear(); items.clear()
        fuseX = -60f; fuseSpeed = 70f; spawnTimer = 0f
        lives = 3; fireCooldown = 0f; invuln = 0f; score = 0; distance = 0f; isGameOver = false; won = false; level = 1
        gameState = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (gameState == 0) {
            // Prólogo: esperar confirmación del jugador
            if (input.a || input.start) gameState = 1
            input.a = false; input.start = false
            return
        }
        if (isGameOver) { if (input.start) reset(); return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
        // fuse chaser
        fuseX += fuseSpeed * dt
        if (fuseX > px - 20f && invuln <= 0f) {
            lives--; invuln = 2f; fuseX = px - 120f
            if (lives <= 0) isGameOver = true
        }
        // movement
        if (input.left) { px -= 200f * dt; facing = -1 }
        if (input.right) { px += 200f * dt; facing = 1 }
        px = px.coerceIn(20f, w * 0.55f)
        if (input.b && onGround) { vy = -620f; onGround = false; input.b = false }
        vy += 1400f * dt
        py += vy * dt
        if (py >= groundY - 32f) { py = groundY - 32f; vy = 0f; onGround = true }
        if (input.a && fireCooldown <= 0f) {
            balls.add(Ball(px + facing * 16f, py, facing * 520f)); fireCooldown = 0.25f
        }
        balls.removeAll { it.x < -20f || it.x > w + 20f }
        for (b in balls) b.x += b.vx * dt
        // scroll
        val scroll = 90f * dt
        distance += scroll
        for (e in enemies) e.x -= scroll
        for (it in items) it.x -= scroll
        fuseX -= scroll * 0.5f
        enemies.removeAll { it.x < -40f }
        items.removeAll { it.x < -20f }
        // spawn
        spawnTimer += dt
        if (spawnTimer > 1.3f) {
            spawnTimer = 0f
            enemies.add(Enemy(w + 20f, groundY - 30f, -70f, Random.nextInt(2)))
            if (Random.nextFloat() < 0.45f) items.add(Item(w + 20f, groundY - 55f, Random.nextInt(3)))
        }
        // enemies
        val ei = enemies.iterator()
        while (ei.hasNext()) {
            val e = ei.next()
            e.x += e.vx * dt
            if (balls.any { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }) {
                balls.removeAll { b -> kotlin.math.abs(b.x - e.x) < 20f && kotlin.math.abs(b.y - e.y) < 20f }
                score += if (e.type == 0) 10 else 20; if (score > highScore) highScore = score; ei.remove(); continue
            }
            if (invuln <= 0f && kotlin.math.abs(e.x - px) < 24f && kotlin.math.abs(e.y - py) < 32f) {
                lives--; invuln = 2f
                if (lives <= 0) isGameOver = true
            }
        }
        // items
        val ii = items.iterator()
        while (ii.hasNext()) {
            val it = ii.next()
            if (kotlin.math.abs(it.x - px) < 20f && kotlin.math.abs(it.y - py) < 32f) {
                when (it.type) {
                    0 -> lives = (lives + 1).coerceAtMost(5)
                    1 -> score += 30
                    2 -> score += 15
                }
                if (score > highScore) highScore = score; ii.remove()
            }
        }
        // fuse speed increases
        fuseSpeed = 70f + level * 10f
        if (distance >= target) { won = true; isGameOver = true; score += 500 }
    }

    override fun draw(canvas: Canvas) {
        if (gameState == 0) {
            // Prólogo estilo NES: la historia del circo encantado
            NesDialogueRenderer.drawPrologueScreen(
                canvas,
                w,
                h,
                gameTitle = "PAYASITO",
                missionTitle = "ACTO 1: LA CARRERA DEL GLOBO-BOMBA",
                prologueLines = listOf(
                    "El circo estelar de la Princesa Miel brillaba",
                    "hasta que el malvado mago Zarko encendió",
                    "la mecha del Globo-Bomba real.",
                    "",
                    "Si el Globo-Bomba revienta, la carpa y todos",
                    "los saltimbanquis desaparecerán para siempre.",
                    "",
                    "Pixel, el payasito más veloz del reino,",
                    "debe correr por el País de la Golosina,",
                    "recolectar los globos dorados y derribar",
                    "a los secuaces de Zarko antes de que la",
                    "mecha lo alcance. ¡El espectáculo debe continuar!"
                ),
                primaryColor = Color.parseColor("#C84C0C")
            )
            return
        }
        // sky
        canvas.drawColor(Color.parseColor("#5C94FC"))
        canvas.drawRect(0f, h * 0.6f, w.toFloat(), h.toFloat(), groundPaint)
        // path
        val pathPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
        canvas.drawRect(0f, groundY - 4f, w.toFloat(), groundY + 4f, pathPaint)
        // fuse chaser (bomb with fuse)
        canvas.drawCircle(fuseX, groundY - 16f, 12f, fusePaint)
        canvas.drawRect(fuseX - 2f, groundY - 32f, fuseX + 2f, groundY - 16f, fusePaint)
        // items
        for (it in items) {
            itemPaint.color = when (it.type) { 0 -> Color.parseColor("#D82800"); 1 -> Color.parseColor("#FC9838"); else -> Color.parseColor("#00A800") }
            canvas.drawCircle(it.x, it.y, 8f, itemPaint)
        }
        // enemies
        for (e in enemies) {
            enemyPaint.color = if (e.type == 0) Color.parseColor("#00A800") else Color.parseColor("#8814A8")
            canvas.drawRoundRect(RectF(e.x - 12f, e.y - 14f, e.x + 12f, e.y + 14f), 4f, 4f, enemyPaint)
        }
        // balls
        for (b in balls) canvas.drawCircle(b.x, b.y, 5f, ballPaint)
        // Pixel payaso protagonista
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            PixelSprite.draw(canvas, px, py, 32f, facingRight = facing > 0, hatColor = 0xD82800)
            // nariz de payaso
            val nosePaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
            canvas.drawCircle(px + if (facing > 0) 8f else -8f, py - 8f, 3f, nosePaint)
        }
        // HUD
        textPaint.textSize = 12f
        canvas.drawText("Score: $score  Vidas: $lives  Nivel: $level", w / 2f, 14f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Dist: ${distance.toInt()}/${target.toInt()}", 8f, h - 8f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 28f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 11f; textPaint.color = Color.parseColor("#F8F8F8")
            val story = if (won) "Pixel atrapó el Globo-Bomba y salvó el circo de la Princesa Miel" else "Zarko rió malvadamente... el circo quedó a oscuras"
            canvas.drawText(story, w / 2f, h / 2f + 20f, textPaint)
            textPaint.textSize = 12f
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 42f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}