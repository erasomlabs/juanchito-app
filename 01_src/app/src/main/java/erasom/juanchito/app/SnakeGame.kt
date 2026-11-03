package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class SnakeGame : Game {
    override val name = "Snake"
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
    private val moveInterval = 0.15f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0

    private val snakePaint = Paint().apply { color = Color.parseColor("#39FF14"); isAntiAlias = true }
    private val headPaint = Paint().apply { color = Color.parseColor("#FF6B1A"); isAntiAlias = true }
    private val foodPaint = Paint().apply { color = Color.parseColor("#E63946"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 36f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#FFD700"); textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

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
        canvas.drawColor(Color.parseColor("#0A0A0F"))
        // Food
        val fr = RectF(food.first * gridSize + 2f, food.second * gridSize + 2f,
            (food.first + 1) * gridSize - 2f, (food.second + 1) * gridSize - 2f)
        canvas.drawRoundRect(fr, 4f, 4f, foodPaint)
        // Snake
        snake.forEachIndexed { i, seg ->
            val r = RectF(seg.first * gridSize + 1f, seg.second * gridSize + 1f,
                (seg.first + 1) * gridSize - 1f, (seg.second + 1) * gridSize - 1f)
            if (i == 0) canvas.drawRoundRect(r, 6f, 6f, headPaint) else canvas.drawRoundRect(r, 4f, 4f, snakePaint)
        }
        // Score
        canvas.drawText("Score: $score", w / 2f, 40f, textPaint.apply { textSize = 28f })
        canvas.drawText("Best: $highScore", w / 2f, 70f, smallPaint)
        if (isGameOver) {
            canvas.drawText("GAME OVER", w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#E63946") })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 50f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}
