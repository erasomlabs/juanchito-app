package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

class BombermanGame : Game {
    override val name = "Bomberman 3"
    override val genre = "Acción"
    private var w = 0; private var h = 0
    private val cols = 13; private val rows = 11
    private var cell = 0f
    // 0 empty, 1 hard wall, 2 soft block, 3 exit (revealed)
    private var grid = Array(rows) { IntArray(cols) }
    private var px = 1; private var py = 1
    private var moveTimer = 0f; private val moveInterval = 0.16f
    private var nextDx = 0; private var nextDy = 0
    private val bombs = mutableListOf<Bomb>()
    private val explosions = mutableListOf<Explosion>()
    private val enemies = mutableListOf<Enemy>()
    private val items = mutableListOf<Item>()
    private var exitX = -1; private var exitY = -1; private var exitRevealed = false
    private var bombsMax = 1; private var fireRange = 2; private var speed = 1f
    private var lives = 3
    private var invuln = 0f
    override var isGameOver = false
    override var score = 0
    private var highScore = 0
    private var won = false
    private var stage = 1

    private data class Bomb(val gx: Int, val gy: Int, var timer: Float, val range: Int)
    private data class Explosion(val gx: Int, val gy: Int, var timer: Float)
    private data class Enemy(var gx: Int, var gy: Int, var type: Int, var moveTimer: Float, var dir: Int)
    private data class Item(val gx: Int, val gy: Int, val type: Int) // 0 bomb,1 fire,2 speed,3 kick

