package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

// 1. Pixel Smash: Romper bloques especiales con física rápida y powerups
class PixelSmashGame : Game {
    override val name = "Pixel Smash"
    override val genre = "Arcade"
    private var w = 0; private var h = 0
    private var paddleX = 0f; private var paddleWidth = 120f
    private var ballX = 0f; private var ballY = 0f
    private var ballVx = 300f; private var ballVy = -320f
    private val bricks = mutableListOf<RectF>()
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) {
        w = width; h = height
    }

    override fun reset() {
        paddleWidth = 140f
        paddleX = w / 2f - paddleWidth / 2f
        ballX = w / 2f; ballY = h * 0.7f
        ballVx = 320f; ballVy = -340f
        score = 0; isGameOver = false
        bricks.clear()
        val cols = 8; val rows = 5
        val bw = (w - 80f) / cols; val bh = 24f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                bricks.add(RectF(40f + c * bw + 3f, 40f + r * bh + 3f, 40f + (c + 1) * bw - 3f, 40f + (r + 1) * bh - 3f))
            }
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        val speed = 500f * dt
        if (input.left) paddleX = (paddleX - speed).coerceAtLeast(0f)
        if (input.right) paddleX = (paddleX + speed).coerceAtMost(w - paddleWidth)

        ballX += ballVx * dt
        ballY += ballVy * dt

        if (ballX <= 12f) { ballX = 12f; ballVx = -ballVx }
        if (ballX >= w - 12f) { ballX = w - 12f; ballVx = -ballVx }
        if (ballY <= 12f) { ballY = 12f; ballVy = -ballVy }

        val pTop = h - 60f
        if (ballY + 10f >= pTop && ballY - 10f <= pTop + 20f && ballX >= paddleX && ballX <= paddleX + paddleWidth && ballVy > 0) {
            ballVy = -ballVy * 1.05f
            ballVx += (ballX - (paddleX + paddleWidth / 2f)) * 4f
        }

        val iterator = bricks.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            if (b.contains(ballX, ballY)) {
                iterator.remove()
                ballVy = -ballVy
                score += 25
                break
            }
        }

        if (ballY > h) isGameOver = true
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        paint.color = Color.parseColor("#FECA57")
        canvas.drawRoundRect(RectF(paddleX, h - 60f, paddleX + paddleWidth, h - 42f), 6f, 6f, paint)

        paint.color = Color.parseColor("#00D9FF")
        canvas.drawCircle(ballX, ballY, 10f, paint)

        paint.color = Color.parseColor("#FF6B6B")
        for (b in bricks) {
            canvas.drawRoundRect(b, 4f, 4f, paint)
        }
    }
}

// 2. Fusion Pixel: Unir orbes numéricos/colores
class FusionPixelGame : Game {
    override val name = "Fusion Pixel"
    override val genre = "Puzzle"
    private var w = 0; private var h = 0
    private var grid = Array(4) { IntArray(4) }
    override var isGameOver = false
    override var score = 0
    private val paint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var canMove = true

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() {
        score = 0; isGameOver = false
        grid = Array(4) { IntArray(4) }
        spawn(); spawn()
    }

