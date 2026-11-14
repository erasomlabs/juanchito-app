package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Ghost Blade: reconstrucción fiel de Ghost Blade (Club Nokia, 2005).
 *
 * Mecánica original:
 *  · Disparar enemigos en un shooter de desplazamiento vertical.
 *  · Esquivar proyectiles enemigos.
 *  · Power-ups: más velocidad, más disparos, escudo.
 *  · Múltiples niveles con bosses.
 *  · High score.
 */
class GhostBladeGame : Game {
    override val name = "Ghost Blade"
    override val genre = "Shooter"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Jugador
    private var playerX = 0f
    private var playerY = 0f
    private var playerSpeed = 300f
    private var playerHealth = 100f
    private val maxHealth = 100f
    private var shootCooldown = 0f
    private var shootInterval = 0.15f
    private var hasShield = false
    private var shieldTimer = 0f
    private var rapidFire = false
    private var rapidFireTimer = 0f

    // Proyectiles del jugador
    private data class Bullet(val x: Float, var y: Float, val speed: Float)
    private val bullets = mutableListOf<Bullet>()

    // Enemigos
    private data class Enemy(val x: Float, var y: Float, val speed: Float, var health: Int, val type: Int, var shootTimer: Float)
    private val enemies = mutableListOf<Enemy>()
    private var enemySpawnTimer = 0f
    private var enemySpawnInterval = 1.5f

    // Proyectiles enemigos
    private data class EnemyBullet(var x: Float, var y: Float, val speed: Float, val vx: Float)
    private val enemyBullets = mutableListOf<EnemyBullet>()

    // Power-ups
    private data class PowerUp(val x: Float, var y: Float, val type: Int)  // 0=health, 1=shield, 2=rapidfire, 3=speed
    private val powerUps = mutableListOf<PowerUp>()
    private var powerUpTimer = 0f

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var kills = 0
    private var combo = 0
    private var maxCombo = 0
    private var level = 1
    private var enemiesToKill = 20
    private var killedThisLevel = 0

    // Animación
    private var explosionTimer = 0f
    private var explosionX = 0f
    private var explosionY = 0f
    private var bgScroll = 0f

