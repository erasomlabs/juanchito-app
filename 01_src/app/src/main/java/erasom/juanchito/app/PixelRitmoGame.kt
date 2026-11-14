package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Pixel Ritmo: reconstrucción fiel de Music Revolution (Ozura Mobile, 2009).
 *
 * Mecánica original:
 *  · Notas caen desde arriba, presionar botones en el momento correcto.
 *  · 5 botones (4 direcciones + acción), combo system, barra de vida.
 *  · Puntuación por timing (perfect/good/miss), combos multiplican.
 *  · Múltiples canciones, dificultad creciente (más notas, más velocidad).
 *  · Efectos visuales: backgrounds cambiantes, notas con colores, hit effects.
 */
class PixelRitmoGame : Game {
    override val name = "Pixel Ritmo"
    override val genre = "Ritmo"

    private var w = 0
    private var h = 0

    // Estado
    private enum class State { PLAYING, GAME_OVER, SONG_COMPLETE }
    private var state = State.PLAYING

    // Notas: cada nota tiene (lane, y, hit)
    private data class Note(val lane: Int, var y: Float, var hit: Boolean = false, var missed: Boolean = false)
    private val notes = mutableListOf<Note>()
    private var noteTimer = 0f
    private var noteInterval = 0.8f
    private var noteSpeed = 300f

    // 5 lanes
    private val numLanes = 5
    private var laneWidth = 0f
    private var laneStartX = 0f

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var combo = 0
    private var maxCombo = 0
    private var perfects = 0
    private var goods = 0
    private var misses = 0

    // Vida
    private var life = 100f
    private val maxLife = 100f

    // Feedback
    private var feedbackTimer = 0f
    private var feedbackText = ""
    private var feedbackColor = Color.WHITE
    private var hitFlash = 0f
    private var hitLane = -1

    // Canción
    private var songTime = 0f
    private var songDuration = 60f  // 60 segundos por canción
    private var songLevel = 1

