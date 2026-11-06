package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

// 5. Pixel Draw: Conexión de puntos y líneas para cerrar polígonos
class PixelDrawGame : Game {
    override val name = "Pixel Draw"
    override val genre = "Puzzle"
    private var w = 0; private var h = 0
    private var curX = 100f; private var curY = 100f
    private val points = mutableListOf<Pair<Float, Float>>()
    private val path = mutableListOf<Pair<Float, Float>>()
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; strokeWidth = 5f }

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        points.clear(); path.clear()
        isGameOver = false; score = 0
        curX = w * 0.2f; curY = h * 0.5f
        points.add(w * 0.4f to h * 0.3f)
        points.add(w * 0.6f to h * 0.3f)
        points.add(w * 0.5f to h * 0.7f)
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        val speed = 250f * dt
        if (input.left) curX -= speed
        if (input.right) curX += speed
        if (input.up) curY -= speed
        if (input.down) curY += speed

        path.add(curX to curY)
        if (path.size > 80) path.removeAt(0)

        val it = points.iterator()
        while (it.hasNext()) {
            val (px, py) = it.next()
            if (kotlin.math.hypot(curX - px, curY - py) < 25f) {
                it.remove()
                score += 100
                if (points.isEmpty()) {
                    isGameOver = false
                    reset()
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#000000"))
        paint.color = Color.parseColor("#5C94FC")
        paint.style = Paint.Style.STROKE
        for (i in 0 until path.size - 1) {
            canvas.drawLine(path[i].first, path[i].second, path[i+1].first, path[i+1].second, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8D878")
        for ((px, py) in points) canvas.drawCircle(px, py, 14f, paint)
        paint.color = Color.parseColor("#D82800")
        canvas.drawCircle(curX, curY, 12f, paint)
    }
}

// 6. Pixel Minas: Esquivar y desactivar minas en cuadrícula retro
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
        canvas.drawColor(Color.parseColor("#000000"))
        val size = 50f
        val ox = (w - size * 5) / 2f; val oy = (h - size * 5) / 2f
        for (r in 0..4) {
            for (c in 0..4) {
                paint.color = if (revealed[r][c]) {
                    if (grid[r][c]) Color.parseColor("#D82800") else Color.parseColor("#2D3748")
                } else {
                    if (r == selY && c == selX) Color.parseColor("#F8D878") else Color.parseColor("#F8F8F8")
                }
                canvas.drawRect(RectF(ox + c * size + 3, oy + r * size + 3, ox + (c + 1) * size - 3, oy + (r + 1) * size - 3), paint)
            }
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
        canvas.drawColor(Color.parseColor("#000000"))
        paint.color = Color.parseColor("#2D3748")
        canvas.drawRect(0f, h * 0.7f + 25f, w.toFloat(), h.toFloat(), paint)
        paint.color = Color.parseColor("#5C94FC")
        canvas.drawRoundRect(RectF(100f, py - 20f, 135f, py + 25f), 4f, 4f, paint)
        paint.color = Color.parseColor("#D82800")
        for (ox in obstacles) {
            canvas.drawRect(ox, h * 0.7f - 10f, ox + 30f, h * 0.7f + 25f, paint)
        }
    }
}

// 8. Reflejos: Reacción ultra rápida al cambio visual de color
class ReflejosGame : Game {
    override val name = "Reflejos"
    override val genre = "Arcade"
    private var w = 0; private var h = 0
    override var isGameOver = false
    override var score = 0
    private var state = 0 // 0 esperando, 1 presiona ya!
    private var timer = 2f
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        score = 0; isGameOver = false; state = 0; timer = Random.nextFloat() * 2f + 1.5f
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        timer -= dt
        if (state == 0 && timer <= 0f) {
            state = 1
            timer = 0.6f
        } else if (state == 1 && timer <= 0f) {
            isGameOver = true
        }

        if (input.a) {
            if (state == 1) {
                score += 100
                reset()
            } else if (state == 0) {
                isGameOver = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        val color = if (state == 1) Color.parseColor("#00A800") else Color.parseColor("#000000")
        canvas.drawColor(color)
        paint.color = Color.WHITE
        paint.textSize = 32f
        val msg = if (state == 1) "¡PRESIONA A!" else "ESPERA..."
        canvas.drawText(msg, w / 2f, h / 2f, paint)
    }
}
