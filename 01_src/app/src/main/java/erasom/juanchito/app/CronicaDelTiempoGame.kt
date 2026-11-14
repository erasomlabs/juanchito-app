package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * CronicaDelTiempo: RPG basado en Chrono Trigger (Square, 1995).
 *
 * Historia: Crono y sus amigos viajan en el tiempo para detener
 * a Lavos, una entidad alienígena que destruirá el mundo en 1999.
 *
 * Mecánica:
 *  · Combate por turnos con Tech combos (doble y triple).
 *  · Viaje en el tiempo: pasado (65M AC), presente (1000 AD), futuro (2300 AD).
 *  · Party de 3 personajes seleccionables.
 *  · Equipamiento, items, level up.
 *  · Múltiples finales (simplificado).
 *  · High score.
 */
class CronicaDelTiempoGame : Game {
    override val name = "Cronica del Tiempo"
    override val genre = "RPG"

    private var w = 0
    private var h = 0

    // Estado del juego
    private enum class State {
        TITLE, FIELD, BATTLE, DIALOG, MENU, GAME_OVER, VICTORY
    }
    private var state = State.TITLE

    // Personajes
    private data class Character(
        val name: String, val color: String, var maxHp: Int, var hp: Int,
        var maxMp: Int, var mp: Int, val attack: Int, val defense: Int,
        val speed: Int, var level: Int = 1, var exp: Int = 0
    )
    private val characters = mutableListOf<Character>()
    private var party = mutableListOf(0, 1, 2)  // índices de personajes en party
    private val allCharacters = listOf(
        Character("CRONO", "#5C94FC", 200, 200, 30, 30, 40, 20, 15),
        Character("MARLE", "#FF69B4", 150, 150, 50, 50, 25, 15, 12),
        Character("LUCCA", "#FF8C00", 160, 160, 45, 45, 30, 18, 10),
        Character("FROG", "#00A800", 220, 220, 20, 20, 50, 25, 13),
        Character("ROBO", "#8814A8", 300, 300, 15, 15, 45, 30, 8),
        Character("AYLA", "#F5C542", 250, 250, 10, 10, 55, 20, 16),
        Character("MAGUS", "#2A2A5A", 180, 180, 60, 60, 35, 15, 14)
    )

    // Enemigos
    private data class Enemy(val name: String, val hp: Int, var currentHp: Int, val attack: Int, val defense: Int, val exp: Int, val color: String)
    private val enemies = mutableListOf<Enemy>()
    private val enemyTemplates = listOf(
        Triple("IMP", 30, "#D82800"),
        Triple("GOBLIN", 50, "#00A800"),
        Triple("SKELETON", 80, "#F8F8F8"),
        Triple("GOLEM", 150, "#7C7C7C"),
        Triple("LAVOS SPAWN", 300, "#8814A8")
    )

    // Combate
    private var battleTurn = 0  // 0=player, 1=enemy
    private var selectedAction = 0  // 0=attack, 1=tech, 2=item, 3=defend
    private var selectedTech = 0
    private var selectedTarget = 0
    private var battleMessage = ""
    private var battleMessageTimer = 0f
    private var battleAnimTimer = 0f

    // Techs (habilidades)
    private data class Tech(val name: String, val mpCost: Int, val power: Int, val type: Int)  // 0=single, 1=all, 2=heal
    private val techs = mapOf(
        "CRONO" to listOf(Tech("Cyclone", 2, 30, 1), Tech("Slash", 4, 50, 0), Tech("Luminaire", 20, 120, 1)),
        "MARLE" to listOf(Tech("Aura", 1, 20, 2), Tech("Ice", 3, 40, 0), Tech("Life", 10, 60, 2)),
        "LUCCA" to listOf(Tech("Flame", 2, 35, 0), Tech("Hypno", 5, 0, 1), Tech("Fire", 15, 100, 1)),
        "FROG" to listOf(Tech("Water", 2, 30, 0), Tech("Leap", 4, 45, 0), Tech("Frog Squash", 12, 90, 1)),
        "ROBO" to listOf(Tech("Rocket", 3, 40, 0), Tech("Heal", 5, 30, 2), Tech("Uzzi", 15, 80, 1)),
        "AYLA" to listOf(Tech("Kiss", 2, 25, 2), Tech("Boulder", 4, 50, 0), Tech("Dino Tail", 12, 85, 1)),
        "MAGUS" to listOf(Tech("Ice 2", 8, 60, 1), Tech("Fire 2", 8, 60, 1), Tech("Dark Matter", 20, 150, 1))
    )

