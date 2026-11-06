package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class BombermanGame : Game {
    override val name = "Bomb Labyrinth"
    override val genre = "Acción"
    private var w = 0; private var h = 0
    private val cols = 11; private val rows = 9
    private var cell = 0f
    private var grid = Array(rows) { IntArray(cols) } // 0 empty, 1 wall, 2 block
    private var px = 1; private var py = 1
    private var moveTimer = 0f; private val moveInterval = 0.18f
    private var nextDx = 0; private var nextDy = 0
    private val bombs = mutableListOf<Bomb>()
    private val enemies = mutableListOf<Enemy>()
    private var lives = 3
    private var invuln = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false

    private data class Bomb(val gx: Int, val gy: Int, var timer: Float)
    private data class Enemy(var gx: Int, var gy: Int, var moveTimer: Float)

    private val playerPaint = Paint().apply { color = Color.parseColor("#FECA57"); isAntiAlias = true }
    private val wallPaint = Paint().apply { color = Color.parseColor("#2D3748"); isAntiAlias = true }
    private val blockPaint = Paint().apply { color = Color.parseColor("#A29BFE"); isAntiAlias = true }
    private val bombPaint = Paint().apply { color = Color.parseColor("#FF6B6B"); isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#55EFC4"); isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.parseColor("#F5F5F5"); textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#48DBFB"); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; cell = (w / cols).coerceAtMost(h / rows).toFloat(); reset() }

    override fun reset() {
        for (r in 0 until rows) for (c in 0 until cols) {
            grid[r][c] = if (r == 0 || c == 0 || r == rows-1 || c == cols-1) 1
            else if (r % 2 == 0 && c % 2 == 0) 1
            else if (Random.nextFloat() < 0.35f && !(r <= 2 && c <= 2)) 2 else 0
        }
        grid[1][1] = 0; grid[1][2] = 0; grid[2][1] = 0
        px = 1; py = 1; bombs.clear(); enemies.clear()
        for (i in 0 until 4) {
            var ex: Int; var ey: Int
            do { ex = Random.nextInt(1, cols-1); ey = Random.nextInt(1, rows-1) } while (grid[ey][ex] != 0 || (ex <= 2 && ey <= 2))
            enemies.add(Enemy(ex, ey, Random.nextFloat() * 0.5f))
        }
        lives = 3; invuln = 0f; score = 0; isGameOver = false; won = false
    }

    override fun update(dt: Float, input: GameInput) {
        if (isGameOver) { if (input.start) reset(); return }
        invuln = (invuln - dt).coerceAtLeast(0f)
        // movement
        nextDx = 0; nextDy = 0
        if (input.up) nextDy = -1
        if (input.down) nextDy = 1
        if (input.left) nextDx = -1
        if (input.right) nextDx = 1
        moveTimer += dt
        if (moveTimer >= moveInterval && (nextDx != 0 || nextDy != 0)) {
            moveTimer = 0f
            val nx = px + nextDx; val ny = py + nextDy
            if (nx in 1 until cols-1 && ny in 1 until rows-1 && grid[ny][nx] == 0 && bombs.none { it.gx == nx && it.gy == ny }) {
                px = nx; py = ny
            }
        }
        // place bomb
        if (input.a) {
            if (bombs.none { it.gx == px && it.gy == py }) bombs.add(Bomb(px, py, 2.5f))
            input.a = false
        }
        // update bombs
        val bi = bombs.iterator()
        while (bi.hasNext()) {
            val b = bi.next()
            b.timer -= dt
            if (b.timer <= 0f) {
                // explode
                val cells = mutableListOf(Pair(b.gx, b.gy))
                for (d in listOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1))) {
                    for (s in 1..2) {
                        val cx = b.gx + d.first * s; val cy = b.gy + d.second * s
                        if (cx !in 0 until cols || cy !in 0 until rows) break
                        if (grid[cy][cx] == 1) break
                        if (grid[cy][cx] == 2) { grid[cy][cx] = 0; score += 5; cells.add(Pair(cx, cy)); break }
                        cells.add(Pair(cx, cy))
                    }
                }
                // kill enemies
                enemies.removeAll { e -> cells.any { it.first == e.gx && it.second == e.gy } }
                score += cells.count { c -> enemies.any { it.gx == c.first && it.gy == c.second } } * 0 // already removed
                // damage player
                if (cells.any { it.first == px && it.second == py } && invuln <= 0f) {
                    lives--; invuln = 2f
                    if (lives <= 0) isGameOver = true
                }
                bi.remove()
            }
        }
        // enemies move
        for (e in enemies) {
            e.moveTimer -= dt
            if (e.moveTimer <= 0f) {
                e.moveTimer = 0.6f
                val dirs = listOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1))
                val d = dirs[Random.nextInt(4)]
                val nx = e.gx + d.first; val ny = e.gy + d.second
                if (nx in 1 until cols-1 && ny in 1 until rows-1 && grid[ny][nx] == 0) { e.gx = nx; e.gy = ny }
            }
            if (e.gx == px && e.gy == py && invuln <= 0f) {
                lives--; invuln = 2f
                if (lives <= 0) isGameOver = true
            }
        }
        // win: all enemies dead
        if (enemies.isEmpty()) { won = true; isGameOver = true; score += 500 }
        if (score > highScore) highScore = score
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        val ox = (w - cols*cell)/2f; val oy = (h - rows*cell)/2f
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = ox + c*cell; val y = oy + r*cell
            when (grid[r][c]) {
                1 -> canvas.drawRect(x, y, x+cell-1, y+cell-1, wallPaint)
                2 -> canvas.drawRect(x+2, y+2, x+cell-3, y+cell-3, blockPaint)
                else -> { val p = Paint().apply { color = Color.parseColor("#252540") }; canvas.drawRect(x, y, x+cell-1, y+cell-1, p) }
            }
        }
        // bombs
        for (b in bombs) {
            bombPaint.color = if ((b.timer * 4).toInt() % 2 == 0) Color.parseColor("#FF6B6B") else Color.parseColor("#FECA57")
            canvas.drawCircle(ox + b.gx*cell + cell/2, oy + b.gy*cell + cell/2, cell/3, bombPaint)
        }
        // enemies
        for (e in enemies) {
            canvas.drawCircle(ox + e.gx*cell + cell/2, oy + e.gy*cell + cell/2, cell/3, enemyPaint)
        }
        // player
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            canvas.drawRoundRect(RectF(ox + px*cell + 4, oy + py*cell + 4, ox + (px+1)*cell - 4, oy + (py+1)*cell - 4), 4f, 4f, playerPaint)
        }
        // HUD
        textPaint.textSize = 22f
        canvas.drawText("Score: $score  Vidas: $lives", w/2f, 30f, textPaint)
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#55EFC4") else Color.parseColor("#FF6B6B")
            canvas.drawText(msg, w/2f, h/2f, textPaint.apply { textSize = 56f; color = col })
            canvas.drawText("Pulsa START", w/2f, h/2f + 60f, smallPaint.apply { textSize = 24f; color = Color.parseColor("#F5F5F5") })
        }
    }
}