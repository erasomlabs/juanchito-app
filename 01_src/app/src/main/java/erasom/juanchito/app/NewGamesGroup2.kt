package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random
class PixelMinasGame : Game {
    override val name = "Pixel Minas"
    override val genre = "Estrategia"
    private var w = 0; private var h = 0
    private var selX = 0; private var selY = 0
    private val grid = Array(5) { BooleanArray(5) }
    private val revealed = Array(5) { BooleanArray(5) }
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true }
    private var moved = false

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        isGameOver = false; score = 0; selX = 0; selY = 0
        for (r in 0..4) for (c in 0..4) {
            grid[r][c] = Random.nextFloat() < 0.22f
            revealed[r][c] = false
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right && !input.up && !input.down && !input.a) { moved = false; return }
        if (moved) return

        if (input.left && selX > 0) { selX--; moved = true }
        if (input.right && selX < 4) { selX++; moved = true }
        if (input.up && selY > 0) { selY--; moved = true }
        if (input.down && selY < 4) { selY++; moved = true }

        if (input.a && !revealed[selY][selX]) {
            revealed[selY][selX] = true
            moved = true
            if (grid[selY][selX]) {
                isGameOver = true
            } else {
                score += 50
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Desierto al atardecer con campo minado
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#28285A", "#8A4A2A", 6)
        NesScenery.drawStars(canvas, w, h, 25)
        NesScenery.drawCacti(canvas, listOf(w * 0.1f, w * 0.9f), h - 16f)
        val size = 50f
        val ox = (w - size * 5) / 2f; val oy = (h - size * 5) / 2f
        // Tierra del campo: casillas con textura de suelo
        for (r in 0..4) {
            for (c in 0..4) {
                val rect = RectF(ox + c * size + 3, oy + r * size + 3, ox + (c + 1) * size - 3, oy + (r + 1) * size - 3)
                if (revealed[r][c]) {
                    if (grid[r][c]) {
                        // Mina detonada: cráter con mina de púas y estallido
                        paint.color = Color.parseColor("#2A1010")
                        canvas.drawRect(rect, paint)
                        // mina: cuerpo negro espinado
                        val mineP = Paint().apply { color = Color.parseColor("#1A1A1A"); isAntiAlias = false }
                        val spikeP = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
                        val mx = rect.centerX(); val my = rect.centerY()
                        canvas.drawCircle(mx, my, 11f, mineP)
                        // púas en 8 direcciones
                        val angles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
                        for (ang in angles) {
                            val rad = Math.toRadians(ang.toDouble())
                            val x1 = mx + Math.cos(rad).toFloat() * 11f
                            val y1 = my + Math.sin(rad).toFloat() * 11f
                            val x2 = mx + Math.cos(rad).toFloat() * 17f
                            val y2 = my + Math.sin(rad).toFloat() * 17f
                            canvas.drawLine(x1, y1, x2, y2, spikeP)
                        }
                        // luz roja de la mina
                        val lamp = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
                        canvas.drawCircle(mx, my, 3.5f, lamp)
                        val lampHi = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
                        canvas.drawCircle(mx - 1f, my - 1f, 1.5f, lampHi)
                    } else {
                        // Celda despejada: suelo con huella de verificación verde
                        paint.color = Color.parseColor("#2D3748")
                        canvas.drawRect(rect, paint)
                        val check = Paint().apply { color = Color.parseColor("#00A800"); style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = false }
                        canvas.drawLine(rect.left + 10f, rect.centerY(), rect.centerX() - 2f, rect.bottom - 12f, check)
                        canvas.drawLine(rect.centerX() - 2f, rect.bottom - 12f, rect.right - 10f, rect.top + 12f, check)
                    }
                } else {
                    // Celda sin revelar: tapa metálica con remaches
                    paint.color = if (r == selY && c == selX) Color.parseColor("#F8D878") else Color.parseColor("#C8C8C8")
                    canvas.drawRect(rect, paint)
                    val plate = Paint().apply { color = Color.parseColor("#8A8A8A"); isAntiAlias = false }
                    canvas.drawRect(rect.left + 6f, rect.top + 6f, rect.right - 6f, rect.bottom - 6f, plate)
                    val rivet = Paint().apply { color = Color.parseColor("#F0F0F0"); isAntiAlias = false }
                    canvas.drawCircle(rect.left + 9f, rect.top + 9f, 2f, rivet)
                    canvas.drawCircle(rect.right - 9f, rect.top + 9f, 2f, rivet)
                    canvas.drawCircle(rect.left + 9f, rect.bottom - 9f, 2f, rivet)
                    canvas.drawCircle(rect.right - 9f, rect.bottom - 9f, 2f, rivet)
                }
            }
        }
        // Marcador de sapper Pixel sobre la celda seleccionada
        if (!revealed[selY][selX]) {
            val fx = ox + selX * size + size / 2f
            val fy = oy + selY * size - 14f
            // banderín de sapper
            canvas.drawRect(fx - 1.5f, fy - 10f, fx + 1.5f, fy, Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false })
            canvas.drawRect(fx + 1.5f, fy - 10f, fx + 10f, fy - 4f, Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false })
        }
    }
}

