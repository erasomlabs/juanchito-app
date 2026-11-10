package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class ChronoTriggerGame : Game {
    override val name = "Chrono Trigger"
    override val genre = "RPG"
    private var w = 0; private var h = 0

    // Modos de juego: 0 = Prólogo narrativo, 1 = Exploración Mundo/Castillo, 2 = Batalla por turnos ATB, 3 = Diálogo In-Game, 4 = Victoria Capítulo
    private var gameState = 0
    override var isGameOver = false
    override var score = 0

    // Progreso narrativo y capítulos
    private var currentChapter = 1
    private var dialogueIndex = 0
    private var dialogueQueue = mutableListOf<Pair<String, String>>()

    // Mundo y mapa por tiles (Castillo Guardia & Bosque Milenario)
    private val tileSize = 32
    private var mapW = 0; private var mapH = 0
    private val tileMap = mutableListOf<IntArray>() // 0=pasto, 1=muro/piedra, 2=árbol denso, 3=camino adoquinado, 4=portal tiempo, 5=trono
    private var px = 0f; private var py = 0f
    private var camX = 0f; private var camY = 0f

    // Party de héroes (Crono, Marle, Lucca)
    data class Hero(val name: String, var hp: Int, val maxHp: Int, var mp: Int, val maxMp: Int, var atb: Float, val color: Int, val techName: String)
    private val party = mutableListOf<Hero>()
    private var activeHeroIndex = 0

    // Enemigos y Boss de fase
    data class Enemy(val name: String, var hp: Int, val maxHp: Int, var atb: Float, val color: Int, val isBoss: Boolean = false)
    private val enemies = mutableListOf<Enemy>()
    private var battleActionMsg = ""
    private var battleEndTimer = 0f
    private var bossTriggered = false

    // Input debounce
    private var btnAPressed = false
    private var btnBPressed = false
    private var btnStartPressed = false

    // Paleta NES
    private val pGrass = Paint().apply { color = Color.parseColor("#008800") }
    private val pWall = Paint().apply { color = Color.parseColor("#7C7C7C") }
    private val pTree = Paint().apply { color = Color.parseColor("#005000") }
    private val pRoad = Paint().apply { color = Color.parseColor("#E4A400") }
    private val pPortal = Paint().apply { color = Color.parseColor("#00E5FF") }
    private val pTrono = Paint().apply { color = Color.parseColor("#C41E24") }
    private val pText = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 11f; isAntiAlias = false }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        gameState = 0 // Inicia en Prólogo
        score = 0
        isGameOver = false
        currentChapter = 1
        bossTriggered = false
        party.clear()
        party.add(Hero("Crono", 95, 95, 20, 20, 0.4f, Color.parseColor("#E6B800"), "Ciclón"))
        party.add(Hero("Marle", 75, 75, 35, 35, 0.2f, Color.parseColor("#00D9FF"), "Aura Cura"))
        party.add(Hero("Lucca", 80, 80, 40, 40, 0.1f, Color.parseColor("#C41E24"), "Fuego"))

        setupChapterMap(currentChapter)
    }

    private fun setupChapterMap(chapter: Int) {
        tileMap.clear()
        mapW = 24
        mapH = 16

        for (r in 0 until mapH) {
            val row = IntArray(mapW)
            for (c in 0 until mapW) {
                // Bordes de bosque/muro
                if (r == 0 || r == mapH - 1 || c == 0 || c == mapW - 1) {
                    row[c] = 2
                } else if (r in 3..5 && c in 7..16) {
                    row[c] = 1 // Muro de fortaleza
                } else if (c == 11 || c == 12) {
                    row[c] = 3 // Calzada de piedra
                } else {
                    row[c] = if ((r + c) % 7 == 0) 2 else 0
                }
            }
            tileMap.add(row)
        }
        tileMap[2][11] = 5 // Trono real
        tileMap[13][11] = 4 // Portal dimensional

        px = 11.5f * tileSize
        py = 8.5f * tileSize
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
                    // Pasar a diálogo introductorio
                    gameState = 3
                    dialogueQueue.clear()
                    dialogueQueue.add("CRONO" to "¡El portal de la feria milenaria se abrió fuera de control!")
                    dialogueQueue.add("MARLE" to "¡Crono, el reino de Guardia de hace 400 años está en peligro!")
                    dialogueQueue.add("LUCCA" to "Debemos infiltrarnos en el castillo antes de que la reina caiga.")
                    dialogueIndex = 0
                }
            }
            3 -> { // Diálogo de personajes
                if (aDown) {
                    dialogueIndex++
                    if (dialogueIndex >= dialogueQueue.size) {
                        gameState = 1 // Pasar a exploración
                    }
                }
            }
            1 -> { // Exploración por mapa
                val speed = 120f * dt
                var nx = px
                var ny = py

                if (input.up) ny -= speed
                if (input.down) ny += speed
                if (input.left) nx -= speed
                if (input.right) nx += speed

                // Colisiones con tiles
                val tx = (nx / tileSize).toInt().coerceIn(0, mapW - 1)
                val ty = (ny / tileSize).toInt().coerceIn(0, mapH - 1)
                val tile = tileMap[ty][tx]

                if (tile != 1 && tile != 2) {
                    px = nx
                    py = ny
                }

                // Interacción con Portal Dimensional (Boss) o Trono
                if (tile == 4 && !bossTriggered) {
                    triggerBossBattle()
                } else if (tile == 5 && bossTriggered) {
                    // Victoria de capítulo
                    gameState = 4
                    score += 500
                }

                // Encuentros aleatorios al caminar
                if ((input.up || input.down || input.left || input.right) && Random.nextFloat() < 0.006f && !bossTriggered) {
                    triggerNormalBattle()
                }

                camX = px - w / 2f
                camY = py - h / 2f
            }
            2 -> { // Batalla ATB
                updateBattle(dt, aDown, bDown, input.select)
            }
            4 -> { // Victoria de capítulo
                if (aDown || startDown) {
                    currentChapter++
                    reset()
                }
            }
        }
    }

    private fun triggerNormalBattle() {
        gameState = 2
        battleActionMsg = "¡Monstruos del bosque atacan!"
        enemies.clear()
        enemies.add(Enemy("Gárgola", 45, 45, 0.2f, Color.parseColor("#00E5FF")))
        enemies.add(Enemy("Caballero", 60, 60, 0.1f, Color.parseColor("#D82800")))
    }

    private fun triggerBossBattle() {
        bossTriggered = true
        gameState = 3
        dialogueQueue.clear()
        dialogueQueue.add("HECHICERO YAKRA" to "¡Nadie salvará al linaje real de Guardia! ¡Perecerán en el tiempo!")
        dialogueQueue.add("CRONO" to "¡Desenvaina tu acero! ¡Por el reino!")
        dialogueIndex = 0

        enemies.clear()
        enemies.add(Enemy("Yakra Boss", 180, 180, 0.1f, Color.parseColor("#FF0055"), isBoss = true))
    }

    private fun updateBattle(dt: Float, aDown: Boolean, bDown: Boolean, selectDown: Boolean) {
        // Carga de barra ATB de héroes
        for (hero in party) {
            if (hero.hp > 0 && hero.atb < 1f) {
                hero.atb = (hero.atb + dt * 0.45f).coerceAtMost(1f)
            }
        }

        // Carga y ataque de enemigos
        for (enemy in enemies) {
            if (enemy.hp > 0) {
                enemy.atb += dt * 0.35f
                if (enemy.atb >= 1f) {
                    enemy.atb = 0f
                    // Ataca a un héroe vivo al azar
                    val targets = party.filter { it.hp > 0 }
                    if (targets.isNotEmpty()) {
                        val target = targets.random()
                        val dmg = if (enemy.isBoss) Random.nextInt(18, 30) else Random.nextInt(8, 16)
                        target.hp = (target.hp - dmg).coerceAtLeast(0)
                        battleActionMsg = "¡${enemy.name} ataca a ${target.name} por $dmg daño!"
                    }
                }
            }
        }

        // Comprobar derrota
        if (party.all { it.hp <= 0 }) {
            isGameOver = true
            return
        }

        // Comprobar victoria
        if (enemies.all { it.hp <= 0 }) {
            battleEndTimer += dt
            if (battleEndTimer > 1.2f) {
                battleEndTimer = 0f
                score += if (bossTriggered) 300 else 100
                gameState = 1 // Vuelve al mapa
                battleActionMsg = ""
            }
            return
        }

        // Cambio de héroe activo con SELECT
        if (selectDown) {
            activeHeroIndex = (activeHeroIndex + 1) % party.size
        }

        val curHero = party[activeHeroIndex]
        val targetEnemy = enemies.firstOrNull { it.hp > 0 } ?: return

        if (curHero.hp > 0 && curHero.atb >= 1f) {
            if (aDown) { // Ataque físico
                curHero.atb = 0f
                val dmg = Random.nextInt(22, 34)
                targetEnemy.hp = (targetEnemy.hp - dmg).coerceAtLeast(0)
                battleActionMsg = "¡${curHero.name} golpea con espada a ${targetEnemy.name} ($dmg pts)!"
            } else if (bDown && curHero.mp >= 8) { // Técnica especial
                curHero.atb = 0f
                curHero.mp -= 8
                if (curHero.name == "Marle") {
                    // Cura a la party
                    for (h in party) if (h.hp > 0) h.hp = (h.hp + 35).coerceAtMost(h.maxHp)
                    battleActionMsg = "¡Marle usa Aura Curativa en el escuadrón!"
                } else {
                    val dmg = Random.nextInt(38, 55)
                    targetEnemy.hp = (targetEnemy.hp - dmg).coerceAtLeast(0)
                    battleActionMsg = "¡${curHero.name} desata ${curHero.techName} ($dmg pts)!"
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        when (gameState) {
            0 -> {
                NesDialogueRenderer.drawPrologueScreen(
                    canvas, w, h,
                    gameTitle = "CHRONO TRIGGER",
                    missionTitle = "CAPÍTULO I: EL REINO DE GUARDIA Y EL PORTAL TEMPORAL",
                    prologueLines = listOf(
                        "• Año 1000 d.C., Reino de Guardia.",
                        "• Durante la Feria Milenaria, el invento de Lucca",
                        "  abrió una fisura dimensional en el continuo espacio-tiempo.",
                        "• La princesa Marle ha desaparecido arrastrada al pasado.",
                        "• Crono empuña su katana y cruza el portal para impedir",
                        "  que la línea temporal sea erradicada por el parásito Lavos.",
                        "• OBJETIVO: Atraviesa el bosque, llega al castillo y derrota al impostor."
                    ),
                    primaryColor = Color.parseColor("#C41E24")
                )
            }
            3 -> {
                // Dibuja el mapa de fondo y el diálogo encima
                drawWorldMap(canvas)
                if (dialogueIndex < dialogueQueue.size) {
                    val (speaker, text) = dialogueQueue[dialogueIndex]
                    NesDialogueRenderer.drawDialogueBox(canvas, w, h, speaker, text)
                }
            }
            1 -> {
                drawWorldMap(canvas)
            }
            2 -> {
                drawBattleScene(canvas)
            }
            4 -> {
                NesDialogueRenderer.drawPrologueScreen(
                    canvas, w, h,
                    gameTitle = "¡CAPÍTULO I SUPERADO!",
                    missionTitle = "EL REINO DE GUARDIA ESTÁ A SALVO POR AHORA",
                    prologueLines = listOf(
                        "• El impostor Yakra fue desterrado del castillo.",
                        "• La reina original ha sido restituida en el trono real.",
                        "• Sin embargo, una campana lejana resuena en el horizonte...",
                        "• ¡Un nuevo portal se ha abierto hacia el futuro desolado (2300 d.C.)!",
                        "• Puntuación acumulada: $score monedas de oro."
                    ),
                    primaryColor = Color.parseColor("#00E5FF")
                )
            }
        }

        if (isGameOver) {
            val pOver = Paint().apply { color = Color.parseColor("#CC150005"); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), pOver)
            val pT = Paint().apply { color = Color.parseColor("#E63946"); textSize = 30f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText("EL DESTINO HA SIDO ALTERADO", w / 2f, h / 2f - 10f, pT)
            pT.textSize = 14f
            pT.color = Color.parseColor("#F5F5F5")
            canvas.drawText("PULSA [START] O [A] PARA REINTENTAR", w / 2f, h / 2f + 25f, pT)
        }
    }

    private fun drawWorldMap(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0E1A0E"))

        val startCol = (camX / tileSize).toInt().coerceAtLeast(0)
        val endCol = ((camX + w) / tileSize).toInt().coerceAtMost(mapW - 1)
        val startRow = (camY / tileSize).toInt().coerceAtLeast(0)
        val endRow = ((camY + h) / tileSize).toInt().coerceAtMost(mapH - 1)

        for (r in startRow..endRow) {
            for (c in startCol..endCol) {
                val dx = c * tileSize - camX
                val dy = r * tileSize - camY
                val tile = tileMap[r][c]
                val rect = RectF(dx, dy, dx + tileSize, dy + tileSize)

                val paint = when (tile) {
                    1 -> pWall
                    2 -> pTree
                    3 -> pRoad
                    4 -> pPortal
                    5 -> pTrono
                    else -> pGrass
                }
                canvas.drawRect(rect, paint)

                // Textura retro simple
                if (tile == 1) {
                    val pBorder = Paint().apply { color = Color.parseColor("#444444"); style = Paint.Style.STROKE; strokeWidth = 2f }
                    canvas.drawRect(rect, pBorder)
                }
            }
        }

        // Jugador (Crono con pelo rojo/katana)
        val cx = px - camX
        val cy = py - camY
        val pCrono = Paint().apply { color = Color.parseColor("#E6B800") }
        canvas.drawCircle(cx, cy - 8f, 7f, pCrono)
        val pPelo = Paint().apply { color = Color.parseColor("#C41E24") }
        canvas.drawRect(cx - 6f, cy - 16f, cx + 6f, cy - 8f, pPelo)
        val pCuerpo = Paint().apply { color = Color.parseColor("#00E5FF") }
        canvas.drawRect(cx - 5f, cy - 1f, cx + 5f, cy + 10f, pCuerpo)

        // HUD mapa
        pText.color = Color.parseColor("#F8F8F8")
        canvas.drawText("CASTILLO DE GUARDIA (600 d.C.) | Nivel $currentChapter | Score: $score", 12f, 18f, pText)
        pText.color = Color.parseColor("#FFD700")
        canvas.drawText("Ve hacia el Norte (Trono) o Portal Celeste", 12f, 32f, pText)
    }

    private fun drawBattleScene(canvas: Canvas) {
        // Fondo de batalla
        canvas.drawColor(Color.parseColor("#151528"))

        // Suelo de combate
        val pFloor = Paint().apply { color = Color.parseColor("#252540") }
        canvas.drawRect(0f, h * 0.55f, w.toFloat(), h.toFloat(), pFloor)

        // Boss HUD si aplica
        val boss = enemies.firstOrNull { it.isBoss }
        if (boss != null) {
            NesDialogueRenderer.drawBossHud(canvas, w, boss.name, boss.hp, boss.maxHp)
        }

        // Enemigos
        for (i in enemies.indices) {
            val enemy = enemies[i]
            if (enemy.hp <= 0) continue
            val ex = w * (0.25f + i * 0.25f)
            val ey = h * 0.45f
            val pE = Paint().apply { color = enemy.color }
            val sz = if (enemy.isBoss) 28f else 18f
            canvas.drawRoundRect(RectF(ex - sz, ey - sz, ex + sz, ey + sz), 4f, 4f, pE)

            // Barra HP enemiga
            val bWidth = sz * 2
            val php = Paint().apply { color = Color.parseColor("#D82800") }
            canvas.drawRect(ex - sz, ey - sz - 8f, ex - sz + bWidth * (enemy.hp.toFloat() / enemy.maxHp), ey - sz - 4f, php)
            pText.textAlign = Paint.Align.CENTER
            canvas.drawText(enemy.name, ex, ey - sz - 12f, pText)
            pText.textAlign = Paint.Align.LEFT
        }

        // Party de Héroes
        val panelTop = h * 0.65f
        val pPanel = Paint().apply { color = Color.parseColor("#080814"); style = Paint.Style.FILL }
        canvas.drawRect(8f, panelTop, w - 8f, h - 8f, pPanel)
        val pBorder = Paint().apply { color = Color.parseColor("#F8F8F8"); style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(8f, panelTop, w - 8f, h - 8f, pBorder)

        for (i in party.indices) {
            val hero = party[i]
            val hy = panelTop + 18f + i * 22f
            val isActive = i == activeHeroIndex

            pText.color = if (isActive) Color.parseColor("#FFD700") else Color.parseColor("#F8F8F8")
            val cursor = if (isActive) "▶ " else "  "
            canvas.drawText("$cursor${hero.name}", 20f, hy, pText)
            canvas.drawText("HP: ${hero.hp}/${hero.maxHp}", 95f, hy, pText)
            canvas.drawText("MP: ${hero.mp}", 175f, hy, pText)

            // Barra ATB
            val pAtb = Paint().apply { color = if (hero.atb >= 1f) Color.parseColor("#00E5FF") else Color.parseColor("#5A5A70") }
            canvas.drawRect(230f, hy - 9f, 230f + 50f * hero.atb, hy - 1f, pAtb)
        }

        // Mensaje de acción / Controles
        pText.color = Color.parseColor("#00E5FF")
        canvas.drawText(battleActionMsg, 300f, panelTop + 20f, pText)
        pText.color = Color.parseColor("#F5C542")
        canvas.drawText("[A] Atacar   [B] Técnica (-8 MP)   [SELECT] Cambiar Héroe", 300f, panelTop + 45f, pText)
    }
}