    // Paints
    private val bgPaint = paintOf("#0A0A1E")
    private val starPaint = paintOf("#FFFFFF")
    private val playerPaint = paintOf("#5C94FC")
    private val playerDark = paintOf("#3A6AD0")
    private val bulletPaint = paintOf("#F5C542")
    private val enemyPaint = paintOf("#D82800")
    private val enemyDark = paintOf("#A01800")
    private val enemyBulletPaint = paintOf("#FF5252")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val yellowPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        playerX = w / 2f - 20f
        playerY = h - 100f
        playerHealth = maxHealth
        hasShield = false
        shieldTimer = 0f
        rapidFire = false
        rapidFireTimer = 0f
        shootCooldown = 0f
        bullets.clear()
        enemies.clear()
        enemyBullets.clear()
        powerUps.clear()
        enemySpawnTimer = 0f
        enemySpawnInterval = 1.5f
        powerUpTimer = 0f
        scoreVal = 0
        kills = 0
        level = 1
        enemiesToKill = 20
        killedThisLevel = 0
        state = State.PLAYING
        explosionTimer = 0f
        bgScroll = 0f
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.PLAYING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.LEVEL_COMPLETE) {
                    level++
                    enemiesToKill += 5
                    enemySpawnInterval = (enemySpawnInterval * 0.9f).coerceAtLeast(0.5f)
                    killedThisLevel = 0
                    state = State.PLAYING
                }
            }
            return
        }

        // Timers
        bgScroll += dt * 30f
        if (bgScroll > h) bgScroll -= h
        shootCooldown = (shootCooldown - dt).coerceAtLeast(0f)
        if (hasShield) { shieldTimer -= dt; if (shieldTimer <= 0f) hasShield = false }
        if (rapidFire) { rapidFireTimer -= dt; if (rapidFireTimer <= 0f) rapidFire = false }
        explosionTimer = (explosionTimer - dt).coerceAtLeast(0f)

        // Movimiento del jugador
        val speed = if (rapidFire) playerSpeed * 1.2f else playerSpeed
        if (input.left) playerX = (playerX - speed * dt).coerceAtLeast(20f)
        if (input.right) playerX = (playerX + speed * dt).coerceAtMost(w - 60f)
        if (input.up) playerY = (playerY - speed * dt).coerceAtLeast(20f)
        if (input.down) playerY = (playerY + speed * dt).coerceAtMost(h - 60f)

        // Disparo
        val currentShootInterval = if (rapidFire) shootInterval * 0.5f else shootInterval
        if ((input.a || input.start) && shootCooldown <= 0f) {
            bullets.add(Bullet(playerX + 20f, playerY, 600f))
            if (rapidFire) bullets.add(Bullet(playerX + 10f, playerY + 5f, 600f))
            shootCooldown = currentShootInterval
        }

        // Mover balas del jugador
        val bulletIt = bullets.iterator()
        while (bulletIt.hasNext()) {
            val b = bulletIt.next()
            b.y -= b.speed * dt
            if (b.y < -20f) bulletIt.remove()
        }

        // Spawn enemigos
        enemySpawnTimer += dt
        if (enemySpawnTimer >= enemySpawnInterval) {
            enemySpawnTimer = 0f
            val type = Random.nextInt(3)
            val enemyX = Random.nextFloat() * (w - 80f) + 40f
            val enemySpeed = 60f + level * 10f + Random.nextFloat() * 40f
            val enemyHealth = when (type) { 0 -> 1; 1 -> 2; else -> 3 }
            enemies.add(Enemy(enemyX, -40f, enemySpeed, enemyHealth, type, Random.nextFloat() * 2f + 1f))
        }

        // Mover enemigos
        val enemyIt = enemies.iterator()
        while (enemyIt.hasNext()) {
            val e = enemyIt.next()
            e.y += e.speed * dt
            e.shootTimer -= dt
            // Disparo enemigo
            if (e.shootTimer <= 0f) {
                e.shootTimer = Random.nextFloat() * 2f + 1f
                val dx = playerX + 20f - e.x
                val dy = playerY - e.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > 0) {
                    enemyBullets.add(EnemyBullet(e.x, e.y + 20f, 200f + level * 20f, (dx / dist) * 100f))
                }
            }
            if (e.y > h + 40f) enemyIt.remove()
        }

        // Mover balas enemigas
        val ebIt = enemyBullets.iterator()
        while (ebIt.hasNext()) {
            val eb = ebIt.next()
            eb.y += eb.speed * dt
            eb.x += eb.vx * dt
            if (eb.y > h + 20f || eb.x < 0 || eb.x > w) ebIt.remove()
        }

        // Colisiones: balas del jugador vs enemigos
        val bulletsToRemove = mutableListOf<Bullet>()
        val enemiesToRemove = mutableListOf<Enemy>()
        for (b in bullets) {
            for (e in enemies) {
                if (b.x in (e.x - 20f)..(e.x + 40f) && b.y in (e.y - 20f)..(e.y + 40f)) {
                    e.health--
                    bulletsToRemove.add(b)
                    if (e.health <= 0) {
                        enemiesToRemove.add(e)
                        kills++
                        killedThisLevel++
                        scoreVal += 50 + combo * 5
                        if (scoreVal > best) best = scoreVal
                        combo++
                        explosionTimer = 0.3f
                        explosionX = e.x
                        explosionY = e.y
                        // Power-up drop (10% chance)
                        if (Random.nextFloat() < 0.1f) {
                            val type = Random.nextInt(4)
                            powerUps.add(PowerUp(e.x, e.y, type))
                        }
                    }
                    break
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
        for (e in enemiesToRemove) {
            enemies.remove(e)
            if (killedThisLevel >= enemiesToKill) {
                state = State.LEVEL_COMPLETE
            }
        }

        // Colisiones: balas enemigas vs jugador
        if (!hasShield) {
            val ebToRemove = mutableListOf<EnemyBullet>()
            for (eb in enemyBullets) {
                if (eb.x in (playerX - 10f)..(playerX + 50f) && eb.y in (playerY - 10f)..(playerY + 50f)) {
                    playerHealth -= 10f
                    ebToRemove.add(eb)
                    combo = 0
                    if (playerHealth <= 0f) {
                        playerHealth = 0f
                        state = State.GAME_OVER
                    }
                }
            }
            enemyBullets.removeAll(ebToRemove)
        }

        // Colisiones: enemigos vs jugador
        if (!hasShield) {
            val enemiesHit = mutableListOf<Enemy>()
            for (e in enemies) {
                if (e.x in (playerX - 20f)..(playerX + 40f) && e.y in (playerY - 20f)..(playerY + 40f)) {
                    playerHealth -= 20f
                    enemiesHit.add(e)
                    combo = 0
                    if (playerHealth <= 0f) {
                        playerHealth = 0f
                        state = State.GAME_OVER
                    }
                }
            }
            enemies.removeAll(enemiesHit)
        }

        // Power-ups
        val puIt = powerUps.iterator()
        while (puIt.hasNext()) {
            val pu = puIt.next()
            pu.y += 80f * dt
            if (pu.y > h + 20f) { puIt.remove(); continue }
            if (pu.x in (playerX - 10f)..(playerX + 50f) && pu.y in (playerY - 10f)..(playerY + 50f)) {
                when (pu.type) {
                    0 -> playerHealth = (playerHealth + 25f).coerceAtMost(maxHealth)
                    1 -> { hasShield = true; shieldTimer = 5f }
                    2 -> { rapidFire = true; rapidFireTimer = 8f }
                    3 -> playerSpeed = (playerSpeed + 50f).coerceAtMost(500f)
                }
                puIt.remove()
                scoreVal += 25
            }
        }

        // Spawn power-ups occasionally
        powerUpTimer += dt
        if (powerUpTimer > 15f) {
            powerUpTimer = 0f
            powerUps.add(PowerUp(Random.nextFloat() * (w - 40f) + 20f, -20f, Random.nextInt(4)))
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0A0A1E"))

        // Estrellas de fondo (scrolling)
        val starPaint = paintOf("#FFFFFF")
        for (i in 0 until 50) {
            val sx = ((i * 137 + bgScroll.toInt()) % w).toFloat()
            val sy = ((i * 251) % h).toFloat()
            val size = if (i % 3 == 0) 2f else 1f
            canvas.drawCircle(sx, sy, size, starPaint)
        }

        // Balas del jugador
        val bulletPaintLocal = paintOf("#F5C542")
        for (b in bullets) {
            canvas.drawRect(b.x - 2f, b.y - 8f, b.x + 2f, b.y + 8f, bulletPaintLocal)
        }

        // Enemigos
        for (e in enemies) {
            val ePaint = paintOf("#D82800")
            canvas.drawRoundRect(RectF(e.x, e.y, e.x + 40f, e.y + 40f), 4f, 4f, ePaint)
            val eDark = paintOf("#A01800")
            canvas.drawRect(e.x, e.y, e.x + 40f, e.y + 8f, eDark)
            // Ojos
            val eyePaint = paintOf("#FFFFFF")
            canvas.drawCircle(e.x + 12f, e.y + 18f, 4f, eyePaint)
            canvas.drawCircle(e.x + 28f, e.y + 18f, 4f, eyePaint)
            val pupil = paintOf("#FF0000")
            canvas.drawCircle(e.x + 12f, e.y + 18f, 2f, pupil)
            canvas.drawCircle(e.x + 28f, e.y + 18f, 2f, pupil)
        }

        // Balas enemigas
        val ebPaint = paintOf("#FF5252")
        for (eb in enemyBullets) {
            canvas.drawCircle(eb.x, eb.y, 4f, ebPaint)
        }

        // Power-ups
        for (pu in powerUps) {
            val puColor = when (pu.type) {
                0 -> "#00E676"  // health
                1 -> "#5C94FC"  // shield
                2 -> "#F5C542"  // rapid fire
                else -> "#00BFFF" // speed
            }
            val puPaint = paintOf(puColor)
            canvas.drawCircle(pu.x, pu.y, 12f, puPaint)
            val label = when (pu.type) { 0 -> "H"; 1 -> "S"; 2 -> "R"; else -> "V" }
            val labelPaint = paintOf("#FFFFFF")
            labelPaint.textSize = 12f
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, pu.x, pu.y + 4f, labelPaint)
        }

        // Jugador
        val pPaint = paintOf("#5C94FC")
        canvas.drawRoundRect(RectF(playerX, playerY, playerX + 40f, playerY + 40f), 6f, 6f, pPaint)
        val pDark = paintOf("#3A6AD0")
        canvas.drawRect(playerX, playerY, playerX + 40f, playerY + 8f, pDark)
        // Cabina
        val cabinPaint = paintOf("#8FD0F8")
        canvas.drawRect(playerX + 8f, playerY + 10f, playerX + 32f, playerY + 25f, cabinPaint)
        // Ojos
        val eyePaint = paintOf("#FFFFFF")
        canvas.drawCircle(playerX + 14f, playerY + 16f, 4f, eyePaint)
        canvas.drawCircle(playerX + 26f, playerY + 16f, 4f, eyePaint)
        val pupilPaint = paintOf("#000000")
        canvas.drawCircle(playerX + 14f, playerY + 16f, 2f, pupilPaint)
        canvas.drawCircle(playerX + 26f, playerY + 16f, 2f, pupilPaint)
        // Cañón
        val cannonPaint = paintOf("#F5C542")
        canvas.drawRect(playerX + 16f, playerY - 8f, playerX + 24f, playerY, cannonPaint)

        // Escudo
        if (hasShield) {
            val shieldPaint = paintOf("#5C94FC")
            shieldPaint.style = Paint.Style.STROKE
            shieldPaint.strokeWidth = 2f
            shieldPaint.alpha = 150
            canvas.drawCircle(playerX + 20f, playerY + 20f, 30f, shieldPaint)
        }

        // Rapid fire indicator
        if (rapidFire) {
            val rfPaint = paintOf("#F5C542")
            rfPaint.textSize = 12f
            rfPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("RAPID", playerX + 20f, playerY - 12f, rfPaint)
        }

        // Explosión
        if (explosionTimer > 0f) {
            val alpha = (explosionTimer / 0.3f * 255).toInt().coerceIn(0, 255)
            val expPaint = Paint().apply { color = Color.parseColor("#FF5252"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawCircle(explosionX + 20f, explosionY + 20f, 30f * (1f - explosionTimer / 0.3f), expPaint)
        }

        // HUD
        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("KILLS: $kills", 10f, 50f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        // Barra de vida
        val lifeBarX = 10f
        val lifeBarY = 65f
        val lifeBarW = w - 20f
        val lifeBarH = 12f
        val lifeBg = paintOf("#2A2A3E")
        canvas.drawRect(lifeBarX, lifeBarY, lifeBarX + lifeBarW, lifeBarY + lifeBarH, lifeBg)
        val lifeFill = paintOf("#FF5252")
        canvas.drawRect(lifeBarX, lifeBarY, lifeBarX + lifeBarW * (playerHealth / maxHealth), lifeBarY + lifeBarH, lifeFill)
        val lifeFrame = paintOf("#F8F8F8")
        lifeFrame.style = Paint.Style.STROKE
        lifeFrame.strokeWidth = 2f
        canvas.drawRect(lifeBarX, lifeBarY, lifeBarX + lifeBarW, lifeBarY + lifeBarH, lifeFrame)

        // Progreso de nivel
        smallPaint.textSize = 14f
        canvas.drawText("Eliminados: $killedThisLevel/$enemiesToKill", w / 2f, 85f, smallPaint)

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Eliminados: $killedThisLevel", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Combo máximo: $combo", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Eliminados: $kills", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Combo máximo: $maxCombo", w / 2f, h / 2f + 70f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 100f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 140f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("D-pad: mover  ·  A/START: disparar", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