// 7. Pixel Runner: Endless runner con obstáculos ascendentes y saltos
class PixelRunnerGame : Game {
    override val name = "Pixel Runner"
    override val genre = "Plataformas"
    private var w = 0; private var h = 0
    private var py = 0f; private var vy = 0f
    private val obstacles = mutableListOf<Float>()
    override var isGameOver = false
    override var score = 0
    private var timer = 0f
    private val paint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        py = h * 0.7f; vy = 0f; isGameOver = false; score = 0
        obstacles.clear()
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        val floor = h * 0.7f
        if (input.a && py >= floor) vy = -460f
        vy += 1200f * dt
        py += vy * dt
        if (py > floor) { py = floor; vy = 0f }

        timer += dt
        if (timer > 1.4f) {
            obstacles.add(w.toFloat())
            timer = 0f
        }

        val it = obstacles.listIterator()
        while (it.hasNext()) {
            val ox = it.next() - 320f * dt
            if (ox < -40f) {
                it.remove()
                score += 20
            } else {
                it.set(ox)
                if (ox in 80f..140f && py > floor - 35f) {
                    isGameOver = true
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Bosque con colinas y nubes
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#5C94FC", "#A8D8FC", 5)
        NesScenery.drawClouds(canvas, w, h * 0.25f)
        NesScenery.drawHills(canvas, w, h * 0.7f, "#00A800", "#008800")
        paint.color = Color.parseColor("#2D3748")
        canvas.drawRect(0f, h * 0.7f + 25f, w.toFloat(), h.toFloat(), paint)
        // Pixel corriendo
        PixelSprite.draw(canvas, 117f, py + 4f, 30f, facingRight = true)
        // Obstáculos: arbustos de espinas con ojos perversos (terreno hostil)
        for (ox in obstacles) {
            val bushTop = h * 0.7f - 14f
            val groundY2 = h * 0.7f + 25f
            val bodyP = Paint().apply { color = Color.parseColor("#1A6A1A"); isAntiAlias = false }
            val darkP = Paint().apply { color = Color.parseColor("#0A4A0A"); isAntiAlias = false }
            val spikeP = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
            val eyeP = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
            val pupilP = Paint().apply { color = Color.parseColor("#000000"); isAntiAlias = false }
            // cuerpo del arbusto
            canvas.drawRoundRect(RectF(ox - 4f, bushTop + 4f, ox + 34f, groundY2), 6f, 6f, bodyP)
            canvas.drawRect(ox - 4f, groundY2 - 10f, ox + 34f, groundY2, darkP)
            // espinas arriba y laterales
            for (i in 0..3) {
                val sx = ox + 2f + i * 9f
                canvas.drawRect(sx, bushTop - 6f, sx + 4f, bushTop + 8f, spikeP)
            }
            canvas.drawRect(ox - 10f, bushTop + 12f, ox, bushTop + 17f, spikeP)
            canvas.drawRect(ox + 34f, bushTop + 12f, ox + 44f, bushTop + 17f, spikeP)
            // ojos perversos
            canvas.drawCircle(ox + 9f, bushTop + 20f, 3.5f, eyeP)
            canvas.drawCircle(ox + 23f, bushTop + 20f, 3.5f, eyeP)
            canvas.drawCircle(ox + 9f, bushTop + 21f, 1.5f, pupilP)
            canvas.drawCircle(ox + 23f, bushTop + 21f, 1.5f, pupilP)
            // boca dentada
            canvas.drawRect(ox + 8f, bushTop + 27f, ox + 24f, bushTop + 29f, pupilP)
        }
    }
}
