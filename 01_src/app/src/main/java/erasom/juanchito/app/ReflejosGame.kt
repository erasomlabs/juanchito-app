package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Reflejos: reconstrucción fiel de Dexter's Lab Brain Reaction (Turner/Player X, 2008).
 */
class ReflejosGame : Game {
    override val name = "Reflejos"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Moléculas
    private data class Molecule(val x: Float, var y: Float, val color: Int, val speed: Float, val type: Int)
    private val molecules = mutableListOf<Molecule>()
    private var spawnTimer = 0f
    private var spawnInterval = 1.2f

    // Zonas
    private val numZones = 3
    private var zoneWidth = 0f
    private var selectedZone = 1

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var combo = 0
    private var maxCombo = 0
    private var zapped = 0
    private var missed = 0

    // Vida
    private var life = 100f
    private val maxLife = 100f

    // Nivel
    private var level = 1
    private var levelTimer = 0f
    private val levelDuration = 30f
    private var moleculesToZap = 20
    private var zappedThisLevel = 0

    // Animaciones
    private var zapFlash = 0f
    private var zapX = 0f
    private var zapY = 0f

    // Colores
    private val moleculeColors = listOf("#5C94FC", "#F8D878", "#D82800", "#00A800", "#8814A8")
    private val zoneColors = listOf("#5C94FC", "#F5C542", "#D82800")

