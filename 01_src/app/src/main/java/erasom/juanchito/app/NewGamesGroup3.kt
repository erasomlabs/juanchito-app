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

        // Líneas divisorias de carril punteadas
        paint.color = Color.parseColor("#F8F8F8")
        for (i in 1..2) {
            var ly = 0f
            while (ly < h) {
                canvas.drawRect(roadX + i * laneWidth - 2f, ly, roadX + i * laneWidth + 2f, ly + 26f, paint)
                ly += 52f
            }
        }
        // Bordes de la carretera (guardarraíl)
        val rail = Paint().apply { color = Color.parseColor("#B8B8B8"); isAntiAlias = false }
        canvas.drawRect(roadX - 6f, 0f, roadX - 2f, h.toFloat(), rail)
        canvas.drawRect(roadX + laneWidth * 3 + 2f, 0f, roadX + laneWidth * 3 + 6f, h.toFloat(), rail)

        // Jugador: coche de Juanchito con parabrisas, faros y ruedas (vista trasera)
        val px = roadX + lane * laneWidth + 15f
        drawCar(canvas, px, h - 80f, laneWidth - 30f, 50f, "#5C94FC", facingUp = true)
        // Juanchito piloto en el asiento
        JuanchitoSprite.draw(canvas, px + (laneWidth - 30f) / 2f, h - 58f, 16f, facingRight = true)

        // Tráfico: coches rivales con detalles (vista frontal, vienen hacia abajo)
        val rivalColors = listOf("#D82800", "#00A800", "#FC9838", "#8814A8", "#F8D878")
        for ((idx, t) in traffic.withIndex()) {
            val (tLane, y) = t
            val tx = roadX + tLane * laneWidth + 15f
            drawCar(canvas, tx, y, laneWidth - 30f, 50f, rivalColors[idx % rivalColors.size], facingUp = false)
        }
    }

    /** Coche top-down: carrocería, techo, parabrisas, faros y ruedas. */
    private fun drawCar(canvas: Canvas, x: Float, y: Float, cw: Float, ch: Float, colorHex: String, facingUp: Boolean) {
        val body = Paint().apply { color = Color.parseColor(colorHex); isAntiAlias = false }
        val bodyDark = Paint().apply { color = Color.parseColor("#000000"); alpha = 70; isAntiAlias = false }
        val roof = Paint().apply { color = Color.parseColor("#1A1A1A"); isAntiAlias = false }
        val glass = Paint().apply { color = Color.parseColor("#A8D8FC"); isAntiAlias = false }
        val tire = Paint().apply { color = Color.parseColor("#1A1A1A"); isAntiAlias = false }
        // ruedas
        canvas.drawRect(x - 2f, y + 6f, x + 4f, y + 16f, tire)
        canvas.drawRect(x + cw - 4f, y + 6f, x + cw + 2f, y + 16f, tire)
        canvas.drawRect(x - 2f, y + ch - 16f, x + 4f, y + ch - 6f, tire)
        canvas.drawRect(x + cw - 4f, y + ch - 16f, x + cw + 2f, y + ch - 6f, tire)
        // carrocería
        canvas.drawRoundRect(RectF(x, y, x + cw, y + ch), 5f, 5f, body)
        // techo / cabina
        canvas.drawRect(x + 4f, y + ch * 0.32f, x + cw - 4f, y + ch * 0.68f, roof)
        // parabrisas y luneta
        canvas.drawRect(x + 5f, y + ch * 0.22f, x + cw - 5f, y + ch * 0.3f, glass)
        canvas.drawRect(x + 5f, y + ch * 0.7f, x + cw - 5f, y + ch * 0.78f, glass)
        if (facingUp) {
            // faros delanteros (arriba)
            val lamp = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
            canvas.drawRect(x + 3f, y + 2f, x + 9f, y + 6f, lamp)
            canvas.drawRect(x + cw - 9f, y + 2f, x + cw - 3f, y + 6f, lamp)
        } else {
            // faros iluminados de los rivales (abajo) + sombra frontal
            val lamp = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
            val beam = Paint().apply { color = Color.parseColor("#FCFCB8"); alpha = 120; isAntiAlias = false }
            canvas.drawRect(x + 3f, y + ch - 6f, x + 9f, y + ch - 2f, lamp)
            canvas.drawRect(x + cw - 9f, y + ch - 6f, x + cw - 3f, y + ch - 2f, lamp)
            canvas.drawRect(x + 2f, y + ch, x + cw - 2f, y + ch + 16f, beam)
            canvas.drawRect(x, y, x + 3f, y + ch, bodyDark)
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
        // Palos: 1 = corazones, 2 = diamantes, 3 = picas (con color rojo/negro)
        val suitSymbols = mapOf(1 to "♥", 2 to "♦", 3 to "♠")
        val suitColors = mapOf(1 to Color.parseColor("#D82800"), 2 to Color.parseColor("#D82800"), 3 to Color.parseColor("#1A1A1A"))
        for (i in 0..5) {
            val r = RectF(startX + i * cardW + 4, h / 2f - cardH / 2f, startX + (i + 1) * cardW - 4, h / 2f + cardH / 2f)
            // reverso de carta con patrón de rombos
            paint.color = if (i == sel) Color.parseColor("#F8D878") else Color.parseColor("#F8F8F8")
            canvas.drawRoundRect(r, 6f, 6f, paint)
            if (revealed[i]) {
                // interior blanco con marco y palo
                paint.color = Color.parseColor("#F8F8F8")
                canvas.drawRoundRect(r, 6f, 6f, paint)
                val frame = Paint().apply { color = suitColors[cards[i]]!!; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = false }
                canvas.drawRoundRect(r, 6f, 6f, frame)
                paint.color = suitColors[cards[i]]!!
                paint.textSize = 34f
                canvas.drawText(suitSymbols[cards[i]]!!, r.centerX(), r.centerY() + 12, paint)
                // índice pequeño en la esquina
                paint.textSize = 14f
                canvas.drawText(suitSymbols[cards[i]]!!, r.centerX() - 16f, r.top + 18f, paint)
                paint.textSize = 24f
            } else if (i != sel) {
                // patrón del reverso: rombos rojos
                val pattern = Paint().apply { color = Color.parseColor("#C41E24"); isAntiAlias = false }
                var py = r.top + 12f
                var rowShift = 0f
                while (py < r.bottom - 8f) {
                    var pxx = r.left + 12f + rowShift
                    while (pxx < r.right - 8f) {
                        canvas.drawCircle(pxx, py, 2.5f, pattern)
                        pxx += 14f
                    }
                    rowShift = if (rowShift == 0f) 7f else 0f
                    py += 12f
                }
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
        // Dado: cuerpo marfil biselado con pips reales según el valor
        val size = 90f
        val r = RectF(w / 2f - size / 2f, h / 2f - size / 2f, w / 2f + size / 2f, h / 2f + size / 2f)
        // sombra sobre la mesa
        paint.color = Color.parseColor("#000000"); paint.alpha = 60
        canvas.drawRoundRect(r, 12f, 12f, paint); paint.alpha = 255
        // cuerpo
        paint.color = Color.parseColor("#F8F8F0")
        canvas.drawRoundRect(r, 12f, 12f, paint)
        // bisel: borde superior claro, inferior oscuro
        val bvl = Paint().apply { isAntiAlias = false }
        bvl.color = Color.parseColor("#000000")
        bvl.style = Paint.Style.STROKE; bvl.strokeWidth = 3f
        canvas.drawRoundRect(r, 12f, 12f, bvl)
        bvl.color = Color.parseColor("#D8D8C8"); bvl.style = Paint.Style.FILL
        canvas.drawRect(r.left + 4f, r.top + 4f, r.right - 4f, r.top + 10f, bvl)
        // pips (coordenadas de las 6 caras)
        val pip = Paint().apply { color = Color.parseColor("#1A1A1A"); isAntiAlias = false }
        val pad = 22f
        val cx0 = r.left + pad; val cy0 = r.top + pad
        val cx1 = r.right - pad; val cy1 = r.bottom - pad
        val mx = r.centerX(); val my = r.centerY()
        val layouts: Map<Int, List<Pair<Float, Float>>> = mapOf(
            1 to listOf(mx to my),
            2 to listOf(cx0 to cy0, cx1 to cy1),
            3 to listOf(cx0 to cy0, mx to my, cx1 to cy1),
            4 to listOf(cx0 to cy0, cx1 to cy0, cx0 to cy1, cx1 to cy1),
            5 to listOf(cx0 to cy0, cx1 to cy0, mx to my, cx0 to cy1, cx1 to cy1),
            6 to listOf(cx0 to cy0, cx1 to cy0, cx0 to my, cx1 to my, cx0 to cy1, cx1 to cy1)
        )
        for ((px, py) in layouts[currentDice]!!) {
            canvas.drawCircle(px, py, 6f, pip)
            // brillo del pip
            val hi = Paint().apply { color = Color.parseColor("#F8F8F8"); alpha = 120; isAntiAlias = false }
            canvas.drawCircle(px - 2f, py - 2f, 2f, hi)
        }

        paint.color = Color.WHITE
        paint.textSize = 22f
        canvas.drawText("ARRIBA = Mayor | ABAJO = Menor", w / 2f, h * 0.82f, paint)
    }
}