    // Tech combos (doble tech)
    private data class Combo(val name: String, val chars: List<String>, val power: Int, val mpCost: Int)
    private val combos = listOf(
        Combo("X Strike", listOf("CRONO", "FROG"), 80, 6),
        Combo("Aura Whirl", listOf("CRONO", "MARLE"), 70, 8),
        Combo("Fire Whirl", listOf("CRONO", "LUCCA"), 75, 8),
        Combo("Ice Sword", listOf("FROG", "MARLE"), 65, 6),
        Combo("Antipode", listOf("LUCCA", "MARLE"), 90, 10),
        Combo("Boogie", listOf("AYLA", "ROBO"), 60, 8)
    )

    // Mapa (campos)
    private data class Location(val name: String, val era: String, val description: String)
    private val locations = listOf(
        Location("GUARDIA CASTLE", "1000 AD", "El castillo del reino de Guardia"),
        Location("TRUCE VILLAGE", "1000 AD", "Pueblo pacífico en el presente"),
        Location("MILLINIAL FAIR", "1000 AD", "Feria del milenio"),
        Location("FOREST", "65M BC", "Bosque prehistórico"),
        Location("REPTITE LAIR", "65M BC", "Guarida de los Reptites"),
        Location("TYRANO LAIR", "65M BC", "Fortaleza de Azala"),
        Location("SEWERS", "2300 AD", "Alcantarillas del futuro"),
        Location("FACTORY", "2300 AD", "Fábrica abandonada"),
        Location("BANGOR DOME", "2300 AD", "Domo de supervivientes"),
        Location("END OF TIME", "END OF TIME", "El fin del tiempo"),
        Location("LAVOS LAIR", "1999 AD", "Guarida de Lavos")
    )
    private var currentLocation = 0
    private var locationIndex = 0

    // Diálogo
    private var dialogText = ""
    private var dialogSpeaker = ""
    private var dialogQueue = mutableListOf<Pair<String, String>>()

    // Puntuación final
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0

