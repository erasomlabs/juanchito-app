package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Payasito: reconstrucción fiel de Animal Circus (ZED, 2010).
 *
 * Mecánica original:
 *  · Apilar animales para alcanzar una altura específica.
 *  · 5 mini-juegos, 25 niveles total (5 escenas × 5 niveles).
 *  · Animales con diferentes tamaños y propiedades.
 *  · Física de apilamiento con balance.
 */
class PayasitoGame : Game {
    override val name = "Payasito"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, LEVEL_COMPLETE, GAME_OVER }
    private var state = State.PLAYING

    // Animal types
    private data class AnimalType(val name: String, val width: Float, val height: Float, val color: String, val weight: Float)
    private val animalTypes = listOf(
        AnimalType("ELEFANTE", 80f, 60f, "#8B4513", 5f),
        AnimalType("JIRAFA", 50f, 90f, "#DAA520", 3f),
        AnimalType("LEON", 70f, 50f, "#CD853F", 4f),
        AnimalType("MONO", 40f, 40f, "#8B4513", 2f),
        AnimalType("PINGUINO", 35f, 45f, "#2F4F4F", 2f),
        AnimalType("OSO", 60f, 55f, "#8B4513", 4f),
        AnimalType("COCODRILO", 90f, 30f, "#228B22", 3f),
        AnimalType("SERPIENTE", 100f, 20f, "#32CD32", 1f)
    )

    // Animales apilados
    private data class Animal(val type: AnimalType, var x: Float, var y: Float, var rotation: Float = 0f, var falling: Boolean = false, var vy: Float = 0f)
    private val animals = mutableListOf<Animal>()
    private var currentAnimal: Animal? = null
    private var animalX = 0f
    private var animalY = 60f
    private var animalSpeed = 150f
    private var animalDir = 1
    private var dropping = false

    // Altura objetivo
    private var targetHeight = 0f
    private var currentHeight = 0f

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var combo = 0
    private var animalsPlaced = 0

    // Nivel
    private var level = 1
    private var scene = 1
    private val maxLevels = 5
    private val maxScenes = 5

    // Animaciones
    private var dropFeedback = 0f
    private var dropFeedbackText = ""
    private var dropFeedbackColor = Color.WHITE
    private var shakeTimer = 0f

    // Física
    private val gravity = 600f
    private var windForce = 0f

    // Paints
    private val bgPaint = paintOf("#87CEEB")
    private val groundPaint = paintOf("#8B4513")
    private val grassPaint = paintOf("#228B22")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#FFFFFF").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        reset()
    }

    override fun reset() {
        animals.clear()
        currentAnimal = null
        animalX = w / 2f - 40f
        animalY = 60f
        animalSpeed = 150f
        animalDir = 1
        dropping = false
        scoreVal = 0
        combo = 0
        animalsPlaced = 0
        level = 1
        scene = 1
        targetHeight = h * 0.4f
        currentHeight = 0f
        state = State.PLAYING
        dropFeedback = 0f
        shakeTimer = 0f
        spawnAnimal()
    }

    private fun spawnAnimal() {
        val type = animalTypes.random()
        currentAnimal = Animal(type, animalX, animalY)
        animalX = w / 2f - type.width / 2f
        animalY = 60f
        animalSpeed = (150f + level * 10f).coerceAtMost(250f)
        animalDir = if (Random.nextBoolean()) 1 else -1
        dropping = false
    }

    private fun dropAnimal() {
        if (dropping) return
        dropping = true
        currentAnimal?.falling = true
        currentAnimal?.vy = 0f
    }

    override fun update(dt: Float, input: GameInput) {
        if (dropFeedback > 0f) dropFeedback -= dt
        if (shakeTimer > 0f) shakeTimer -= dt

        // Viento
        windForce = (windForce * 0.99f + (Random.nextFloat() * 20f - 10f)) * 0.5f

        when (state) {
            State.PLAYING -> updatePlaying(dt, input)
            State.LEVEL_COMPLETE -> {
                if (input.start || input.a) {
                    level++
                    if (level > maxLevels) {
                        level = 1
                        scene++
                        if (scene > maxScenes) {
                            state = State.GAME_OVER
                        } else {
                            targetHeight = h * (0.3f + scene * 0.1f)
                            animals.clear()
                            state = State.PLAYING
                            spawnAnimal()
                        }
                    } else {
                        targetHeight = h * (0.3f + level * 0.05f + scene * 0.1f)
                        animals.clear()
                        state = State.PLAYING
                        spawnAnimal()
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
            dropAnimal()
            input.a = false; input.start = false
        }

        val animal = currentAnimal ?: return

        if (!dropping) {
            animalX += animalSpeed * animalDir * dt
            if (animalX < 20f) { animalX = 20f; animalDir = 1 }
            if (animalX > w - animal.type.width - 20f) { animalX = w - animal.type.width - 20f; animalDir = -1 }
            animalX += windForce * dt * 0.3f
            animal.x = animalX
        } else {
            animal.vy += gravity * dt
            animal.y += animal.vy * dt

            // Colisión con el suelo o el animal anterior
            val targetY = if (animals.isEmpty()) h - 80f - animal.type.height else animals.last().y - animal.type.height
            if (animal.y >= targetY) {
                animal.y = targetY
                // Evaluar estabilidad
                val towerX = if (animals.isEmpty()) w / 2f - animal.type.width / 2f else animals.last().x
                val offset = Math.abs(animal.x - towerX)
                val stable = offset < animal.type.width * 0.5f

                if (stable) {
                    animals.add(Animal(animal.type, animal.x, animal.y))
                    animalsPlaced++
                    combo++
                    val pts = 50 + combo * 25
                    scoreVal += pts
                    if (scoreVal > best) best = scoreVal
                    currentHeight = if (animals.isEmpty()) 0f else animals.last().y
                    dropFeedbackText = if (combo > 2) "¡COMBO x$combo! +$pts" else "¡BIEN! +$pts"
                    dropFeedbackColor = Color.parseColor("#00E676")
                    dropFeedback = 1f

                    // Verificar si alcanzó la altura objetivo
                    if (animals.isNotEmpty() && animals.last().y <= targetHeight) {
                        state = State.LEVEL_COMPLETE
                    } else {
                        spawnAnimal()
                    }
                } else {
                    // Inestable - se cae
                    combo = 0
                    dropFeedbackText = "¡SE CAYÓ!"
                    dropFeedbackColor = Color.parseColor("#FF5252")
                    dropFeedback = 1f
                    shakeTimer = 0.3f
                    spawnAnimal()
                }
                dropping = false
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#87CEEB"))

        // Suelo
        canvas.drawRect(0f, h - 60f, w.toFloat(), h.toFloat(), groundPaint)
        canvas.drawRect(0f, h - 70f, w.toFloat(), h - 60f, grassPaint)

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level  ·  ESCENA: $scene", w / 2f - 80f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        val lastY = animals.lastOrNull()?.y ?: h.toFloat()
        val heightPx = (h - lastY).toInt()
        val targetPx = (h - targetHeight).toInt()
        canvas.drawText("ALTURA: ${heightPx}px / ${targetPx}px", w / 2f - 100f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Animales apilados
        for (animal in animals) {
            drawAnimal(canvas, animal)
        }

        // Animal actual
        currentAnimal?.let {
            val paint = paintOf(it.type.color)
            if (dropping) paint.alpha = 200
            canvas.drawRoundRect(RectF(animalX, animalY, animalX + it.type.width, animalY + it.type.height), 6f, 6f, paint)
            // Ojos
            val eyePaint = paintOf("#FFFFFF")
            canvas.drawCircle(animalX + it.type.width * 0.3f, animalY + it.type.height * 0.3f, 4f, eyePaint)
            canvas.drawCircle(animalX + it.type.width * 0.7f, animalY + it.type.height * 0.3f, 4f, eyePaint)
            val pupil = paintOf("#000000")
            canvas.drawCircle(animalX + it.type.width * 0.3f, animalY + it.type.height * 0.3f, 2f, pupil)
            canvas.drawCircle(animalX + it.type.width * 0.7f, animalY + it.type.height * 0.3f, 2f, pupil)
            // Nombre del animal
            smallPaint.textSize = 10f
            canvas.drawText(it.type.name, animalX + it.type.width / 2f, animalY - 5f, smallPaint)
        }

        // Línea de altura objetivo
        val targetY = targetHeight
        val targetPaint = paintOf("#FF5252")
        targetPaint.style = Paint.Style.STROKE
        targetPaint.strokeWidth = 2f
        targetPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
        canvas.drawLine(0f, targetY, w.toFloat(), targetY, targetPaint)
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#FF5252")
        canvas.drawText("META", w - 30f, targetY - 5f, smallPaint)

        // Feedback
        if (dropFeedback > 0f) {
            val alpha = (dropFeedback * 255).toInt().coerceIn(0, 255)
            val fbPaint = Paint().apply { color = dropFeedbackColor; this.alpha = alpha; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 24f; isFakeBoldText = true }
            canvas.drawText(dropFeedbackText, w / 2f, h / 2f, fbPaint)
        }

        // Combo
        if (combo > 1) {
            smallPaint.textSize = 18f
            smallPaint.color = Color.parseColor("#F5C542")
            canvas.drawText("COMBO x$combo", w / 2f, 70f, smallPaint)
        }

        // Viento
        val windArrow = if (windForce > 0) "→" else "←"
        smallPaint.textSize = 14f
        smallPaint.color = Color.parseColor("#8FD0F8")
        canvas.drawText("Viento: $windArrow", 20f, h - 20f, smallPaint)

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Escena $scene · Nivel $level", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Animales: $animalsPlaced", w / 2f, h / 2f + 40f, smallPaint)
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
            canvas.drawText("Animales colocados: $animalsPlaced", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Combo máximo: $combo", w / 2f, h / 2f + 70f, smallPaint)
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
            smallPaint.color = Color.parseColor("#FFFFFF")
            canvas.drawText("A/START: soltar animal  ·  Viento afecta la caída", w / 2f, h - 10f, smallPaint)
        }
    }

    private fun drawAnimal(canvas: Canvas, animal: Animal) {
        val paint = paintOf(animal.type.color)
        canvas.drawRoundRect(RectF(animal.x, animal.y, animal.x + animal.type.width, animal.y + animal.type.height), 6f, 6f, paint)
        // Ojos
        val eyePaint = paintOf("#FFFFFF")
        canvas.drawCircle(animal.x + animal.type.width * 0.3f, animal.y + animal.type.height * 0.3f, 4f, eyePaint)
        canvas.drawCircle(animal.x + animal.type.width * 0.7f, animal.y + animal.type.height * 0.3f, 4f, eyePaint)
        val pupil = paintOf("#000000")
        canvas.drawCircle(animal.x + animal.type.width * 0.3f, animal.y + animal.type.height * 0.3f, 2f, pupil)
        canvas.drawCircle(animal.x + animal.type.width * 0.7f, animal.y + animal.type.height * 0.3f, 2f, pupil)
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