    private val playerPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val wallPaint = Paint().apply { color = Color.parseColor("#5C94FC"); isAntiAlias = false }
    private val softPaint = Paint().apply { color = Color.parseColor("#FC9838"); isAntiAlias = false }
    private val bombPaint = Paint().apply { color = Color.parseColor("#D82800"); isAntiAlias = false }
    private val exploPaint = Paint().apply { color = Color.parseColor("#F8D878"); isAntiAlias = false }
    private val enemyPaint = Paint().apply { color = Color.parseColor("#00A800"); isAntiAlias = false }
    private val itemPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); isAntiAlias = false }
    private val exitPaint = Paint().apply { color = Color.parseColor("#55EFC4"); isAntiAlias = false }
    private val textPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 14f; isAntiAlias = false; textAlign = Paint.Align.CENTER }
    private val smallPaint = Paint().apply { color = Color.parseColor("#F8F8F8"); textSize = 12f; isAntiAlias = false; textAlign = Paint.Align.CENTER }

    override fun init(width: Int, height: Int) { w = width; h = height; cell = (w / cols).coerceAtMost(h / rows).toFloat(); reset() }

    override fun reset() {
        for (r in 0 until rows) for (c in 0 until cols) {
            grid[r][c] = if (r == 0 || c == 0 || r == rows - 1 || c == cols - 1) 1
            else if (r % 2 == 0 && c % 2 == 0) 1
            else if (Random.nextFloat() < 0.55f && !(r <= 2 && c <= 2)) 2 else 0
        }
        grid[1][1] = 0; grid[1][2] = 0; grid[2][1] = 0
        px = 1; py = 1; bombs.clear(); explosions.clear(); enemies.clear(); items.clear()
        bombsMax = 1; fireRange = 2; speed = 1f; lives = 3; invuln = 0f; score = 0; isGameOver = false; won = false; stage = 1
        // exit hidden in a soft block
        val softs = mutableListOf<Pair<Int, Int>>()
        for (r in 1 until rows - 1) for (c in 1 until cols - 1) if (grid[r][c] == 2) softs.add(Pair(c, r))
        if (softs.isNotEmpty()) {
            val e = softs[Random.nextInt(softs.size)]
            exitX = e.first; exitY = e.second
        } else { exitX = cols - 2; exitY = rows - 2; grid[exitY][exitX] = 2 }
        // enemies
        val enemyCount = 3 + stage
        for (i in 0 until enemyCount) {
            var ex: Int; var ey: Int
            do { ex = Random.nextInt(1, cols - 1); ey = Random.nextInt(1, rows - 1) } while (grid[ey][ex] != 0 || (ex <= 2 && ey <= 2))
            enemies.add(Enemy(ex, ey, Random.nextInt(3), Random.nextFloat() * 0.5f, Random.nextInt(4)))
        }
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
        moveTimer += dt * speed
        if (moveTimer >= moveInterval && (nextDx != 0 || nextDy != 0)) {
            moveTimer = 0f
            val nx = px + nextDx; val ny = py + nextDy
            if (nx in 1 until cols - 1 && ny in 1 until rows - 1 && grid[ny][nx] != 1 && grid[ny][nx] != 2 && bombs.none { it.gx == nx && it.gy == ny }) {
                px = nx; py = ny
            }
        }
        // place bomb
        if (input.a) {
            if (bombs.count { it.gx == px && it.gy == py } < 1 && bombs.size < bombsMax) {
                bombs.add(Bomb(px, py, 2.5f, fireRange))
            }
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
                for (d in listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))) {
                    for (s in 1..b.range) {
                        val cx = b.gx + d.first * s; val cy = b.gy + d.second * s
                        if (cx !in 0 until cols || cy !in 0 until rows) break
                        if (grid[cy][cx] == 1) break
                        if (grid[cy][cx] == 2) {
                            grid[cy][cx] = 0
                            score += 5
                            // reveal exit
                            if (cx == exitX && cy == exitY) exitRevealed = true
                            // item drop
                            if (Random.nextFloat() < 0.4f) {
                                val t = Random.nextInt(4)
                                items.add(Item(cx, cy, t))
                            }
                            cells.add(Pair(cx, cy)); break
                        }
                        cells.add(Pair(cx, cy))
                    }
                }
                for (c in cells) explosions.add(Explosion(c.first, c.second, 0.4f))
                // kill enemies
                val killed = enemies.count { e -> cells.any { it.first == e.gx && it.second == e.gy } }
                enemies.removeAll { e -> cells.any { it.first == e.gx && it.second == e.gy } }
                score += killed * 100
                // damage player
                if (cells.any { it.first == px && it.second == py } && invuln <= 0f) {
                    lives--; invuln = 2f
                    if (lives <= 0) isGameOver = true
                }
                bi.remove()
            }
        }
        // update explosions
        explosions.removeAll { it.timer <= 0f }
        for (e in explosions) e.timer -= dt
        // update enemies
        for (e in enemies) {
            e.moveTimer -= dt
            if (e.moveTimer <= 0f) {
                e.moveTimer = 0.5f + Random.nextFloat() * 0.3f
                val dirs = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
                // chase player sometimes
                if (Random.nextFloat() < 0.6f) {
                    val dx = (px - e.gx).coerceIn(-1, 1); val dy = (py - e.gy).coerceIn(-1, 1)
                    val nx = e.gx + dx; val ny = e.gy + dy
                    if (nx in 1 until cols - 1 && ny in 1 until rows - 1 && grid[ny][nx] != 1 && grid[ny][nx] != 2) {
                        e.gx = nx; e.gy = ny
                    }
                } else {
                    val d = dirs[Random.nextInt(4)]
                    val nx = e.gx + d.first; val ny = e.gy + d.second
                    if (nx in 1 until cols - 1 && ny in 1 until rows - 1 && grid[ny][nx] != 1 && grid[ny][nx] != 2) {
                        e.gx = nx; e.gy = ny
                    }
                }
            }
            if (e.gx == px && e.gy == py && invuln <= 0f) {
                lives--; invuln = 2f
                if (lives <= 0) isGameOver = true
            }
        }
        // items pickup
        val ii = items.iterator()
        while (ii.hasNext()) {
            val it = ii.next()
            if (it.gx == px && it.gy == py) {
                when (it.type) {
                    0 -> bombsMax = (bombsMax + 1).coerceAtMost(5)
                    1 -> fireRange = (fireRange + 1).coerceAtMost(6)
                    2 -> speed = (speed + 0.2f).coerceAtMost(2f)
                    3 -> { /* kick placeholder */ }
                }
                score += 20
                ii.remove()
            }
        }
        // win: all enemies dead and exit reached
        if (enemies.isEmpty() && exitRevealed) {
            // if player on exit -> next stage
            if (px == exitX && py == exitY) {
                stage++
                if (stage > 5) { won = true; isGameOver = true; score += 1000 }
                else reset()
            }
        }
        if (score > highScore) highScore = score
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#000000"))
        val ox = (w - cols * cell) / 2f
        val oy = (h - rows * cell) / 2f
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = ox + c * cell; val y = oy + r * cell
            when (grid[r][c]) {
                1 -> canvas.drawRect(x, y, x + cell - 1, y + cell - 1, wallPaint)
                2 -> canvas.drawRect(x + 2, y + 2, x + cell - 3, y + cell - 3, softPaint)
                3 -> canvas.drawRect(x + 2, y + 2, x + cell - 3, y + cell - 3, exitPaint)
            }
            // grid lines
            val gp = Paint().apply { color = Color.parseColor("#1A1A2E") }
            canvas.drawRect(x, y, x + 1, y + cell, gp)
        }
        // exit
        if (exitRevealed) {
            val x = ox + exitX * cell; val y = oy + exitY * cell
            canvas.drawRect(x + 2, y + 2, x + cell - 3, y + cell - 3, exitPaint)
        }
        // items
        for (it in items) {
            val x = ox + it.gx * cell; val y = oy + it.gy * cell
            itemPaint.color = when (it.type) { 0 -> Color.parseColor("#D82800"); 1 -> Color.parseColor("#FC9838"); 2 -> Color.parseColor("#5C94FC"); else -> Color.parseColor("#00A800") }
            canvas.drawCircle(x + cell / 2, y + cell / 2, cell / 4, itemPaint)
        }
        // bombs
        for (b in bombs) {
            val x = ox + b.gx * cell; val y = oy + b.gy * cell
            bombPaint.color = if ((b.timer * 4).toInt() % 2 == 0) Color.parseColor("#D82800") else Color.parseColor("#F8D878")
            canvas.drawCircle(x + cell / 2, y + cell / 2, cell / 3, bombPaint)
        }
        // explosions
        for (e in explosions) {
            val x = ox + e.gx * cell; val y = oy + e.gy * cell
            exploPaint.alpha = (e.timer / 0.4f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(x + 2, y + 2, x + cell - 3, y + cell - 3, exploPaint)
        }
        exploPaint.alpha = 255
        // enemies
        for (e in enemies) {
            val x = ox + e.gx * cell; val y = oy + e.gy * cell
            enemyPaint.color = when (e.type) { 0 -> Color.parseColor("#00A800"); 1 -> Color.parseColor("#D82800"); else -> Color.parseColor("#FC9838") }
            canvas.drawOval(RectF(x + 3, y + 3, x + cell - 3, y + cell - 3), enemyPaint)
        }
        // player
        if (invuln <= 0f || (invuln * 8).toInt() % 2 == 0) {
            val x = ox + px * cell; val y = oy + py * cell
            canvas.drawRoundRect(RectF(x + 3, y + 3, x + cell - 3, y + cell - 3), 3f, 3f, playerPaint)
        }
        // HUD
        textPaint.textSize = 12f
        canvas.drawText("Score: $score  Vidas: $lives  Bombas: $bombsMax  Fuego: $fireRange", w / 2f, 14f, textPaint)
        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Etapa: $stage  Enemigos: ${enemies.size}", 8f, h - 8f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
        if (isGameOver) {
            val msg = if (won) "¡GANASTE!" else "GAME OVER"
            val col = if (won) Color.parseColor("#00A800") else Color.parseColor("#D82800")
            textPaint.textSize = 32f; textPaint.color = col
            canvas.drawText(msg, w / 2f, h / 2f, textPaint)
            textPaint.textSize = 12f; textPaint.color = Color.parseColor("#F8F8F8")
            canvas.drawText("Pulsa START", w / 2f, h / 2f + 20f, smallPaint.apply { textSize = 14f; color = Color.parseColor("#F8F8F8"); textAlign = Paint.Align.CENTER })
        }
    }
}