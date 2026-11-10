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

    // Estados: 0=Prólogo, 1=Gameplay Misión, 2=Diálogo de Radio, 3=Victoria Misión
    private var gameState = 0
    override var isGameOver = false
    override var score = 0

    private var currentPhase = 1
    private var dialogueIndex = 0
    private var dialogueQueue = mutableListOf<Pair<String, String>>()

    // Jugador (Bill Rizer)
    private var px = 80f; private var py = 0f
    private var vy = 0f
    private var onGround = true
    private var facing = 1
    private var lives = 3
    private var invulnTimer = 0f
    private var weaponType = "M" // M=Machine Gun, S=Spread, L=Laser
    private var reloadTimer = 0f

    // Nivel con scroll horizontal
    private var scrollX = 0f
    private val levelLength = 2400f
    private val groundY get() = h * 0.75f

    // Proyectiles y Enemigos
    data class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float, val isEnemy: Boolean, val color: Int)
    private val bullets = mutableListOf<Bullet>()

    data class AlienTrooper(var x: Float, var y: Float, var vx: Float, var hp: Int, var shootCooldown: Float)
    private val troopers = mutableListOf<AlienTrooper>()

    // Boss de fin de nivel: "Nave Nodriza / Núcleo Blindado"
    private var bossActive = false
    private var bossHp = 220
    private val bossMaxHp = 220
    private var bossShootTimer = 0f

    // Input debounce
    private var btnAPressed = false
    private var btnBPressed = false
    private var btnStartPressed = false

    private val pText = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        gameState = 0
        score = 0
        isGameOver = false
        lives = 3
        currentPhase = 1
        weaponType = "M"
        bossActive = false
        bossHp = 220
        scrollX = 0f
        px = 80f
        py = groundY
        vy = 0f
        bullets.clear()
        troopers.clear()

        // Spawnea soldados iniciales
        for (i in 1..8) {
            troopers.add(AlienTrooper(x = 400f + i * 240f, y = groundY, vx = -70f, hp = 1, shootCooldown = Random.nextFloat() * 2f))
        }
    }

    override fun update(dt: Float, input: GameInput) {
        val aDown = input.a && !btnAPressed
        val bDown = input.b && !btnBPressed
        val startDown = input.start && !btnStartPressed
        btnAPressed = input.a
        btnBPressed = input.b
        btnStartPressed = input.start

        when (gameState) {
            0 -> { // Prólogo
                if (aDown || startDown) {
                    gameState = 2
                    dialogueQueue.clear()
                    dialogueQueue.add("COMANDANTE HQ" to "¡Soldados Bill y Lance! Las fuerzas alienígenas de Red Falcon han tomado Neo City.")
                    dialogueQueue.add("BILL RIZER" to "Entendido HQ. Desplegando en la autopista principal. ¡Fuego a discreción!")
                    dialogueIndex = 0
                }
            }
            2 -> { // Diálogo de Radio
                if (aDown) {
                    dialogueIndex++
                    if (dialogueIndex >= dialogueQueue.size) {
                        gameState = 1
                    }
                }
            }
            1 -> { // Gameplay
                updateGameplay(dt, input, aDown, bDown)
            }
            3 -> { // Victoria de Misión
                if (aDown || startDown) {
                    currentPhase++
                    reset()
                }
            }
        }
    }

    private fun updateGameplay(dt: Float, input: GameInput, aDown: Boolean, bDown: Boolean) {
        if (invulnTimer > 0f) invulnTimer -= dt

        // Movimiento horizontal
        val moveSpeed = 160f
        if (input.left) {
            px -= moveSpeed * dt
            facing = -1
        }
        if (input.right) {
            px += moveSpeed * dt
            facing = 1
        }

        // Scroll de cámara
        if (px - scrollX > w * 0.55f && scrollX < levelLength - w) {
            scrollX = px - w * 0.55f
        }

        // Salto NES clásico con altura variable
        if (input.b && onGround) {
            vy = -430f
            onGround = false
        }
        vy += 1100f * dt
        py += vy * dt

        if (py >= groundY) {
            py = groundY
            vy = 0f
            onGround = true
        }

        // Disparo con [A]
        reloadTimer -= dt
        if (input.a && reloadTimer <= 0f) {
            reloadTimer = if (weaponType == "S") 0.35f else 0.16f
            val bx = px + facing * 18f
            val by = py - 12f

            if (weaponType == "S") { // Spread Gun (3 balas en abanico)
                bullets.add(Bullet(bx, by, facing * 450f, 0f, false, Color.parseColor("#E6B800")))
                bullets.add(Bullet(bx, by, facing * 420f, -90f, false, Color.parseColor("#E6B800")))
                bullets.add(Bullet(bx, by, facing * 420f, 90f, false, Color.parseColor("#E6B800")))
            } else {
                bullets.add(Bullet(bx, by, facing * 500f, 0f, false, Color.parseColor("#00E5FF")))
            }
        }

        // Activación del Boss al final de la ruta
        if (px >= levelLength - w * 0.5f && !bossActive) {
            bossActive = true
            gameState = 2
            dialogueQueue.clear()
            dialogueQueue.add("RED FALCON CORE" to "¡Humanos insignificantes! ¡Neo City será el nido de nuestra prole!")
            dialogueQueue.add("BILL RIZER" to "¡Apunta a las torretas gemelas y al núcleo central!")
            dialogueIndex = 0
            return
        }

        // Actualizar Balas
        val bIt = bullets.iterator()
        while (bIt.hasNext()) {
            val b = bIt.next()
            b.x += b.vx * dt
            b.y += b.vy * dt

            // Fuera de pantalla
            if (b.x < scrollX - 50f || b.x > scrollX + w + 50f || b.y < 0 || b.y > h) {
                bIt.remove()
                continue
            }

            if (!b.isEnemy) {
                // Impacto en soldados
                val tIt = troopers.iterator()
                while (tIt.hasNext()) {
                    val t = tIt.next()
                    if (kotlin.math.abs(b.x - t.x) < 18f && kotlin.math.abs(b.y - t.y) < 22f) {
                        t.hp--
                        bIt.remove()
                        if (t.hp <= 0) {
                            tIt.remove()
                            score += 50
                            if (Random.nextFloat() < 0.25f) weaponType = "S" // Drop Spread Gun
                        }
                        break
                    }
                }

                // Impacto en Boss
                if (bossActive && b.x >= levelLength - 160f) {
                    bossHp -= 6
                    bIt.remove()
                    if (bossHp <= 0) {
                        bossActive = false
                        score += 1000
                        gameState = 3 // Victoria
                    }
                    break
                }
            } else {
                // Bala enemiga impacta en jugador
                if (invulnTimer <= 0f && kotlin.math.abs(b.x - px) < 14f && kotlin.math.abs(b.y - py) < 20f) {
                    bIt.remove()
                    lives--
                    invulnTimer = 2.0f
                    if (lives <= 0) isGameOver = true
                }
            }
        }

        // Actualizar Soldados
        for (t in troopers) {
            if (t.x > scrollX - 30f && t.x < scrollX + w + 30f) {
                t.x += t.vx * dt
                t.shootCooldown -= dt
                if (t.shootCooldown <= 0f) {
                    t.shootCooldown = 2.4f
                    val bvx = if (px < t.x) -220f else 220f
                    bullets.add(Bullet(t.x, t.y - 12f, bvx, 0f, true, Color.parseColor("#E63946")))
                }
            }
        }

        // Boss dispara proyectiles
        if (bossActive) {
            bossShootTimer -= dt
            if (bossShootTimer <= 0f) {
                bossShootTimer = 1.3f
                val bossX = levelLength - 120f
                bullets.add(Bullet(bossX, groundY - 45f, -280f, -60f, true, Color.parseColor("#FF0055")))
                bullets.add(Bullet(bossX, groundY - 20f, -320f, 20f, true, Color.parseColor("#FF0055")))
            }
        }
    }

    override fun draw(canvas: Canvas) {
        when (gameState) {
            0 -> {
                NesDialogueRenderer.drawPrologueScreen(
                    canvas, w, h,
                    gameTitle = "CONTRA 3: THE ALIEN WARS",
                    missionTitle = "MISIÓN I: AUTOPISTA METROPOLITANA EN RUINAS",
                    prologueLines = listOf(
                        "• Año 2636. Las tropas extraterrestres de Red Falcon",
                        "  iniciaron una invasión total en la Tierra.",
                        "• La humanidad deposita su última esperanza en los comandos",
                        "  veteranos de élite: Bill Rizer y Lance Bean.",
                        "• MISION: Infiltra el sector industrial destruido,",
                        "  aniquila la vanguardia bio-mecánica y destruye el búnker jefe.",
                        "• CONTROLES: [A] Disparar  [B] Saltar  [SELECT] Recargar"
                    ),
                    primaryColor = Color.parseColor("#00A800")
                )
            }
            2 -> {
                drawScene(canvas)
                if (dialogueIndex < dialogueQueue.size) {
                    val (speaker, text) = dialogueQueue[dialogueIndex]
                    NesDialogueRenderer.drawDialogueBox(canvas, w, h, speaker, text, speakerColor = Color.parseColor("#00E5FF"))
                }
            }
            1 -> {
                drawScene(canvas)
            }
            3 -> {
                NesDialogueRenderer.drawPrologueScreen(
                    canvas, w, h,
                    gameTitle = "¡MISIÓN I CUMPLIDA!",
                    missionTitle = "SECTOR NEO CITY ASEGURADO",
                    prologueLines = listOf(
                        "• El núcleo defensivo alienígena explotó en llamas.",
                        "• Se ha interceptado una señal proveniente del Sector Oceánico.",
                        "• El transporte blindado te llevará a la base subterránea.",
                        "• Puntuación final de misión: $score pts."
                    ),
                    primaryColor = Color.parseColor("#E6B800")
                )
            }
        }

        if (isGameOver) {
            val pOver = Paint().apply { color = Color.parseColor("#CC150005"); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), pOver)
            val pT = Paint().apply { color = Color.parseColor("#E63946"); textSize = 32f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText("GAME OVER - CONTINUES: 0", w / 2f, h / 2f - 10f, pT)
            pT.textSize = 14f; pT.color = Color.parseColor("#F5F5F5")
            canvas.drawText("PULSA [START] O [A] PARA REINTENTAR", w / 2f, h / 2f + 25f, pT)
        }
    }

    private fun drawScene(canvas: Canvas) {
        // Cielo nocturno en llamas (Neo City)
        canvas.drawColor(Color.parseColor("#150B15"))

        // Edificios en ruinas de fondo con efecto parallax
        val pBldg = Paint().apply { color = Color.parseColor("#2A1E30") }
        for (i in 0..12) {
            val bx = (i * 120f) - scrollX * 0.35f
            canvas.drawRect(bx, h * 0.25f + (i % 3) * 20f, bx + 90f, groundY, pBldg)
        }

        // Calzada y plataformas
        val pRoad = Paint().apply { color = Color.parseColor("#383848") }
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), pRoad)
        val pStripes = Paint().apply { color = Color.parseColor("#E6B800"); strokeWidth = 3f }
        canvas.drawLine(0f, groundY + 4f, w.toFloat(), groundY + 4f, pStripes)

        // Boss Final al final del nivel
        if (bossActive) {
            val bx = levelLength - 150f - scrollX
            val pBoss = Paint().apply { color = Color.parseColor("#C41E24") }
            canvas.drawRoundRect(RectF(bx, groundY - 80f, bx + 120f, groundY), 8f, 8f, pBoss)
            // Núcleo palpitante
            val pCore = Paint().apply { color = Color.parseColor("#00E5FF") }
            canvas.drawCircle(bx + 60f, groundY - 40f, 18f, pCore)

            NesDialogueRenderer.drawBossHud(canvas, w, "RED FALCON BIO-CORE", bossHp, bossMaxHp)
        }

        // Soldados enemigos
        val pTrooper = Paint().apply { color = Color.parseColor("#D82800") }
        for (t in troopers) {
            val tx = t.x - scrollX
            canvas.drawRect(tx - 10f, t.y - 24f, tx + 10f, t.y, pTrooper)
            val pGun = Paint().apply { color = Color.parseColor("#808080") }
            canvas.drawRect(tx - 16f, t.y - 16f, tx, t.y - 12f, pGun)
        }

        // Jugador Bill Rizer
        if (invulnTimer <= 0f || (invulnTimer * 10).toInt() % 2 == 0) {
            val dx = px - scrollX
            val pBill = Paint().apply { color = Color.parseColor("#00A800") } // Pantalón comando
            canvas.drawRect(dx - 8f, py - 12f, dx + 8f, py, pBill)
            val pChest = Paint().apply { color = Color.parseColor("#F5C542") }
            canvas.drawRect(dx - 7f, py - 24f, dx + 7f, py - 12f, pChest)
            val pBandana = Paint().apply { color = Color.parseColor("#C41E24") }
            canvas.drawRect(dx - 8f, py - 27f, dx + 8f, py - 23f, pBandana)
            // Rifle
            val pRifle = Paint().apply { color = Color.parseColor("#F8F8F8") }
            canvas.drawRect(dx, py - 18f, dx + facing * 18f, py - 14f, pRifle)
        }

        // Balas
        for (b in bullets) {
            val pBullet = Paint().apply { color = b.color }
            val sz = if (weaponType == "S" && !b.isEnemy) 4.5f else 3f
            canvas.drawCircle(b.x - scrollX, b.y, sz, pBullet)
        }

        // HUD Superior
        pText.color = Color.parseColor("#F8F8F8")
        canvas.drawText("P1: BILL  VIDAS: $lives  ARMA: [$weaponType]  SCORE: $score", 12f, 18f, pText)
        pText.color = Color.parseColor("#E6B800")
        canvas.drawText("FASE $currentPhase | NEO CITY | AVANCE: ${(px / levelLength * 100).toInt()}%", 12f, 32f, pText)
    }
}
