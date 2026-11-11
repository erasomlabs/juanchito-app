package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

// 9. Pixel Puzzles: Alinear bloques en secuencia
class PixelPuzzlesGame : Game {
    override val name = "Pixel Puzzles"
    override val genre = "Puzzle"
    private var w = 0; private var h = 0
    private var pos = 0
    private val pattern = listOf(0, 1, 2, 0, 2, 1)
    private var step = 0
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() { isGameOver = false; score = 0; pos = 0; step = 0 }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right && !input.a) { moved = false; return }
        if (moved) return

        if (input.left && pos > 0) { pos--; moved = true }
        if (input.right && pos < 2) { pos++; moved = true }
        if (input.a) {
            moved = true
            if (pos == pattern[step % pattern.size]) {
                score += 50
                step++
            } else {
                isGameOver = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Taller de ladrillos con marco verde
        NesScenery.drawBrickWall(canvas, w, h, 150)
        NesScenery.drawNeonFrame(canvas, w, h, "#00A800")
        paint.textSize = 24f
        paint.color = Color.WHITE
        canvas.drawText("Objetivo: Bloque ${pattern[step % pattern.size] + 1}", w / 2f, h * 0.25f, paint)

        val size = 60f
        val startX = (w - size * 3) / 2f
        for (i in 0..2) {
            paint.color = if (i == pos) Color.parseColor("#F8D878") else Color.parseColor("#5C94FC")
            val r = RectF(startX + i * size + 6, h * 0.5f, startX + (i + 1) * size - 6, h * 0.5f + size)
            canvas.drawRoundRect(r, 6f, 6f, paint)
        }
    }
}

// 10. Carreras: Esquivar coches en carretera de 3 carriles
class CarrerasGame : Game {
    override val name = "Carreras"
    override val genre = "Carreras"
    private var w = 0; private var h = 0
    private var lane = 1 // 0, 1, 2
    private val traffic = mutableListOf<Pair<Int, Float>>()
    override var isGameOver = false
    override var score = 0
    private var timer = 0f
    private val paint = Paint().apply { isAntiAlias = true }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        traffic.clear(); isGameOver = false; score = 0; lane = 1; timer = 0f
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right) moved = false
        else if (!moved) {
            if (input.left && lane > 0) { lane--; moved = true }
            if (input.right && lane < 2) { lane++; moved = true }
        }

        timer += dt
        if (timer > 1.1f) {
            traffic.add(Pair(Random.nextInt(3), -40f))
            timer = 0f
        }

        val it = traffic.listIterator()
        while (it.hasNext()) {
            val (tLane, y) = it.next()
            val newY = y + 360f * dt
            if (newY > h) {
                it.remove()
                score += 20
            } else {
                it.set(tLane to newY)
                if (tLane == lane && newY in (h - 90f)..(h - 20f)) {
                    isGameOver = true
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Carretera entre colinas con cielo azul
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#5C94FC", "#A8D8FC", 5)
        NesScenery.drawSun(canvas, w - 60f, 60f, 26f)
        NesScenery.drawClouds(canvas, w, h * 0.22f)
        NesScenery.drawHills(canvas, w, h * 0.6f, "#00A800", "#008800")
        canvas.drawRect(0f, h * 0.6f, w.toFloat(), h.toFloat(), Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false })
        val laneWidth = 70f
        val roadX = (w - laneWidth * 3) / 2f
        paint.color = Color.parseColor("#2D3748")
        canvas.drawRect(roadX, 0f, roadX + laneWidth * 3, h.toFloat(), paint)

        // Jugador
        paint.color = Color.parseColor("#5C94FC")
        val px = roadX + lane * laneWidth + 15f
        canvas.drawRoundRect(RectF(px, h - 80f, px + laneWidth - 30f, h - 30f), 6f, 6f, paint)

        // Tráfico
        paint.color = Color.parseColor("#D82800")
        for ((tLane, y) in traffic) {
            val tx = roadX + tLane * laneWidth + 15f
            canvas.drawRoundRect(RectF(tx, y, tx + laneWidth - 30f, y + 50f), 6f, 6f, paint)
        }
    }
}

