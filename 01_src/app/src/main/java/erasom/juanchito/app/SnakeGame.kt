package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class SnakeGame : Game {
    override val name = "Serpiente Retro"
    override val genre = "Arcade"
    private var w = 0
    private var h = 0

    private val gridSize = 24
    private var cols = 0
    private var rows = 0
    private val snake = mutableListOf<Pair<Int, Int>>()
    private var food = Pair(0, 0)
    private var dir = Direction.RIGHT
    private var nextDir = Direction.RIGHT
    private var moveTimer = 0f
    private var moveInterval = 0.15f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0

    private val snakePaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val headPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val foodPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 36f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 24f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        cols = w / gridSize
        rows = h / gridSize
        reset()
    }

    override fun reset() {
        snake.clear()
        val cx = cols / 2
        val cy = rows / 2
        snake.add(Pair(cx, cy))
        snake.add(Pair(cx - 1, cy))
        snake.add(Pair(cx - 2, cy))
        dir = Direction.RIGHT
        nextDir = Direction.RIGHT
        score = 0
        isGameOver = false
        spawnFood()
    }

    private fun spawnFood() {
        do {
            food = Pair(Random.nextInt(cols), Random.nextInt(rows))
        } while (snake.contains(food))
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) {
            if (input.start) reset()
            return
        }
        // Change direction
        if (input.up && dir != Direction.DOWN) nextDir = Direction.UP
        if (input.down && dir != Direction.UP) nextDir = Direction.DOWN
        if (input.left && dir != Direction.RIGHT) nextDir = Direction.LEFT
        if (input.right && dir != Direction.LEFT) nextDir = Direction.RIGHT

        // Speed increases with length (Nokia Snake style)
        moveInterval = (0.18f - snake.size * 0.002f).coerceAtLeast(0.06f)
        moveTimer += dt
        if (moveTimer < moveInterval) return
        moveTimer = 0f
        dir = nextDir

        val head = snake.first()
        val newHead = when (dir) {
            Direction.UP -> Pair(head.first, head.second - 1)
            Direction.DOWN -> Pair(head.first, head.second + 1)
            Direction.LEFT -> Pair(head.first - 1, head.second)
            Direction.RIGHT -> Pair(head.first + 1, head.second)
        }

        // Wall collision
        if (newHead.first < 0 || newHead.first >= cols || newHead.second < 0 || newHead.second >= rows) {
            gameOver(); return
        }
        // Self collision
        if (snake.contains(newHead)) {
            gameOver(); return
        }

        snake.add(0, newHead)
        if (newHead == food) {
            score += 10
            if (score > highScore) highScore = score
            spawnFood()
        } else {
            snake.removeAt(snake.size - 1)
        }
    }

    private fun gameOver() {
        isGameOver = true
    }

    override fun draw(canvas: Canvas) {
        // Cancha ajedrezada estilo jardín NES
        val grass1 = Paint().apply { color = Color.parseColor("#0A6A0A"); isAntiAlias = false }
        val grass2 = Paint().apply { color = Color.parseColor("#0E7A0E"); isAntiAlias = false }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val paint = if ((r + c) % 2 == 0) grass1 else grass2
                canvas.drawRect(c * gridSize.toFloat(), r * gridSize.toFloat(),
                    (c + 1) * gridSize.toFloat(), (r + 1) * gridSize.toFloat(), paint)
            }
        }
        // Muro perimetral de piedra
        val wall = Paint().apply { color = Color.parseColor("#7C7C7C"); isAntiAlias = false }
        val wallDark = Paint().apply { color = Color.parseColor("#4A4A4A"); isAntiAlias = false }
        canvas.drawRect(0f, 0f, w.toFloat(), 4f, wall)
        canvas.drawRect(0f, h - 4f, w.toFloat(), h.toFloat(), wall)
        canvas.drawRect(0f, 0f, 4f, h.toFloat(), wall)
        canvas.drawRect(w - 4f, 0f, w.toFloat(), h.toFloat(), wall)
        var wx = 0f
        while (wx < w) {
            canvas.drawRect(wx, 0f, wx + 2f, 4f, wallDark)
            canvas.drawRect(wx, h - 4f, wx + 2f, h.toFloat(), wallDark)
            wx += 16f
        }
        // Manzana: fruto rojo, brillo, hoja y tallo
        val fx = food.first * gridSize.toFloat(); val fy = food.second * gridSize.toFloat()
        canvas.drawCircle(fx + gridSize / 2f, fy + gridSize / 2f + 1f, gridSize * 0.4f, foodPaint)
        val shine = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
        canvas.drawCircle(fx + gridSize * 0.35f, fy + gridSize * 0.38f, gridSize * 0.12f, shine)
        val stem = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = false }
        canvas.drawRect(fx + gridSize / 2f - 1.5f, fy + 2f, fx + gridSize / 2f + 1.5f, fy + gridSize * 0.4f, stem)
        val leaf = Paint().apply { color = Color.parseColor("#00E000"); isAntiAlias = false }
        canvas.drawRect(fx + gridSize / 2f + 2f, fy + 2f, fx + gridSize * 0.8f, fy + gridSize * 0.25f, leaf)
        // Serpiente: cuerpo con escamas alternadas
        snake.forEachIndexed { i, seg ->
            val x = seg.first * gridSize.toFloat(); val y = seg.second * gridSize.toFloat()
            if (i == 0) {
                // Cabeza con gorra Juanchito
                canvas.drawRoundRect(RectF(x + 1f, y + 3f, x + gridSize - 1f, y + gridSize - 1f), 7f, 7f, headPaint)
                // gorra roja
                val cap = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
                canvas.drawRect(x + 1f, y + 3f, x + gridSize - 1f, y + gridSize * 0.35f, cap)
                canvas.drawRect(x + 1f, y + gridSize * 0.35f, x + gridSize - 1f, y + gridSize * 0.42f, cap)
                // ojos según dirección
                val eye = Paint().apply { color = Color.parseColor("#000000"); isAntiAlias = false }
                val cxp = x + gridSize / 2f; val cyp = y + gridSize * 0.6f
                when (dir) {
                    Direction.RIGHT, Direction.LEFT -> {
                        canvas.drawCircle(cxp + gridSize * 0.22f, cyp, 2.5f, eye)
                        canvas.drawCircle(cxp - gridSize * 0.22f, cyp, 2.5f, eye)
                    }
                    else -> {
                        canvas.drawCircle(cxp + gridSize * 0.18f, cyp, 2.5f, eye)
                        canvas.drawCircle(cxp - gridSize * 0.18f, cyp, 2.5f, eye)
                    }
                }
                // lengua bífida
                val tongue = Paint().apply { color = Color.parseColor("#D82800"); strokeWidth = 2f; isAntiAlias = false }
                when (dir) {
                    Direction.RIGHT -> canvas.drawLine(x + gridSize - 1f, cyp, x + gridSize + 5f, cyp, tongue)
                    Direction.LEFT -> canvas.drawLine(x + 1f, cyp, x - 5f, cyp, tongue)
                    Direction.UP -> canvas.drawLine(cxp, y + 3f, cxp, y - 5f, tongue)
                    Direction.DOWN -> canvas.drawLine(cxp, y + gridSize - 1f, cxp, y + gridSize + 5f, tongue)
                }
            } else {
                val shade = if (i % 2 == 0) snakePaint else Paint().apply { color = Color.parseColor("#007000"); isAntiAlias = false }
                canvas.drawRoundRect(RectF(x + 2f, y + 2f, x + gridSize - 2f, y + gridSize - 2f), 5f, 5f, shade)
                // escama central
                val scale = Paint().apply { color = Color.parseColor("#30D030"); isAntiAlias = false }
                canvas.drawCircle(x + gridSize / 2f, y + gridSize / 2f, 2.5f, scale)
            }
        }
        // Score
        canvas.drawText("Score: $score", w / 2f, 40f, textPaint.apply { textSize = 28f })
        canvas.drawText("Best: $highScore", w / 2f, 70f, smallPaint)
        if (isGameOver) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            canvas.drawText("GAME OVER", w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#E63946") })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 50f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}