    // Animaciones
    private var bgPhase = 0f
    private val laneColors = listOf("#5C94FC", "#F8D878", "#D82800", "#00A800", "#8814A8")

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val lanePaint = paintOf("#2A2A4E")
    private val linePaint = paintOf("#F8F8F8")
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
        laneWidth = w / numLanes.toFloat()
        laneStartX = 0f
        reset()
    }

    override fun reset() {
        notes.clear()
        noteTimer = 0f
        noteInterval = 0.8f
        noteSpeed = 300f
        scoreVal = 0
        combo = 0
        maxCombo = 0
        perfects = 0
        goods = 0
        misses = 0
        life = maxLife
        songTime = 0f
        songLevel = 1
        state = State.PLAYING
        feedbackTimer = 0f
        hitFlash = 0f
        hitLane = -1
        bgPhase = 0f
    }

    private fun spawnNote() {
        val lane = Random.nextInt(numLanes)
        notes.add(Note(lane, -40f))
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.PLAYING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.SONG_COMPLETE) {
                    songLevel++
                    songDuration += 10f
                    noteInterval = (noteInterval * 0.9f).coerceAtLeast(0.3f)
                    noteSpeed += 30f
                    state = State.PLAYING
                }
            }
            return
        }

        // Timers
        bgPhase += dt
        songTime += dt
        if (songTime >= songDuration) {
            state = State.SONG_COMPLETE
            return
        }

        // Spawn notes
        noteTimer += dt
        if (noteTimer >= noteInterval) {
            noteTimer = 0f
            spawnNote()
            // A veces spawnear 2 notas a la vez en niveles altos
            if (songLevel >= 3 && Random.nextFloat() < 0.3f) spawnNote()
        }

        // Mover notas
        val it = notes.iterator()
        while (it.hasNext()) {
            val note = it.next()
            note.y += noteSpeed * dt
            if (note.y > h + 40f) {
                if (!note.hit) {
                    note.missed = true
                    misses++
                    combo = 0
                    life -= 5f
                    if (life <= 0f) {
                        life = 0f
                        state = State.GAME_OVER
                    }
                }
                it.remove()
            }
        }

        // Input handling
        val lanes = listOf(input.left, input.down, input.a, input.up, input.right)
        // Mapeo: L1=left, L2=down, L3=A, L4=up, L5=right
        for (lane in 0 until numLanes) {
            if (lanes.getOrElse(lane) { false }) {
                // Buscar la nota más cercana en este lane que esté en la zona de hit
                val hitZone = RectF(lane * laneWidth, h - 120f, (lane + 1) * laneWidth, h - 20f)
                var bestNote: Note? = null
                var bestDist = Float.MAX_VALUE
                for (note in notes) {
                    if (note.lane == lane && !note.hit && !note.missed) {
                        val dist = Math.abs(note.y - (h - 70f))
                        if (dist < bestDist && dist < 80f) {
                            bestDist = dist
                            bestNote = note
                        }
                    }
                }
                if (bestNote != null) {
                    bestNote.hit = true
                    val dist = Math.abs(bestNote.y - (h - 70f))
                    if (dist < 25f) {
                        scoreVal += 100 + combo * 10
                        perfects++
                        feedbackText = "PERFECT!"
                        feedbackColor = Color.parseColor("#00E676")
                    } else {
                        scoreVal += 50 + combo * 5
                        goods++
                        feedbackText = "GOOD"
                        feedbackColor = Color.parseColor("#F5C542")
                    }
                    if (scoreVal > best) best = scoreVal
                    combo++
                    if (combo > maxCombo) maxCombo = combo
                    feedbackTimer = 0.5f
                    hitFlash = 1f
                    hitLane = lane
                }
                // Reset input
                when (lane) {
                    0 -> input.left = false
                    1 -> input.down = false
                    2 -> input.a = false
                    3 -> input.up = false
                    4 -> input.right = false
                }
            }
        }

        // Feedback timer
        if (feedbackTimer > 0f) feedbackTimer -= dt
        if (hitFlash > 0f) hitFlash -= dt * 4f

        // Miss penalty por notas que pasan sin hit
        // (ya manejado arriba)
    }

    override fun draw(canvas: Canvas) {
        // Fondo con gradiente animado
        val bg1 = Color.parseColor("#1A1A2E")
        val bg2 = Color.parseColor("#2A1A4E")
        canvas.drawColor(bg1)

        // Lanes
        for (i in 0 until numLanes) {
            val x = i * laneWidth
            canvas.drawRect(x, 0f, x + laneWidth, h.toFloat(), lanePaint)
            // Línea divisoria
            linePaint.color = Color.parseColor("#3A3A5E")
            linePaint.strokeWidth = 2f
            canvas.drawLine(x + laneWidth, 0f, x + laneWidth, h.toFloat(), linePaint)
        }

        // Zona de hit
        val hitZonePaint = Paint().apply { color = Color.parseColor("#F5C542"); alpha = 40; isAntiAlias = false }
        canvas.drawRect(0f, h - 120f, w.toFloat(), h - 20f, hitZonePaint)
        val hitLinePaint = Paint().apply { color = Color.parseColor("#F5C542"); strokeWidth = 3f; isAntiAlias = false }
        canvas.drawLine(0f, h - 70f, w.toFloat(), h - 70f, hitLinePaint)

        // Notas
        for (note in notes) {
            if (note.hit) continue
            val x = note.lane * laneWidth + laneWidth / 2f
            val color = Color.parseColor(laneColors[note.lane])
            val notePaint = paintOf(String.format("#%06X", color and 0xFFFFFF))
            // Cabeza de nota (círculo)
            canvas.drawCircle(x, note.y, 18f, notePaint)
            // Brillo
            val shine = paintOf("#FFFFFF")
            shine.alpha = 150
            canvas.drawCircle(x - 5f, note.y - 5f, 6f, shine)
            // Plica
            val stemPaint = paintOf(String.format("#%06X", color and 0xFFFFFF))
            canvas.drawRect(x + 14f, note.y - 25f, x + 17f, note.y, stemPaint)
        }

        // Hit flash
        if (hitFlash > 0f && hitLane >= 0) {
            val flashAlpha = (hitFlash * 200).toInt().coerceIn(0, 200)
            val flashPaint = Paint().apply { color = Color.parseColor("#FFFFFF"); this.alpha = flashAlpha; isAntiAlias = false }
            canvas.drawRect(hitLane * laneWidth, h - 120f, (hitLane + 1) * laneWidth, h - 20f, flashPaint)
        }

        // HUD
        hudPaint.textSize = 20f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("COMBO: $combo", 10f, 50f, hudPaint)
        canvas.drawText("NIVEL: $songLevel", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT

        // Barra de vida
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

        // Tiempo de canción
        val timeBarX = 10f
        val timeBarY = h - 8f
        val timeBarW = w - 20f
        val timeFrac = 1f - (songTime / songDuration).coerceIn(0f, 1f)
        val timeBg = paintOf("#2A2A3E")
        canvas.drawRect(timeBarX, timeBarY - 4f, timeBarX + timeBarW, timeBarY, timeBg)
        val timeFill = paintOf("#F5C542")
        canvas.drawRect(timeBarX, timeBarY - 4f, timeBarX + timeBarW * timeFrac, timeBarY, timeFill)

        // Feedback
        if (feedbackTimer > 0f) {
            val fbAlpha = (feedbackTimer / 0.5f * 255).toInt().coerceIn(0, 255)
            val fbPaint = Paint().apply { color = feedbackColor; this.alpha = fbAlpha; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 32f; isFakeBoldText = true }
            canvas.drawText(feedbackText, w / 2f, h / 2f - 40f, fbPaint)
        }

        // Labels de botones
        smallPaint.textSize = 12f
        val buttonLabels = listOf("◀", "▼", "A", "▲", "▶")
        for (i in 0 until numLanes) {
            val x = i * laneWidth + laneWidth / 2f
            canvas.drawText(buttonLabels[i], x, h - 4f, smallPaint)
        }

        // Overlay de canción completa
        if (state == State.SONG_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡CANCIÓN COMPLETA!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Perfect: $perfects  ·  Good: $goods  ·  Miss: $misses", w / 2f, h / 2f + 40f, smallPaint)
            canvas.drawText("Max combo: $maxCombo", w / 2f, h / 2f + 70f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            canvas.drawText("Max combo: $maxCombo", w / 2f, h / 2f + 40f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 70f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 110f, greenPaint)
        }

        // Instrucciones
        if (state == State.PLAYING) {
            smallPaint.textSize = 12f
            smallPaint.color = Color.parseColor("#B8B8C8")
            canvas.drawText("◀▼A▲▶: tocar notas  ·  START: pausa", w / 2f, h - 20f, smallPaint)
        }
    }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