// 11. Solitario: Memoria / emparejar cartas
class SolitarioGame : Game {
    override val name = "Solitario"
    override val genre = "Cartas"
    private var w = 0; private var h = 0
    private var cards = mutableListOf<Int>()
    private var revealed = Array(6) { false }
    private var firstPick = -1
    private var sel = 0
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        cards = mutableListOf(1, 1, 2, 2, 3, 3).apply { shuffle() }
        revealed = Array(6) { false }
        firstPick = -1; sel = 0; isGameOver = false; score = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right && !input.a) { moved = false; return }
        if (moved) return

        if (input.left && sel > 0) { sel--; moved = true }
        if (input.right && sel < 5) { sel++; moved = true }

        if (input.a && !revealed[sel]) {
            moved = true
            revealed[sel] = true
            if (firstPick == -1) {
                firstPick = sel
            } else {
                if (cards[firstPick] == cards[sel]) {
                    score += 100
                    firstPick = -1
                    if (revealed.all { it }) isGameOver = true
                } else {
                    revealed[firstPick] = false
                    revealed[sel] = false
                    firstPick = -1
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Mesa de casino: fieltro verde + marco dorado
        NesScenery.drawCheckerFloor(canvas, w, h, 0f, "#006400", "#005000")
        NesScenery.drawNeonFrame(canvas, w, h, "#F5C542")
        val cardW = 50f; val cardH = 75f
        val startX = (w - cardW * 6) / 2f
        paint.textSize = 24f
        for (i in 0..5) {
            val r = RectF(startX + i * cardW + 4, h / 2f - cardH / 2f, startX + (i + 1) * cardW - 4, h / 2f + cardH / 2f)
            paint.color = if (i == sel) Color.parseColor("#F8D878") else Color.parseColor("#F8F8F8")
            canvas.drawRoundRect(r, 6f, 6f, paint)
            if (revealed[i]) {
                paint.color = Color.BLACK
                canvas.drawText("${cards[i]}", r.centerX(), r.centerY() + 8, paint)
            }
        }
    }
}

// 12. Pixel Dados: Apuesta y acierto mayor o menor
class PixelDadosGame : Game {
    override val name = "Pixel Dados"
    override val genre = "Azar"
    private var w = 0; private var h = 0
    private var currentDice = 3
    private var nextDice = 4
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        currentDice = Random.nextInt(1, 7)
        isGameOver = false; score = 0
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.up && !input.down) { moved = false; return }
        if (moved) return

        nextDice = Random.nextInt(1, 7)
        if (input.up) { // Apuesta a Mayor
            moved = true
            if (nextDice >= currentDice) { score += 50; currentDice = nextDice } else isGameOver = true
        }
        if (input.down) { // Apuesta a Menor
            moved = true
            if (nextDice <= currentDice) { score += 50; currentDice = nextDice } else isGameOver = true
        }
    }

    override fun draw(canvas: Canvas) {
        // Casino nocturno con reflectores
        NesScenery.drawCheckerFloor(canvas, w, h, 0f, "#006400", "#005000")
        NesScenery.drawSpotlights(canvas, w, h)
        paint.color = Color.parseColor("#5C94FC")
        val size = 90f
        val r = RectF(w / 2f - size / 2f, h / 2f - size / 2f, w / 2f + size / 2f, h / 2f + size / 2f)
        canvas.drawRoundRect(r, 12f, 12f, paint)

        paint.color = Color.BLACK
        paint.textSize = 42f
        canvas.drawText("$currentDice", r.centerX(), r.centerY() + 14, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        canvas.drawText("ARRIBA = Mayor | ABAJO = Menor", w / 2f, h * 0.82f, paint)
    }
}
