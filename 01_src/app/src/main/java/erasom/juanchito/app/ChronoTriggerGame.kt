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

    // World map
    private val tileSize = 32
    private var mapW = 0; private var mapH = 0
    private val map = mutableListOf<IntArray>()
    private var px = 0f; private var py = 0f
    private var camX = 0f; private var camY = 0f

    // Party
    private data class Hero(var name: String, var hp: Int, var maxHp: Int, var mp: Int, var maxMp: Int, var atb: Float, var color: Int, var tech: String)
    private val party = mutableListOf<Hero>()
    private var activeHero = 0

    // Battle
    private var inBattle = false
    private data class Enemy(var name: String, var hp: Int, var maxHp: Int, var atb: Float, var color: Int, var x: Float, var y: Float)
    private val enemies = mutableListOf<Enemy>()
    private var battleMsg = ""
    private var battleTimer = 0f
    private var encounterCooldown = 0f

    // State
    private var state = 0 // 0 explore, 1 battle, 2 victory, 3 defeat, 4 gameover
    override var score = 0
    private var xp = 0
    private var level = 1
    private var gold = 0
    override var isGameOver = false
    private var won = false
    private var highScore = 0

    // Colors (NES-ish but colorful)
    private val grassPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val waterPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val treePaint = Paint().apply { color = Color.parseColor("#228B22"); isAntiAlias = false }
    private val pathPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val heroPaint = Paint().apply { isAntiAlias = false }
    private val enemyPaint = Paint().apply { isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 10f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val hpPaint = Paint().apply { isAntiAlias = false }
    private val panelPaint = Paint().apply { color = Color.parseColor("#000000"); alpha = 180; isAntiAlias = false }

    override fun init(width: Int, height: Int) { w = width; h = height; reset() }

    private fun genMap() {
        map.clear()
        mapW = (w / tileSize).coerceAtLeast(12)
        mapH = (h / tileSize).coerceAtLeast(10)
        for (r in 0 until mapH) {
            val row = IntArray(mapW)
            for (c in 0 until mapW) {
                val v = Random.nextInt(100)
                row[c] = when {
                    v < 5 -> 2 // tree
                    v < 12 -> 1 // water
                    v < 20 -> 3 // path
                    else -> 0 // grass
                }
            }
            map.add(row)
        }
        // ensure center walkable
        for (r in mapH / 2 - 1..mapH / 2 + 1) for (c in mapW / 2 - 1..mapW / 2 + 1) map[r][c] = 0
    }

    override fun reset() {
        genMap()
        px = mapW / 2f * tileSize + tileSize / 2f
        py = mapH / 2f * tileSize + tileSize / 2f
        party.clear()
        party.add(Hero("Crono", 50, 50, 10, 10, 0f, Color.parseColor("#F8D878"), "Cyclone"))
        party.add(Hero("Marle", 40, 40, 20, 20, 0f, Color.parseColor("#5C94FC"), "Aura"))
        party.add(Hero("Lucca", 35, 35, 25, 25, 0f, Color.parseColor("#D82800"), "Fire"))
        activeHero = 0
        inBattle = false; enemies.clear(); battleMsg = ""; battleTimer = 0f; encounterCooldown = 4f
        state = 0; xp = 0; level = 1; gold = 0; score = 0; isGameOver = false; won = false
    }

    private fun startBattle() {
        inBattle = true; state = 1
        enemies.clear()
        val count = 1 + Random.nextInt(3)
        val names = listOf("Imp", "Goblin", "Reptite", "Ogan", "Basilisk")
        val cols = listOf(Color.parseColor("#00A800"), Color.parseColor("#FC9838"), Color.parseColor("#8814A8"), Color.parseColor("#D82800"), Color.parseColor("#F8D878"))
        for (i in 0 until count) {
            val n = names[Random.nextInt(names.size)]
            val hp = 15 + Random.nextInt(20) + level * 5
            enemies.add(Enemy(n, hp, hp, Random.nextFloat() * 0.3f, cols[Random.nextInt(cols.size)], w * (0.55f + i * 0.12f), h * 0.35f))
        }
        battleMsg = "¡Enemigos aparecen!"
        for (h in party) h.atb = 0f
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        if (state == 0) {
            // explore
            var dx = 0f; var dy = 0f
            if (input.up) dy -= 1f
            if (input.down) dy += 1f
            if (input.left) dx -= 1f
            if (input.right) dx += 1f
            val sp = 100f * dt
            val nx = px + dx * sp; val ny = py + dy * sp
            val tc = (nx / tileSize).toInt().coerceIn(0, mapW - 1)
            val tr = (ny / tileSize).toInt().coerceIn(0, mapH - 1)
            if (map[tr][tc] != 2 && map[tr][tc] != 1) { px = nx; py = ny }
            // camera
            camX = (px - w / 2f).coerceIn(0f, (mapW * tileSize - w).coerceAtLeast(0).toFloat())
            camY = (py - h / 2f).coerceIn(0f, (mapH * tileSize - h).coerceAtLeast(0).toFloat())
            // encounter
            encounterCooldown -= dt
            if (encounterCooldown <= 0f && (dx != 0f || dy != 0f)) {
                if (Random.nextFloat() < 0.008f) { startBattle(); encounterCooldown = 3f }
            }
        } else if (state == 1) {
            battleTimer += dt
            // ATB
            for (h in party) if (h.hp > 0) h.atb = (h.atb + dt * 0.4f).coerceAtMost(1f)
            for (e in enemies) e.atb = (e.atb + dt * 0.35f).coerceAtMost(1f)
            // player action
            val hero = party[activeHero]
            if (hero.hp > 0 && hero.atb >= 1f) {
                if (input.a) {
                    // attack
                    val target = enemies.filter { it.hp > 0 }.minByOrNull { kotlin.math.abs(it.x - hero.color) } ?: enemies.firstOrNull { it.hp > 0 }
                    target?.let {
                        val dmg = 8 + Random.nextInt(8) + level * 2
                        it.hp -= dmg; hero.atb = 0f; battleMsg = "${hero.name} ataca por $dmg"
                        if (it.hp <= 0) { score += 20; xp += 15; gold += 10; battleMsg = "${it.name} derrotado!" }
                    }
                    input.a = false
                }
                if (input.b) {
                    // tech
                    hero.atb = 0f
                    when (hero.tech) {
                        "Cyclone" -> { for (e in enemies) if (e.hp > 0) { val d = 12 + Random.nextInt(8); e.hp -= d; if (e.hp <= 0) { score += 20; xp += 15; gold += 10 } }; battleMsg = "Cyclone!" }
                        "Aura" -> { for (h in party) h.hp = (h.hp + 15).coerceAtMost(h.maxHp); battleMsg = "Aura cura!" }
                        "Fire" -> { val t = enemies.filter { it.hp > 0 }.firstOrNull(); t?.let { val d = 18 + Random.nextInt(10); it.hp -= d; if (it.hp <= 0) { score += 20; xp += 15; gold += 10 }; battleMsg = "Fire! $d" } }
                    }
                    input.b = false
                }
                if (input.select) { activeHero = (activeHero + 1) % party.size; while (party[activeHero].hp <= 0) activeHero = (activeHero + 1) % party.size; input.select = false }
            }
            // enemy action
            for (e in enemies) {
                if (e.hp > 0 && e.atb >= 1f) {
                    e.atb = 0f
                    val target = party.filter { it.hp > 0 }.randomOrNull()
                    target?.let {
                        val dmg = 4 + Random.nextInt(6) + level
                        it.hp -= dmg; battleMsg = "${e.name} ataca a ${it.name} por $dmg"
                        if (it.hp <= 0) { battleMsg = "${it.name} cayó!" }
                    }
                }
            }
            // check battle end
            if (enemies.all { it.hp <= 0 }) {
                state = 2; battleTimer = 0f; battleMsg = "¡Victoria!"
                // level up check
                if (xp >= level * 30) { level++; for (h in party) { h.maxHp += 10; h.hp = h.maxHp; h.maxMp += 5; h.mp = h.maxMp }; battleMsg = "¡Subiste a nivel $level!" }
            }
            if (party.all { it.hp <= 0 }) { state = 3; battleMsg = "Derrota..." }
        } else if (state == 2 || state == 3) {
            battleTimer += dt
            if (battleTimer > 2f) {
                if (state == 2) { inBattle = false; state = 0; encounterCooldown = 4f; enemies.clear() }
                else { isGameOver = true; won = false }
            }
        }
        if (score > highScore) highScore = score
    }

    override fun draw(canvas: Canvas) {
        if (state == 0) {
            // world map
            canvas.drawColor(Color.parseColor("#5C94FC"))
            for (r in 0 until mapH) for (c in 0 until mapW) {
                val x = c * tileSize - camX; val y = r * tileSize - camY
                if (x + tileSize < 0 || y + tileSize < 0 || x > w || y > h) continue
                when (map[r][c]) {
                    0 -> canvas.drawRect(x, y, x + tileSize, y + tileSize, grassPaint)
                    1 -> canvas.drawRect(x, y, x + tileSize, y + tileSize, waterPaint)
                    2 -> canvas.drawRect(x, y, x + tileSize, y + tileSize, treePaint)
                    3 -> canvas.drawRect(x, y, x + tileSize, y + tileSize, pathPaint)
                }
            }
            // party leader
            val cx = px - camX; val cy = py - camY
            heroPaint.color = party[0].color
            canvas.drawRoundRect(RectF(cx - 8f, cy - 12f, cx + 8f, cy + 4f), 3f, 3f, heroPaint)
            canvas.drawRect(cx - 4f, cy - 20f, cx + 4f, cy - 12f, heroPaint)
            // HUD
            textPaint.textSize = 11f
            canvas.drawText("Nivel: $level  XP: $xp  Oro: $gold  Score: $score", w / 2f, 14f, textPaint)
            smallPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Explora el mapa. Encuentros aleatorios.", 8f, h - 8f, smallPaint)
            smallPaint.textAlign = Paint.Align.CENTER
        } else {
            // battle scene
            canvas.drawColor(Color.parseColor("#2D3748"))
            // background gradient-ish (flat bands)
            val bandPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, bandPaint)
            val groundPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
            canvas.drawRect(0f, h * 0.5f, w.toFloat(), h.toFloat(), groundPaint)
            // enemies
            for (e in enemies) {
                if (e.hp <= 0) continue
                enemyPaint.color = e.color
                canvas.drawRoundRect(RectF(e.x - 18f, e.y - 24f, e.x + 18f, e.y + 8f), 4f, 4f, enemyPaint)
                // hp bar
                hpPaint.color = Color.parseColor("#D82800")
                canvas.drawRect(e.x - 18f, e.y - 30f, e.x - 18f + 36f * (e.hp.toFloat() / e.maxHp), e.y - 26f, hpPaint)
                hpPaint.color = Color.parseColor("#2D3748")
                canvas.drawRect(e.x - 18f + 36f * (e.hp.toFloat() / e.maxHp), e.y - 30f, e.x + 18f, e.y - 26f, hpPaint)
                smallPaint.textSize = 9f
                canvas.drawText(e.name, e.x, e.y - 32f, smallPaint)
            }
            // party
            for (i in party.indices) {
                val hero = party[i]
                if (hero.hp <= 0) continue
                val hx = w * (0.15f + i * 0.12f); val hy = h * 0.75f
                heroPaint.color = hero.color
                canvas.drawRoundRect(RectF(hx - 12f, hy - 20f, hx + 12f, hy + 4f), 3f, 3f, heroPaint)
                canvas.drawRect(hx - 6f, hy - 28f, hx + 6f, hy - 20f, heroPaint)
                // hp bar
                hpPaint.color = Color.parseColor("#00A800")
                canvas.drawRect(hx - 14f, hy + 8f, hx - 14f + 28f * (hero.hp.toFloat() / hero.maxHp), hy + 12f, hpPaint)
                hpPaint.color = Color.parseColor("#2D3748")
                canvas.drawRect(hx - 14f + 28f * (hero.hp.toFloat() / hero.maxHp), hy + 8f, hx + 14f, hy + 12f, hpPaint)
                // atb bar
                hpPaint.color = Color.parseColor("#5C94FC")
                canvas.drawRect(hx - 14f, hy + 14f, hx - 14f + 28f * hero.atb, hy + 16f, hpPaint)
                smallPaint.textSize = 9f
                canvas.drawText(hero.name, hx, hy - 30f, smallPaint)
                if (i == activeHero && hero.atb >= 1f) {
                    val selPaint = Paint().apply { color = Color.parseColor("#F8D878"); strokeWidth = 2f; style = Paint.Style.STROKE; isAntiAlias = false }
                    canvas.drawRect(hx - 16f, hy - 22f, hx + 16f, hy + 6f, selPaint)
                }
            }
            // message panel
            canvas.drawRect(0f, h - 40f, w.toFloat(), h.toFloat(), panelPaint)
            textPaint.textSize = 11f
            canvas.drawText(battleMsg, w / 2f, h - 24f, textPaint)
            smallPaint.textSize = 10f
            canvas.drawText("A: Atacar  B: Técnica  SELECT: Cambiar", w / 2f, h - 10f, smallPaint)
            // score
            textPaint.textSize = 11f
            canvas.drawText("Score: $score  Nivel: $level  XP: $xp", w / 2f, 14f, textPaint)
        }
        if (isGameOver) {
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), panelPaint)
            val msg = if (won) "¡VICTORIA!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 32f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 12f; textPaint.color = Color.parseColor("#F8F8F8")
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 24f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}