package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PuzzleBobbleGame : Game {
    override val name = "Bubble Pop"
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
    private val colors = listOf("#000000", "#5C98FC", "#D82800", "#F8D878", "#00A800")
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
        // Mundo de burbujas: cielo brillante estilo Puzzle Bobble
        NesScenery.drawSkyBands(canvas, w, h.toFloat(), "#5C94FC", "#B8D8F8", 6)
        NesScenery.drawClouds(canvas, w, h * 0.18f)
        NesScenery.drawNeonFrame(canvas, w, h, "#F8D878")
        val ox = (w - cols * cell) / 2f; val oy = 40f
        // Techo de burbujas contenedor (franja superior)
        val ceil = Paint().apply { color = Color.parseColor("#8814A8"); isAntiAlias = false }
        val ceilDark = Paint().apply { color = Color.parseColor("#5A0A70"); isAntiAlias = false }
        canvas.drawRect(0f, 24f, w.toFloat(), 40f, ceil)
        var tx = 0f
        while (tx < w) {
            canvas.drawRect(tx, 24f, tx + 2f, 40f, ceilDark)
            tx += 18f
        }
        // Bola auxiliar con brillo
        fun bubble(cx: Float, cy: Float, radius: Float, colorHex: String) {
            val bp = Paint().apply { color = Color.parseColor(colorHex); isAntiAlias = false }
            canvas.drawCircle(cx, cy, radius, bp)
            val shine = Paint().apply { color = Color.parseColor("#F8F8F8"); alpha = 200; isAntiAlias = false }
            canvas.drawCircle(cx - radius * 0.35f, cy - radius * 0.35f, radius * 0.3f, shine)
            val rim = Paint().apply { color = Color.parseColor("#000000"); alpha = 60; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = false }
            canvas.drawCircle(cx, cy, radius, rim)
        }
        // grid
        for (r in 0 until rows) for (c in 0 until cols) {
            val v = grid[r][c]
            if (v != 0) bubble(ox + c * cell + cell / 2, oy + r * cell + cell / 2, cell / 2 - 2, colors[v])
        }
        // Cañón: Pixel operador + cañón apuntando
        val barrel = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
        val barrelTipX = shooterX + cos(angle) * cell * 0.9f
        val barrelTipY = shooterY + sin(angle) * cell * 0.9f
        canvas.save()
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f, shooterX, shooterY)
        canvas.drawRect(shooterX - cell * 0.18f, shooterY - cell * 1.1f, shooterX + cell * 0.18f, shooterY, barrel)
        canvas.restore()
        PixelSprite.draw(canvas, shooterX, shooterY + cell * 0.6f, cell * 1.1f, facingRight = true, hatColor = 0x00A800)
        // Burbuja cargada en la boca del cañón
        bubble(shooterX, shooterY, cell / 2 - 2, colors[curColor])
        // Línea de puntería punteada
        val lp = Paint().apply { color = Color.parseColor("#F5F5F5"); strokeWidth = 3f; alpha = 90; isAntiAlias = false }
        for (i in 1..4) {
            val d = cell * 1.4f * i
            canvas.drawCircle(shooterX + cos(angle) * d, shooterY + sin(angle) * d, 3f, lp)
        }
        // flying bubble
        if (flying) bubble(bubbleX, bubbleY, cell / 2 - 2, colors[curColor])
        // next color indicator con marco
        val nx = w - 34f; val ny = h - 40f
        paint.color = Color.parseColor("#000000"); paint.alpha = 120
        canvas.drawCircle(nx, ny, cell / 2 + 6, paint)
        paint.alpha = 255
        bubble(nx, ny, cell / 2 - 2, colors[nextColor])
        canvas.drawText("SIG", nx, ny - cell / 2 - 8, smallPaint.apply { textSize = 13f; color = Color.parseColor("#F8D878") })
        // HUD
        canvas.drawText("Score: $score", w / 2f, h - 10f, textPaint.apply { textSize = 24f })
        if (isGameOver) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            canvas.drawText("GAME OVER", w / 2f, h / 2f, textPaint.apply { textSize = 56f; color = Color.parseColor("#FF6B6B") })
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}