    // Paints
    private val bgPaint = paintOf("#05050A")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val bluePaint = paintOf("#5C94FC").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        state = State.TITLE
        characters.clear()
        characters.addAll(allCharacters.map { it.copy() })
        party.clear()
        party.addAll(listOf(0, 1, 2))  // Crono, Marle, Lucca
        currentLocation = 0
        locationIndex = 0
        scoreVal = 0
        battleMessage = ""
        battleMessageTimer = 0f
        dialogQueue.clear()
        // Diálogo de intro
        dialogQueue.add("CRONO" to "¡El festival del milenio comienza hoy!")
        dialogQueue.add("MARLE" to "¡Vamos, Crono! ¡No te quedes atrás!")
        dialogQueue.add("LUCCA" to "He terminado mi nueva máquina del tiempo...")
    }

    private fun startBattle(enemyNames: List<String>) {
        enemies.clear()
        for (name in enemyNames) {
            val template = enemyTemplates.find { it.first == name } ?: enemyTemplates[0]
            enemies.add(Enemy(template.first, template.second, template.second, template.second / 5, template.second / 10, template.second, template.third))
        }
        battleTurn = 0
        selectedAction = 0
        selectedTech = 0
        selectedTarget = 0
        battleMessage = "¡Enemigos aparecen!"
        battleMessageTimer = 2f
        state = State.BATTLE
    }

    private fun doPlayerAttack() {
        val attacker = characters[party[battleTurn % party.size]]
        val target = enemies.getOrElse(selectedTarget) { enemies[0] }
        val damage = (attacker.attack - target.defense).coerceAtLeast(1)
        target.currentHp = (target.currentHp - damage).coerceAtLeast(0)
        battleMessage = "${attacker.name} ataca a ${target.name} por $damage de daño!"
        battleMessageTimer = 1.5f
        if (target.currentHp <= 0) {
            enemies.remove(target)
            scoreVal += target.exp
            if (scoreVal > best) best = scoreVal
            // Dar EXP al party
            for (idx in party) {
                characters[idx].exp += target.exp
                if (characters[idx].exp >= characters[idx].level * 100) {
                    characters[idx].level++
                    characters[idx].maxHp += 20
                    characters[idx].hp = characters[idx].maxHp
                    characters[idx].maxMp += 5
                    characters[idx].mp = characters[idx].maxMp
                    battleMessage = "${characters[idx].name} sube a nivel ${characters[idx].level}!"
                    battleMessageTimer = 2f
                }
            }
        }
        if (enemies.isEmpty()) {
            battleMessage = "¡Victoria!"
            battleMessageTimer = 2f
            state = State.FIELD
        } else {
            battleTurn++
            if (battleTurn >= party.size) {
                battleTurn = 0
                state = State.BATTLE  // enemy turn handled below
                doEnemyTurn()
            }
        }
    }

    private fun doPlayerTech() {
        val attacker = characters[party[battleTurn % party.size]]
        val techList = techs[attacker.name] ?: emptyList()
        if (selectedTech >= techList.size) return
        val tech = techList[selectedTech]
        if (attacker.mp < tech.mpCost) {
            battleMessage = "¡MP insuficiente!"
            battleMessageTimer = 1.5f
            return
        }
        attacker.mp -= tech.mpCost
        val target = enemies.getOrElse(selectedTarget) { enemies[0] }
        val damage = tech.power + attacker.attack / 2 - target.defense
        if (tech.type == 2) {
            // Heal
            val healTarget = characters[party[0]]
            healTarget.hp = (healTarget.hp + tech.power).coerceAtMost(healTarget.maxHp)
            battleMessage = "${attacker.name} usa ${tech.name}! Cura ${tech.power} HP a ${healTarget.name}!"
        } else {
            if (tech.type == 1) {
                // All enemies
                for (e in enemies) {
                    e.currentHp = (e.currentHp - damage).coerceAtLeast(0)
                }
                battleMessage = "${attacker.name} usa ${tech.name}! ${damage} de daño a todos!"
            } else {
                target.currentHp = (target.currentHp - damage).coerceAtLeast(0)
                battleMessage = "${attacker.name} usa ${tech.name}! ${damage} de daño a ${target.name}!"
            }
            // Remover enemigos muertos
            val dead = enemies.filter { it.currentHp <= 0 }
            for (d in dead) {
                enemies.remove(d)
                scoreVal += d.exp
                if (scoreVal > best) best = scoreVal
                for (idx in party) {
                    characters[idx].exp += d.exp
                    if (characters[idx].exp >= characters[idx].level * 100) {
                        characters[idx].level++
                        characters[idx].maxHp += 20
                        characters[idx].hp = characters[idx].maxHp
                        characters[idx].maxMp += 5
                        characters[idx].mp = characters[idx].maxMp
                    }
                }
            }
        }
        battleMessageTimer = 1.5f
        if (enemies.isEmpty()) {
            battleMessage = "¡Victoria!"
            battleMessageTimer = 2f
            state = State.FIELD
        } else {
            battleTurn++
            if (battleTurn >= party.size) {
                battleTurn = 0
                doEnemyTurn()
            }
        }
    }

    private fun doEnemyTurn() {
        for (e in enemies) {
            val target = party.random()
            val char = characters[target]
            val damage = (e.attack - char.defense).coerceAtLeast(1)
            char.hp = (char.hp - damage).coerceAtLeast(0)
            battleMessage = "${e.name} ataca a ${char.name} por $damage de daño!"
            battleMessageTimer = 1.5f
            if (char.hp <= 0) {
                battleMessage = "${char.name} ha caído!"
                battleMessageTimer = 2f
                party.remove(target)
                if (party.isEmpty()) {
                    state = State.GAME_OVER
                    if (scoreVal > best) best = scoreVal
                }
            }
        }
        battleTurn = 0
    }

    override fun update(dt: Float, input: GameInput) {
        battleMessageTimer = (battleMessageTimer - dt).coerceAtLeast(0f)

        when (state) {
            State.TITLE -> {
                if (input.start || input.a) {
                    state = State.FIELD
                    input.start = false; input.a = false
                }
            }
            State.FIELD -> {
                // Navegación entre ubicaciones
                if (input.up) { locationIndex = (locationIndex - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { locationIndex = (locationIndex + 1).coerceAtMost(locations.size - 1); input.down = false }
                if (input.a || input.start) {
                    // Entrar a ubicación
                    currentLocation = locationIndex
                    // Encuentro aleatorio
                    if (Random.nextFloat() < 0.4f) {
                        val enemyCount = Random.nextInt(1, 4)
                        val enemyNames = (0 until enemyCount).map { enemyTemplates.random().first }
                        startBattle(enemyNames)
                    } else {
                        // Diálogo
                        dialogQueue.clear()
                        dialogQueue.add(locations[currentLocation].name to locations[currentLocation].description)
                        state = State.DIALOG
                    }
                    input.a = false; input.start = false
                }
                if (input.b) {
                    // Menú
                    state = State.MENU
                    input.b = false
                }
            }
            State.DIALOG -> {
                if (input.a || input.start) {
                    if (dialogQueue.isNotEmpty()) {
                        val (speaker, text) = dialogQueue.removeAt(0)
                        dialogSpeaker = speaker
                        dialogText = text
                    } else {
                        state = State.FIELD
                    }
                    input.a = false; input.start = false
                }
            }
            State.MENU -> {
                if (input.b) {
                    state = State.FIELD
                    input.b = false
                }
            }
            State.BATTLE -> {
                if (battleMessageTimer > 0f) return
                if (input.up) { selectedAction = (selectedAction - 1).coerceAtLeast(0); input.up = false }
                if (input.down) { selectedAction = (selectedAction + 1).coerceAtMost(3); input.down = false }
                if (input.left) { selectedTech = (selectedTech - 1).coerceAtLeast(0); input.left = false }
                if (input.right) { selectedTech = (selectedTech + 1).coerceAtMost(2); input.right = false }
                if (input.a) {
                    when (selectedAction) {
                        0 -> doPlayerAttack()
                        1 -> doPlayerTech()
                        2 -> { /* item - not implemented */ }
                        3 -> { /* defend */ }
                    }
                    input.a = false
                }
                if (input.start) {
                    // Boss final
                    if (currentLocation == locations.size - 1) {
                        startBattle(listOf("LAVOS SPAWN", "LAVOS SPAWN", "LAVOS SPAWN"))
                    }
                    input.start = false
                }
            }
            State.GAME_OVER -> {
                if (input.start || input.a) reset()
            }
            State.VICTORY -> {
                if (input.start || input.a) reset()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#05050A"))

        when (state) {
            State.TITLE -> drawTitle(canvas)
            State.FIELD -> drawField(canvas)
            State.BATTLE -> drawBattle(canvas)
            State.DIALOG -> drawDialog(canvas)
            State.MENU -> drawMenu(canvas)
            State.GAME_OVER -> drawGameOver(canvas)
            State.VICTORY -> drawVictory(canvas)
        }
    }

    private fun drawTitle(canvas: Canvas) {
        titlePaint.textSize = 48f
        titlePaint.color = Color.parseColor("#F5C542")
        canvas.drawText("CRÓNICA DEL TIEMPO", w / 2f, h / 2f - 40f, titlePaint)
        smallPaint.textSize = 18f
        canvas.drawText("Un RPG de viaje en el tiempo", w / 2f, h / 2f + 10f, smallPaint)
        canvas.drawText("Basado en Chrono Trigger", w / 2f, h / 2f + 40f, smallPaint)
        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA COMENZAR", w / 2f, h / 2f + 80f, greenPaint)
    }

    private fun drawField(canvas: Canvas) {
        // Ubicación actual
        titlePaint.textSize = 24f
        canvas.drawText(locations[currentLocation].name, w / 2f, 30f, titlePaint)
        smallPaint.textSize = 14f
        canvas.drawText(locations[currentLocation].era, w / 2f, 55f, smallPaint)

        // Lista de ubicaciones
        for (i in locations.indices) {
            val y = 80f + i * 22f
            val isSelected = i == locationIndex
            if (isSelected) {
                val selPaint = paintOf("#F5C542")
                selPaint.alpha = 40
                canvas.drawRect(20f, y - 14f, w - 20f, y + 6f, selPaint)
            }
            val locPaint = if (isSelected) titlePaint else smallPaint
            locPaint.textSize = if (isSelected) 16f else 14f
            canvas.drawText("${locations[i].name}  (${locations[i].era})", w / 2f, y, locPaint)
        }

        // Party info
        val partyY = h - 80f
        for ((i, idx) in party.withIndex()) {
            val char = characters[idx]
            val x = 20f + i * (w / 3f)
            smallPaint.textSize = 12f
            canvas.drawText("${char.name} Lv.${char.level}", x, partyY, smallPaint)
            // HP bar
            val barW = 60f
            val barH = 6f
            val barBg = paintOf("#2A2A3E")
            canvas.drawRect(x, partyY + 5f, x + barW, partyY + 5f + barH, barBg)
            val barFill = paintOf("#00E676")
            canvas.drawRect(x, partyY + 5f, x + barW * (char.hp.toFloat() / char.maxHp), partyY + 5f + barH, barFill)
        }

        // Instrucciones
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("▲▼: navegar  ·  A: entrar  ·  B: menú", w / 2f, h - 10f, smallPaint)
    }

    private fun drawBattle(canvas: Canvas) {
        // Fondo de batalla
        canvas.drawColor(Color.parseColor("#1A1A3E"))

        // Enemigos
        for ((i, e) in enemies.withIndex()) {
            val x = w * 0.7f
            val y = 80f + i * 60f
            val ePaint = paintOf(e.color)
            canvas.drawCircle(x, y, 25f, ePaint)
            // HP bar
            val barW = 80f
            val barH = 6f
            val barBg = paintOf("#2A2A3E")
            canvas.drawRect(x - barW / 2f, y + 30f, x + barW / 2f, y + 30f + barH, barBg)
            val barFill = paintOf("#FF5252")
            canvas.drawRect(x - barW / 2f, y + 30f, x - barW / 2f + barW * (e.currentHp.toFloat() / e.hp), y + 30f + barH, barFill)
            smallPaint.textSize = 12f
            canvas.drawText("${e.name} ${e.currentHp}/${e.hp}", x, y - 35f, smallPaint)
        }

        // Party
        for ((i, idx) in party.withIndex()) {
            val char = characters[idx]
            val x = w * 0.2f
            val y = 80f + i * 60f
            val cPaint = paintOf(char.color)
            canvas.drawCircle(x, y, 20f, cPaint)
            // HP bar
            val barW = 80f
            val barH = 6f
            val barBg = paintOf("#2A2A3E")
            canvas.drawRect(x - barW / 2f, y + 25f, x + barW / 2f, y + 25f + barH, barBg)
            val barFill = paintOf("#00E676")
            canvas.drawRect(x - barW / 2f, y + 25f, x - barW / 2f + barW * (char.hp.toFloat() / char.maxHp), y + 25f + barH, barFill)
            smallPaint.textSize = 12f
            canvas.drawText("${char.name} ${char.hp}/${char.maxHp}", x, y - 28f, smallPaint)
            // MP
            canvas.drawText("MP: ${char.mp}/${char.maxMp}", x, y - 15f, smallPaint.apply { textSize = 10f })
        }

        // Menú de acciones
        val actions = listOf("ATACAR", "TECH", "ITEM", "DEFENDER")
        for (i in actions.indices) {
            val y = h - 80f + i * 20f
            val isSelected = i == selectedAction
            if (isSelected) {
                val selPaint = paintOf("#F5C542")
                selPaint.alpha = 40
                canvas.drawRect(20f, y - 14f, w / 2f - 20f, y + 6f, selPaint)
            }
            val actPaint = if (isSelected) titlePaint else smallPaint
            actPaint.textSize = 16f
            canvas.drawText(actions[i], w / 4f, y, actPaint)
        }

        // Techs (si TECH seleccionado)
        if (selectedAction == 1) {
            val char = characters[party[battleTurn % party.size]]
            val techList = techs[char.name] ?: emptyList()
            for (i in techList.indices) {
                val y = h - 80f + i * 20f
                val isSelected = i == selectedTech
                if (isSelected) {
                    val selPaint = paintOf("#F5C542")
                    selPaint.alpha = 40
                    canvas.drawRect(w / 2f + 20f, y - 14f, w - 20f, y + 6f, selPaint)
                }
                val techPaint = if (isSelected) titlePaint else smallPaint
                techPaint.textSize = 14f
                canvas.drawText("${techList[i].name} (${techList[i].mpCost} MP)", 3 * w / 4f, y, techPaint)
            }
        }

        // Mensaje de batalla
        if (battleMessageTimer > 0f) {
            val alpha = (battleMessageTimer / 2f * 255).toInt().coerceIn(0, 255)
            val msgPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); this.alpha = alpha; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 16f; isFakeBoldText = true }
            canvas.drawText(battleMessage, w / 2f, h / 2f, msgPaint)
        }

        // Instrucciones
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("▲▼: acción  ·  ◀▶: tech  ·  A: confirmar  ·  START: boss final", w / 2f, h - 10f, smallPaint)
    }

    private fun drawDialog(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#05050A"))
        // Fondo de ubicación
        titlePaint.textSize = 24f
        canvas.drawText(locations[currentLocation].name, w / 2f, 30f, titlePaint)
        smallPaint.textSize = 14f
        canvas.drawText(locations[currentLocation].era, w / 2f, 55f, smallPaint)

        // Diálogo
        val dialogPaint = paintOf("#080812")
        canvas.drawRect(20f, h - 120f, w - 20f, h - 20f, dialogPaint)
        val borderPaint = paintOf("#F8F8F8")
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 2f
        canvas.drawRect(20f, h - 120f, w - 20f, h - 20f, borderPaint)

        if (dialogSpeaker.isNotEmpty()) {
            val speakerPaint = paintOf("#F5C542")
            speakerPaint.textSize = 16f
            speakerPaint.isFakeBoldText = true
            canvas.drawText("▶ $dialogSpeaker", 30f, h - 100f, speakerPaint)
        }
        textPaint.textSize = 14f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(dialogText, 30f, h - 75f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER

        smallPaint.textSize = 12f
        canvas.drawText("PULSA A PARA CONTINUAR", w / 2f, h - 30f, smallPaint)
    }

    private fun drawMenu(canvas: Canvas) {
        titlePaint.textSize = 32f
        canvas.drawText("MENÚ", w / 2f, 40f, titlePaint)
        smallPaint.textSize = 16f
        canvas.drawText("B: volver al campo", w / 2f, 80f, smallPaint)
        // Party info
        for ((i, idx) in party.withIndex()) {
            val char = characters[idx]
            val y = 120f + i * 40f
            canvas.drawText("${char.name}  Lv.${char.level}  HP:${char.hp}/${char.maxHp}  MP:${char.mp}/${char.maxMp}", w / 2f, y, smallPaint)
        }
    }

    private fun drawGameOver(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#05050A"))
        redPaint.textSize = 48f
        canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
        smallPaint.textSize = 22f
        canvas.drawText("Tu party ha caído...", w / 2f, h / 2f + 10f, smallPaint)
        canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 40f, smallPaint)
        if (scoreVal >= best && scoreVal > 0) {
            titlePaint.textSize = 22f
            canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
        }
        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
    }

    private fun drawVictory(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#05050A"))
        titlePaint.textSize = 48f
        titlePaint.color = Color.parseColor("#F5C542")
        canvas.drawText("¡VICTORIA!", w / 2f, h / 2f - 40f, titlePaint)
        smallPaint.textSize = 22f
        canvas.drawText("Derrotaste a Lavos", w / 2f, h / 2f, smallPaint)
        canvas.drawText("Salvaste el mundo", w / 2f, h / 2f + 30f, smallPaint)
        canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 60f, smallPaint)
        if (scoreVal >= best && scoreVal > 0) {
            titlePaint.textSize = 22f
            canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 90f, titlePaint)
        }
        greenPaint.textSize = 20f
        canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 130f, greenPaint)
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