    // Paints (definidos después de paintOf)
    private lateinit var bgPaint: Paint
    private lateinit var textPaint: Paint
    private lateinit var titlePaint: Paint
    private lateinit var smallPaint: Paint
    private lateinit var hudPaint: Paint
    private lateinit var greenPaint: Paint
    private lateinit var redPaint: Paint

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        zoneWidth = w / numZones.toFloat()
        // Inicializar paints
        bgPaint = paintOf("#1A1A2E")
        textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
        hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
        greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        reset()
    }

    override fun reset() {
        molecules.clear()
        spawnTimer = 0f
        spawnInterval = 1.2f
        scoreVal = 0
        combo = 0
        maxCombo = 0
        zapped = 0
        missed = 0
        life = maxLife
        level = 1
        levelTimer = 0f
        moleculesToZap = 20
        zappedThisLevel = 0
        state = State.PLAYING
        selectedZone = 1
        zapFlash = 0f
    }

    private fun spawnMolecule() {
        val type = Random.nextInt(3)
        val color = Color.parseColor(moleculeColors[Random.nextInt(moleculeColors.size)])
        val speed = when (type) {
            1 -> Random.nextFloat() * 100f + 150f
            2 -> Random.nextFloat() * 50f + 80f
            else -> Random.nextFloat() * 80f + 100f
        }
        val x = Random.nextFloat() * (w - 60f) + 30f
        molecules.add(Molecule(x, -40f, color, speed, type))
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.PLAYING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.LEVEL_COMPLETE) {
                    level++
                    moleculesToZap += 5
                    spawnInterval = (spawnInterval * 0.9f).coerceAtLeast(0.4f)
                    zappedThisLevel = 0
                    state = State.PLAYING
                }
            }
            return
        }

        levelTimer += dt
        spawnTimer += dt
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnMolecule()
        }
        if (zapFlash > 0f) zapFlash -= dt * 4f

        val it = molecules.iterator()
        while (it.hasNext()) {
            val m = it.next()
            m.y += m.speed * dt
            if (m.y > h + 40f) {
                if (m.type != 2) {
                    missed++
                    combo = 0
                    life -= 3f
                    if (life <= 0f) {
                        life = 0f
                        state = State.GAME_OVER
                    }
                }
                it.remove()
            }
        }

        if (input.left) { selectedZone = (selectedZone - 1).coerceAtLeast(0); input.left = false }
        if (input.right) { selectedZone = (selectedZone + 1).coerceAtMost(numZones - 1); input.right = false }
        if (input.a || input.start) {
            val zoneLeft = selectedZone * zoneWidth
            val zoneRight = zoneLeft + zoneWidth
            val toRemove = mutableListOf<Molecule>()
            for (m in molecules) {
                if (m.x in zoneLeft..zoneRight && m.y in (h * 0.3f)..(h * 0.8f)) {
                    toRemove.add(m)
                }
            }
            if (toRemove.isNotEmpty()) {
                for (m in toRemove) {
                    molecules.remove(m)
                    zapped++
                    zappedThisLevel++
                    val pts = when (m.type) {
                        1 -> 150
                        2 -> 50
                        else -> 100
                    } + combo * 10
                    scoreVal += pts
                    if (scoreVal > best) best = scoreVal
                    combo++
                    if (combo > maxCombo) maxCombo = combo
                    zapFlash = 1f
                    zapX = m.x
                    zapY = m.y
                }
                if (toRemove.any { it.type == 2 }) {
                    life = (life + 10f).coerceAtMost(maxLife)
                }
                if (zappedThisLevel >= moleculesToZap) {
                    state = State.LEVEL_COMPLETE
                }
            } else {
                combo = 0
                life -= 2f
                if (life <= 0f) {
                    life = 0f
                    state = State.GAME_OVER
                }
            }
            input.a = false
            input.start = false
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        for (i in 0 until numZones) {
            val x = i * zoneWidth
            val zonePaint = paintOf(String.format("#%06X", (Color.parseColor(zoneColors[i]) and 0xFFFFFF)))
            zonePaint.alpha = if (i == selectedZone) 60 else 20
            canvas.drawRect(x, 0f, x + zoneWidth, h.toFloat(), zonePaint)
            if (i == selectedZone) {
                val borderPaint = paintOf(zoneColors[i])
                borderPaint.style = Paint.Style.STROKE
                borderPaint.strokeWidth = 3f
                canvas.drawRect(x + 2f, 2f, x + zoneWidth - 2f, h - 2f, borderPaint)
            }
        }

        for (m in molecules) {
            val molPaint = paintOf(String.format("#%06X", m.color and 0xFFFFFF))
            canvas.drawCircle(m.x, m.y, 20f, molPaint)
            val shine = paintOf("#FFFFFF")
            shine.alpha = 150
            canvas.drawCircle(m.x - 6f, m.y - 6f, 7f, shine)
            if (m.type == 1) {
                val ring = paintOf("#FF5252")
                ring.style = Paint.Style.STROKE
                ring.strokeWidth = 2f
                canvas.drawCircle(m.x, m.y, 24f, ring)
            } else if (m.type == 2) {
                val star = paintOf("#00E676")
                star.textSize = 16f
                star.textAlign = Paint.Align.CENTER
                canvas.drawText("★", m.x, m.y + 6f, star)
            }
        }

        if (zapFlash > 0f) {
            val alpha = (zapFlash * 255).toInt().coerceIn(0, 255)
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = alpha; isAntiAlias = false }
            canvas.drawCircle(zapX, zapY, 40f * (1f - zapFlash) + 20f, flashPaint)
        }

        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("COMBO: $combo", 10f, 50f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        val lifeBarX = 10f
        val lifeBarY = 65f
        val lifeBarW = w - 20f
        val lifeBarH = 12f
        val lifeBg = paintOf("#2A2A3E")
        canvas.drawRect(lifeBarX, lifeBarY, lifeBarX + lifeBarW, lifeBarY + lifeBarH, lifeBg)
        val lifeFill = paintOf("#00E676")
        canvas.drawRect(lifeBarX, lifeBarY, lifeBarX + lifeBarW * (life / maxLife), lifeBarY + lifeBarH, lifeFill)
        val lifeFrame = paintOf("#F8F8F8")
        lifeFrame.style = Paint.Style.STROKE
        lifeFrame.strokeWidth = 2f
        canvas.drawRect(lifeBarX, lifeBarY, lifeBarX + lifeBarW, lifeBarY + lifeBarH, lifeFrame)

        smallPaint.textSize = 14f
        canvas.drawText("Zapeadas: $zappedThisLevel/$moleculesToZap", w / 2f, 85f, smallPaint)

        smallPaint.textSize = 14f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("◀▶: cambiar zona  ·  A/START: zapear", w / 2f, h - 10f, smallPaint)

        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Zapeadas: $zappedThisLevel", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Combo máximo: $maxCombo", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 80f, greenPaint)
        }

        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Zapeadas: $zapped", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Combo máximo: $maxCombo", w / 2f, h / 2f + 70f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 100f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 140f, greenPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
