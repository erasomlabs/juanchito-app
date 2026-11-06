package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PuzzleBobbleGame : Game {
    override val name = "Puzzle Bobble"
    override val genre = "Puzzle"
    private var w = 0; private var h = 0
    private val rows = 8; private val cols = 8
    private var cell = 0f
    private var grid = Array(rows) { IntArray(cols) } // 0 empty, 1-4 color
    private var shooterX = 0f; private var shooterY = 0f
    private var angle = -Math.PI.toFloat() / 2f
    private var bubbleX = 0f; private var bubbleY = 0f
    private var bubbleVx = 0f; private var bubbleVy = 0f
    private var flying = false
    private var nextColor = 1
    private var curColor = 1
    private val colors = listOf("#1A1A2E", "#00D9FF", "#FF6B6B", "#FECA57", "#48DBFB")
    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    override var isGameOver = false
    override var score = 0
    private var highScore = 0

    override fun init(width: Int, height: Int) { w = width; h = height; cell = (w / cols).toFloat().coerceAtMost(h / (rows + 4).toFloat()); reset() }

    override fun reset() {
        for (r in 0 until rows) for (c in 0 until cols) grid[r][c] = 0
        // fill top rows
        for (r in 0 until 4) for (c in 0 until cols) grid[r][c] = Random.nextInt(1, 5)
        shooterX = w / 2f; shooterY = h - 60f
        curColor = Random.nextInt(1, 5); nextColor = Random.nextInt(1, 5)
        flying = false; angle = -Math.PI.toFloat()/2f
        score = 0; isGameOver = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        if (input.left) { angle -= 2f * dt; input.left = false }
        if (input.right) { angle += 2f * dt; input.right = false }
        angle = angle.coerceIn(-Math.PI.toFloat()*0.85f, -Math.PI.toFloat()*0.15f)
        if (input.a && !flying) {
            bubbleX = shooterX; bubbleY = shooterY
            bubbleVx = cos(angle) * 600f; bubbleVy = sin(angle) * 600f
            flying = true; input.a = false
        }
        if (flying) {
            bubbleX += bubbleVx * dt; bubbleY += bubbleVy * dt
            if (bubbleX < 0f || bubbleX > w - cell) { bubbleVx = -bubbleVx; bubbleX = bubbleX.coerceIn(0f, w - cell) }
            if (bubbleY < 0f) { landBubble(); return }
            // collision with grid
            for (r in 0 until rows) for (c in 0 until cols) {
                if (grid[r][c] != 0) {
                    val cx = c * cell + cell/2; val cy = r * cell + cell/2
                    val dx = bubbleX - cx; val dy = bubbleY - cy
                    if (dx*dx + dy*dy < cell*cell*0.8f) { landBubble(); return }
                }
            }
        }
    }

    private fun landBubble() {
        flying = false
        val c = (bubbleX / cell).toInt().coerceIn(0, cols-1)
        val r = (bubbleY / cell).toInt().coerceIn(0, rows-1)
        if (r in 0 until rows && c in 0 until cols && grid[r][c] == 0) {
            grid[r][c] = curColor
            // remove connected same color
            val visited = mutableSetOf<Pair<Int,Int>>()
            val stack = mutableListOf(Pair(r,c))
            while (stack.isNotEmpty()) {
                val (rr,cc) = stack.removeAt(stack.lastIndex)
                if (rr !in 0 until rows || cc !in 0 until cols) continue
                if (visited.contains(Pair(rr,cc))) continue
                if (grid[rr][cc] != curColor) continue
                visited.add(Pair(rr,cc))
                stack.add(Pair(rr-1,cc)); stack.add(Pair(rr+1,cc)); stack.add(Pair(rr,cc-1)); stack.add(Pair(rr,cc+1))
            }
            if (visited.size >= 3) {
                for ((rr,cc) in visited) grid[rr][cc] = 0
                score += visited.size * 10
                if (score > highScore) highScore = score
            }
            // drop floating
            // simple: if any bubble in bottom row disconnected... skip for simplicity
        }
        curColor = nextColor; nextColor = Random.nextInt(1,5)
        // game over if any bubble in last row
        if (grid[rows-1].any { it != 0 }) isGameOver = true
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        val ox = (w - cols*cell)/2f; val oy = 40f
        // grid
        for (r in 0 until rows) for (c in 0 until cols) {
            val v = grid[r][c]
            if (v != 0) {
                paint.color = Color.parseColor(colors[v])
                canvas.drawCircle(ox + c*cell + cell/2, oy + r*cell + cell/2, cell/2 - 2, paint)
            }
        }
        // shooter
        paint.color = Color.parseColor(colors[curColor])
        canvas.drawCircle(shooterX, shooterY, cell/2 - 2, paint)
        // aim line
        val lp = Paint().apply { color = Color.parseColor("#F5F5F5"); strokeWidth = 2f; alpha = 80 }
        canvas.drawLine(shooterX, shooterY, shooterX + cos(angle)*80, shooterY + sin(angle)*80, lp)
        // flying bubble
        if (flying) {
            paint.color = Color.parseColor(colors[curColor])
            canvas.drawCircle(bubbleX, bubbleY, cell/2 - 2, paint)
        }
        // next color indicator
        paint.color = Color.parseColor(colors[nextColor])
        canvas.drawCircle(w - 30f, h - 30f, cell/3, paint)
        // HUD
        canvas.drawText("Score: $score", w/2f, h - 10f, textPaint.apply { textSize = 24f })
        if (isGameOver) {
            canvas.drawText("GAME OVER", w/2f, h/2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#FF6B6B") })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}