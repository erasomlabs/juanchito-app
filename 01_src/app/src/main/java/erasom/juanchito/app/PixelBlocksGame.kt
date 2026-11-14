package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Blocks: reconstrucción fiel de Tower Bloxx (Digital Chocolate, 2009).
 *
 * Mecánica original:
 *  · Dejar caer pisos (bloques) para construir una torre.
 *  · El viento empuja los bloques lateralmente.
 *  · Cuanto más rápido y preciso, más puntos.
 *  · Los bloques mal colocados se caen.
 *  · Combos por pisos consecutivos perfectos.
 *  · Múltiples edificios, ciudad completa al terminar.
 */
class PixelBlocksGame : Game {
    override val name = "Pixel Blocks"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Estado del juego
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Torre
    private val blocks = mutableListOf<Block>()
    private var currentBlock: Block? = null
    private var blockX = 0f
    private var blockY = 0f
    private var blockSpeed = 0f
    private var blockDir = 1  // 1 = derecha, -1 = izquierda
    private var falling = false
    private var fallSpeed = 0f

    // Viento
    private var windForce = 0f
    private var windTimer = 0f
    private var windDir = 1

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var combo = 0
    private var maxCombo = 0
    private var perfectDrops = 0
    private var totalDrops = 0

    // Nivel
    private var level = 1
    private var targetHeight = 10
    private var buildingsCompleted = 0
    private val maxBuildings = 5

    // Animaciones
    private var shakeTimer = 0f
    private var shakeIntensity = 0f
    private var dropFeedback = 0f
    private var dropFeedbackText = ""
    private var dropFeedbackColor = Color.WHITE

    // Dimensiones
    private var blockSize = 60f
    private var towerCenter = 0f
    private var groundY = 0f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val groundPaint = paintOf("#3A5A2A")
    private val blockColors = listOf("#5C94FC", "#F8D878", "#D82800", "#00A800", "#8814A8", "#FC9838")
    private val windPaint = paintOf("#8FD0F8")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    data class Block(val x: Float, val y: Float, val width: Float, val color: Int, val perfect: Boolean)

    override fun init(width: Int, height: Int) {
        w = width; h = height
        blockSize = (w / 8f).coerceAtMost(80f)
        towerCenter = w / 2f
        groundY = h - 60f
        reset()
    }

    override fun reset() {
        blocks.clear()
        currentBlock = null
        blockX = towerCenter - blockSize / 2f
        blockY = 60f
        blockSpeed = 200f
        blockDir = 1
        falling = false
        fallSpeed = 0f
        windForce = 0f
        windTimer = 0f
        windDir = 1
        scoreVal = 0
        combo = 0
        maxCombo = 0
        perfectDrops = 0
        totalDrops = 0
        level = 1
        targetHeight = 10
        buildingsCompleted = 0
        state = State.PLAYING
        shakeTimer = 0f
        dropFeedback = 0f
        spawnBlock()
    }

    private fun spawnBlock() {
        val color = Color.parseColor(blockColors[blocks.size % blockColors.size])
        currentBlock = Block(blockX, blockY, blockSize, color, false)
        blockX = towerCenter - blockSize / 2f
        blockY = 60f
        blockSpeed = (200f + level * 30f).coerceAtMost(400f)
        blockDir = if (Random.nextBoolean()) 1 else -1
        falling = false
        fallSpeed = 0f
    }

    private fun dropBlock() {
        if (falling) return
        falling = true
        fallSpeed = 50f
        totalDrops++
    }

    override fun update(dt: Float, input: GameInput) {
        // Animaciones
        if (shakeTimer > 0f) shakeTimer -= dt
        if (dropFeedback > 0f) dropFeedback -= dt

        // Viento
        windTimer += dt
        if (windTimer > 3f) {
            windTimer = 0f
            windDir = if (Random.nextBoolean()) 1 else -1
            windForce = (Random.nextFloat() * 60f + 20f) * windDir
        }
        windForce = windForce * 0.98f + (Random.nextFloat() * 10f - 5f) * windDir

        when (state) {
            State.PLAYING -> updatePlaying(dt, input)
            State.LEVEL_COMPLETE -> {
                if (input.start || input.a) {
                    buildingsCompleted++
                    if (buildingsCompleted >= maxBuildings) {
                        state = State.GAME_OVER
                    } else {
                        level++
                        targetHeight += 3
                        blocks.clear()
                        state = State.PLAYING
                        spawnBlock()
                    }
                }
            }
            State.GAME_OVER -> {
                if (input.start || input.a) reset()
            }
        }
    }