    private fun spawn() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) for (c in 0..3) if (grid[r][c] == 0) empty.add(r to c)
        if (empty.isNotEmpty()) {
            val (r, c) = empty.random()
            grid[r][c] = if (Random.nextFloat() < 0.8f) 2 else 4
        }
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        if (!input.left && !input.right && !input.up && !input.down) { canMove = true; return }
        if (!canMove) return

        var moved = false
        if (input.left) { moved = slideLeft(); canMove = false }
        else if (input.right) { rotate(); rotate(); moved = slideLeft(); rotate(); rotate(); canMove = false }
        else if (input.up) { rotate(); rotate(); rotate(); moved = slideLeft(); rotate(); canMove = false }
        else if (input.down) { rotate(); moved = slideLeft(); rotate(); rotate(); rotate(); canMove = false }

        if (moved) spawn()
    }

    private fun slideLeft(): Boolean {
        var changed = false
        for (r in 0..3) {
            val row = grid[r].filter { it != 0 }.toMutableList()
            var c = 0
            while (c < row.size - 1) {
                if (row[c] == row[c + 1]) {
                    row[c] *= 2
                    score += row[c]
                    row.removeAt(c + 1)
                    changed = true
                }
                c++
            }
            while (row.size < 4) row.add(0)
            for (i in 0..3) {
                if (grid[r][i] != row[i]) changed = true
                grid[r][i] = row[i]
            }
        }
        return changed
    }

    private fun rotate() {
        val next = Array(4) { IntArray(4) }
        for (r in 0..3) for (c in 0..3) next[c][3 - r] = grid[r][c]
        grid = next
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        val size = 70f
        val startX = (w - size * 4) / 2f
        val startY = (h - size * 4) / 2f
        paint.textSize = 28f
        for (r in 0..3) {
            for (c in 0..3) {
                val v = grid[r][c]
                paint.color = when (v) {
                    0 -> Color.parseColor("#2D3748")
                    2 -> Color.parseColor("#48DBFB")
                    4 -> Color.parseColor("#00D9FF")
                    8 -> Color.parseColor("#FECA57")
                    16 -> Color.parseColor("#FF6B6B")
                    else -> Color.parseColor("#FD79A8")
                }
                val rect = RectF(startX + c * size + 4, startY + r * size + 4, startX + (c + 1) * size - 4, startY + (r + 1) * size - 4)
                canvas.drawRoundRect(rect, 8f, 8f, paint)
                if (v > 0) {
                    paint.color = Color.WHITE
                    canvas.drawText("$v", rect.centerX(), rect.centerY() + 10, paint)
                }
            }
        }
    }
}

// 3. Pixel Blocks: Rompecabezas geométrico de caída y eliminación
class PixelBlocksGame : Game {
    override val name = "Pixel Blocks"
    override val genre = "Puzzle"
    private var w = 0; private var h = 0
    override var isGameOver = false
    override var score = 0
    private var targetRow = 0
    private var speed = 400f
    private var blockX = 100f
    private var dir = 1
    private val paint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() { isGameOver = false; score = 0; blockX = 100f; dir = 1 }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        blockX += dir * speed * dt
        if (blockX > w - 160f) { blockX = w - 160f; dir = -1 }
        if (blockX < 40f) { blockX = 40f; dir = 1 }
        if (input.a) {
            val center = w / 2f
            if (kotlin.math.abs(blockX + 60f - center) < 80f) {
                score += 100
                speed += 25f
            } else {
                isGameOver = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        paint.color = Color.parseColor("#48DBFB")
        canvas.drawRect(RectF(w / 2f - 40f, 0f, w / 2f + 40f, h.toFloat()), Paint().apply { color = Color.parseColor("#2D3748") })
        paint.color = Color.parseColor("#00D9FF")
        canvas.drawRoundRect(RectF(blockX, h * 0.45f, blockX + 120f, h * 0.45f + 45f), 8f, 8f, paint)
    }
}

// 4. Pixel Ritmo: Juego de tempo y reflejos con botones
class PixelRitmoGame : Game {
    override val name = "Pixel Ritmo"
    override val genre = "Ritmo"
    private var w = 0; private var h = 0
    private val notes = mutableListOf<Float>()
    override var isGameOver = false
    override var score = 0
    private var timer = 0f
    private val paint = Paint().apply { isAntiAlias = true }

    override fun init(width: Int, height: Int) { w = width; h = height }
    override fun reset() { notes.clear(); isGameOver = false; score = 0; timer = 0f }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) return
        timer += dt
        if (timer > 1.2f) {
            notes.add(0f)
            timer = 0f
        }
        for (i in notes.indices) {
            notes[i] += 260f * dt
        }
        if (notes.isNotEmpty() && notes.first() > h) {
            isGameOver = true
        }
        if (input.a && notes.isNotEmpty()) {
            val hitY = notes.first()
            if (hitY in (h - 120f)..(h - 20f)) {
                notes.removeAt(0)
                score += 50
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        paint.color = Color.parseColor("#FECA57")
        canvas.drawLine(0f, h - 70f, w.toFloat(), h - 70f, paint)
        paint.color = Color.parseColor("#FF6B6B")
        for (y in notes) {
            canvas.drawCircle(w / 2f, y, 22f, paint)
        }
    }
}