    private fun updatePlaying(dt: Float, input: GameInput) {
        if (input.a || input.start) {
            dropBlock()
            input.a = false; input.start = false
        }

        val block = currentBlock ?: return

        if (!falling) {
            // Bloque se mueve horizontalmente
            blockX += blockSpeed * blockDir * dt
            if (blockX < 20f) { blockX = 20f; blockDir = 1 }
            if (blockX > w - blockSize - 20f) { blockX = w - blockSize - 20f; blockDir = -1 }
            // Viento afecta la velocidad
            blockX += windForce * dt * 0.5f
        } else {
            // Bloque cae
            fallSpeed += 800f * dt
            blockY += fallSpeed * dt

            // Colisión con el suelo o el bloque anterior
            val targetY = if (blocks.isEmpty()) groundY - blockSize else blocks.last().y - blockSize
            if (blockY >= targetY) {
                blockY = targetY
                // Evaluar precisión
                val towerX = if (blocks.isEmpty()) towerCenter - blockSize / 2f else blocks.last().x
                val offset = Math.abs(blockX - towerX)
                val perfect = offset < blockSize * 0.15f
                val good = offset < blockSize * 0.4f

                if (good) {
                    val color = Color.parseColor(blockColors[blocks.size % blockColors.size])
                    blocks.add(Block(blockX, blockY, blockSize, color, perfect))
                    if (perfect) {
                        combo++
                        if (combo > maxCombo) maxCombo = combo
                        perfectDrops++
                        val pts = 100 + combo * 50
                        scoreVal += pts
                        if (scoreVal > best) best = scoreVal
                        dropFeedbackText = "¡PERFECTO! +$pts"
                        dropFeedbackColor = Color.parseColor("#00E676")
                        dropFeedback = 1f
                    } else {
                        combo = 0
                        scoreVal += 50
                        if (scoreVal > best) best = scoreVal
                        dropFeedbackText = "BIEN +50"
                        dropFeedbackColor = Color.parseColor("#F5C542")
                        dropFeedback = 0.8f
                    }
                    // Verificar si se completó el nivel
                    if (blocks.size >= targetHeight) {
                        state = State.LEVEL_COMPLETE
                    } else {
                        spawnBlock()
                    }
                } else {
                    // Bloque cae mal - se cae
                    combo = 0
                    dropFeedbackText = "¡FALLASTE!"
                    dropFeedbackColor = Color.parseColor("#FF5252")
                    dropFeedback = 1f
                    shakeTimer = 0.3f
                    shakeIntensity = 8f
                    spawnBlock()
                }
                falling = false
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        // Shake effect
        val shakeX = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f
        val shakeY = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f
        canvas.save()
        canvas.translate(shakeX, shakeY)

        // Fondo
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)

        // Viento (indicador)
        val windArrow = if (windForce > 0) "→" else "←"
        windPaint.textSize = 20f
        canvas.drawText("Viento: $windArrow ${Math.abs(windForce).toInt()}", 20f, 30f, windPaint)

        // HUD
        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 20f, 55f, hudPaint)
        canvas.drawText("BEST: $best", w - 20f - hudPaint.measureText("BEST: $best"), 55f, hudPaint)
        canvas.drawText("NIVEL: $level  ·  PISOS: ${blocks.size}/$targetHeight", w / 2f - 120f, 80f, smallPaint.apply { textAlign = Paint.Align.LEFT; textSize = 16f })

        // Bloques de la torre
        for (block in blocks) {
            val paint = paintOf(String.format("#%06X", block.color and 0xFFFFFF))
            canvas.drawRoundRect(RectF(block.x, block.y, block.x + block.width, block.y + block.width), 6f, 6f, paint)
            // Brillo superior
            val shine = paintOf("#FFFFFF")
            shine.alpha = 80
            canvas.drawRect(block.x + 4f, block.y + 2f, block.x + block.width - 4f, block.y + 6f, shine)
            if (block.perfect) {
                // Estrella de perfecto
                val starPaint = paintOf("#F5C542")
                canvas.drawText("★", block.x + block.width / 2f, block.y + block.width / 2f + 6f, starPaint)
            }
        }

        // Bloque actual
        currentBlock?.let {
            val paint = paintOf(String.format("#%06X", it.color and 0xFFFFFF))
            if (falling) paint.alpha = 200
            canvas.drawRoundRect(RectF(blockX, blockY, blockX + blockSize, blockY + blockSize), 6f, 6f, paint)
            val shine = paintOf("#FFFFFF")
            shine.alpha = 80
            canvas.drawRect(blockX + 4f, blockY + 2f, blockX + blockSize - 4f, blockY + 6f, shine)
        }

        // Feedback de drop
        if (dropFeedback > 0f) {
            val fbAlpha = (dropFeedback * 255).toInt().coerceIn(0, 255)
            val fbPaint = Paint().apply { color = dropFeedbackColor; this.alpha = fbAlpha; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 28f; isFakeBoldText = true }
            canvas.drawText(dropFeedbackText, w / 2f, h / 2f, fbPaint)
        }

        // Combo
        if (combo > 1) {
            smallPaint.textSize = 18f
            smallPaint.color = Color.parseColor("#F5C542")
            canvas.drawText("COMBO x$combo", w / 2f, 100f, smallPaint)
        }

        canvas.restore()

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Edificios: $buildingsCompleted/$maxBuildings", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Pisos perfectos: $perfectDrops", w / 2f, h / 2f + 40f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA CONTINUAR", w / 2f, h / 2f + 80f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Mejor combo: $maxCombo", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Pisos perfectos: $perfectDrops", w / 2f, h / 2f + 70f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 100f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 140f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 14f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("A/START: soltar bloque  ·  Viento afecta la caída", w / 2f, h - 10f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
