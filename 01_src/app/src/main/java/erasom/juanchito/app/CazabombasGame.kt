package erasom.juanchito.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Cazabombas: reconstrucción fiel de Bomber 2 (Kevin Yank, 2008).
 *
 * Mecánica original:
 *  · Colocar bombas para destruir bloques y enemigos.
 *  · 5 niveles con terrain diferente.
 *  · Power-ups: más bombas, más rango, velocidad.
 *  · Enemigos con IA básica.
 *  · High score.
 */
class CazabombasGame : Game {
    override val name = "Cazabombas"
    override val genre = "Arcade"

    private var w = 0
    private var h = 0

    // Grid
    private val tileSize = 40f
    private var cols = 0
    private var rows = 0
    private var gridX = 0f
    private var gridY = 0f

    // Grid: 0=empty, 1=wall (indestructible), 2=block (destructible), 3=bomb, 4=explosion, 5=player, 6=enemy
    private val grid = mutableListOf<MutableList<Int>>()

    // Jugador
    private var playerR = 1
    private var playerC = 1
    private var playerSpeed = 0f
    private var moveTimer = 0f
    private var moveInterval = 0.15f
    private var bombs = 1
    private var bombRange = 2
    private var maxBombs = 1
    private var activeBombs = 0

    // Bombas
    private data class Bomb(val r: Int, val c: Int, var timer: Float)
    private val bombList = mutableListOf<Bomb>()

    // Explosiones
    private data class Explosion(val r: Int, val c: Int, var timer: Float)
    private val explosions = mutableListOf<Explosion>()

    // Enemigos
    private data class Enemy(var r: Int, var c: Int, var timer: Float, var dir: Int)
    private val enemies = mutableListOf<Enemy>()

    // Power-ups
    private data class PowerUp(val r: Int, val c: Int, val type: Int)  // 0=bomb+, 1=range+, 2=speed+
    private val powerUps = mutableListOf<PowerUp>()

    // Estado
    private enum class State { PLAYING, GAME_OVER, LEVEL_COMPLETE }
    private var state = State.PLAYING

    // Puntuación
    private var scoreVal = 0
    override val score: Int get() = scoreVal
    private var best = 0
    private var level = 1
    private var maxLevel = 5

    // Animación
    private var bombFlash = 0f

    // Paints
    private val bgPaint = paintOf("#1A1A2E")
    private val wallPaint = paintOf("#7C7C7C")
    private val wallDark = paintOf("#4A4A4A")
    private val blockPaint = paintOf("#8B4513")
    private val blockDark = paintOf("#5A2A0A")
    private val floorPaint = paintOf("#2A2A3E")
    private val playerPaint = paintOf("#5C94FC")
    private val enemyPaint = paintOf("#D82800")
    private val bombPaint = paintOf("#2A2A2A")
    private val explosionPaint = paintOf("#FF5252")
    private val powerupPaint = paintOf("#00E676")
    private val textPaint = paintOf("#F8F8F8").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titlePaint = paintOf("#F5C542").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallPaint = paintOf("#B8B8C8").apply { textAlign = Paint.Align.CENTER }
    private val hudPaint = paintOf("#F5C542").apply { textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val greenPaint = paintOf("#00E676").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val redPaint = paintOf("#FF5252").apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private fun paintOf(hex: String): Paint = Paint().apply {
        setColor(Color.parseColor(hex)); isAntiAlias = true
    }

    override fun init(width: Int, height: Int) {
        w = width; h = height
        cols = (w / tileSize).toInt().coerceAtLeast(10)
        rows = ((h - 60f) / tileSize).toInt().coerceAtLeast(8)
        gridX = (w - cols * tileSize) / 2f
        gridY = 40f
        reset()
    }

    override fun reset() {
        state = State.PLAYING
        scoreVal = 0
        level = 1
        playerR = 1
        playerC = 1
        bombs = 1
        bombRange = 2
        maxBombs = 1
        activeBombs = 0
        bombList.clear()
        explosions.clear()
        enemies.clear()
        powerUps.clear()
        moveTimer = 0f
        generateLevel()
    }

    private fun generateLevel() {
        grid.clear()
        for (r in 0 until rows) {
            val row = mutableListOf<Int>()
            for (c in 0 until cols) {
                when {
                    r == 0 || r == rows - 1 || c == 0 || c == cols - 1 -> row.add(1)  // paredes
                    r % 2 == 0 && c % 2 == 0 -> row.add(1)  // paredes internas
                    else -> row.add(0)  // espacio vacío
                }
            }
            grid.add(row)
        }

        // Bloques destructibles
        for (r in 1 until rows - 1) for (c in 1 until cols - 1) {
            if (grid[r][c] == 0 && !(r == 1 && c == 1) && !(r == 1 && c == 2) && !(r == 2 && c == 1)) {
                if (Random.nextFloat() < 0.6f) grid[r][c] = 2
            }
        }

        // Enemigos
        val numEnemies = level + 1
        for (i in 0 until numEnemies) {
            var r: Int; var c: Int
            do {
                r = Random.nextInt(1, rows - 1)
                c = Random.nextInt(1, cols - 1)
            } while (grid[r][c] != 0)
            enemies.add(Enemy(r, c, 0f, Random.nextInt(4)))
            grid[r][c] = 6
        }

        // Power-ups ocultos en bloques
        for (i in 0 until 3) {
            var r: Int; var c: Int
            do {
                r = Random.nextInt(1, rows - 1)
                c = Random.nextInt(1, cols - 1)
            } while (grid[r][c] != 2)
            // Marcar bloque con power-up (se revela al destruir)
        }

        playerR = 1
        playerC = 1
        grid[1][1] = 5
    }

    override fun update(dt: Float, input: GameInput) {
        if (state != State.PLAYING) {
            if (input.start || input.a) {
                if (state == State.GAME_OVER) reset()
                else if (state == State.LEVEL_COMPLETE) {
                    level++
                    if (level > maxLevel) state = State.GAME_OVER
                    else { generateLevel(); state = State.PLAYING }
                }
            }
            return
        }

        // Mover bombas
        val bombIt = bombList.iterator()
        while (bombIt.hasNext()) {
            val bomb = bombIt.next()
            bomb.timer -= dt
            if (bomb.timer <= 0f) {
                // Explotar
                explode(bomb.r, bomb.c)
                bombIt.remove()
                activeBombs--
            }
        }

        // Mover explosiones
        val expIt = explosions.iterator()
        while (expIt.hasNext()) {
            val exp = expIt.next()
            exp.timer -= dt
            if (exp.timer <= 0f) expIt.remove()
        }

        // Mover enemigos
        for (enemy in enemies) {
            enemy.timer += dt
            if (enemy.timer > 0.5f) {
                enemy.timer = 0f
                // Movimiento simple: hacia el jugador
                val dr = playerR - enemy.r
                val dc = playerC - enemy.c
                var newR = enemy.r; var newC = enemy.c
                if (Math.abs(dr) > Math.abs(dc)) {
                    newR += if (dr > 0) 1 else -1
                } else {
                    newC += if (dc > 0) 1 else -1
                }
                if (newR in 0 until rows && newC in 0 until cols && grid[newR][newC] == 0) {
                    grid[enemy.r][enemy.c] = 0
                    enemy.r = newR; enemy.c = newC
                    grid[newR][newC] = 6
                }
            }
        }

        // Colisión enemigo-jugador
        if (grid[playerR][playerC] == 6) {
            state = State.GAME_OVER
            if (scoreVal > best) best = scoreVal
            return
        }

        // Colisión explosión-enemigo
        for (exp in explosions) {
            for (enemy in enemies.toList()) {
                if (exp.r == enemy.r && exp.c == enemy.c) {
                    enemies.remove(enemy)
                    grid[enemy.r][enemy.c] = 0
                    scoreVal += 100
                    if (scoreVal > best) best = scoreVal
                    // Power-up drop
                    if (Random.nextFloat() < 0.3f) {
                        powerUps.add(PowerUp(enemy.r, enemy.c, Random.nextInt(3)))
                    }
                }
            }
        }

        // Colisión explosión-bloques
        for (exp in explosions) {
            if (exp.r in 0 until rows && exp.c in 0 until cols) {
                if (grid[exp.r][exp.c] == 2) {
                    grid[exp.r][exp.c] = 0
                    scoreVal += 10
                    // Verificar power-up
                    powerUps.filter { it.r == exp.r && it.c == exp.c }.forEach { pu ->
                        when (pu.type) {
                            0 -> maxBombs++
                            1 -> bombRange++
                            2 -> moveInterval = (moveInterval * 0.9f).coerceAtLeast(0.08f)
                        }
                        powerUps.remove(pu)
                    }
                }
            }
        }

        // Colisión explosión-jugador
        for (exp in explosions) {
            if (exp.r == playerR && exp.c == playerC) {
                state = State.GAME_OVER
                if (scoreVal > best) best = scoreVal
                return
            }
        }

        // Power-ups
        for (pu in powerUps) {
            if (pu.r == playerR && pu.c == playerC) {
                when (pu.type) {
                    0 -> maxBombs++
                    1 -> bombRange++
                    2 -> moveInterval = (moveInterval * 0.9f).coerceAtLeast(0.08f)
                }
                powerUps.remove(pu)
                scoreVal += 50
            }
        }

        // Input: movimiento
        moveTimer += dt
        if (moveTimer >= moveInterval) {
            moveTimer = 0f
            var newR = playerR; var newC = playerC
            if (input.up) newR--
            if (input.down) newR++
            if (input.left) newC--
            if (input.right) newC++
            if (newR != playerR || newC != playerC) {
                if (newR in 0 until rows && newC in 0 until cols && grid[newR][newC] in listOf(0, 6)) {
                    grid[playerR][playerC] = if (grid[playerR][playerC] == 5) 0 else grid[playerR][playerC]
                    playerR = newR; playerC = newC
                    if (grid[playerR][playerC] != 6) grid[playerR][playerC] = 5
                }
            }
        }

        // Colocar bomba (A)
        if (input.a && activeBombs < maxBombs) {
            if (grid[playerR][playerC] != 3) {
                bombList.add(Bomb(playerR, playerC, 2f))
                grid[playerR][playerC] = 3
                activeBombs++
            }
            input.a = false
        }

        // Verificar nivel completo (todos los enemigos eliminados)
        if (enemies.isEmpty() && bombList.isEmpty() && explosions.isEmpty()) {
            state = State.LEVEL_COMPLETE
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        // Grid
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = gridX + c * tileSize
            val y = gridY + r * tileSize
            val pad = 2f

            // Fondo de celda
            canvas.drawRect(x + pad, y + pad, x + tileSize - pad, y + tileSize - pad, floorPaint)

            // Contenido
            when (grid[r][c]) {
                1 -> { // Pared
                    canvas.drawRect(x + pad, y + pad, x + tileSize - pad, y + tileSize - pad, wallPaint)
                    val wd = paintOf("#4A4A4A")
                    canvas.drawRect(x + pad, y + pad, x + tileSize - pad, y + tileSize / 2f, wd)
                }
                2 -> { // Bloque
                    canvas.drawRoundRect(RectF(x + pad + 2f, y + pad + 2f, x + tileSize - pad - 2f, y + tileSize - pad - 2f), 4f, 4f, blockPaint)
                    val bd = paintOf("#5A2A0A")
                    canvas.drawRect(x + pad + 4f, y + pad + 2f, x + tileSize - pad - 4f, y + tileSize / 2f, bd)
                }
                3 -> { // Bomba
                    canvas.drawCircle(x + tileSize / 2f, y + tileSize / 2f, tileSize / 3f, bombPaint)
                    val fuse = paintOf("#F5C542")
                    canvas.drawRect(x + tileSize / 2f - 2f, y + 4f, x + tileSize / 2f + 2f, y + tileSize / 3f, fuse)
                }
                5 -> { // Jugador
                    canvas.drawRoundRect(RectF(x + pad + 4f, y + pad + 4f, x + tileSize - pad - 4f, y + tileSize - pad - 4f), 4f, 4f, playerPaint)
                    // Ojos
                    val eye = paintOf("#FFFFFF")
                    canvas.drawCircle(x + tileSize / 2f - 5f, y + tileSize / 2f - 2f, 3f, eye)
                    canvas.drawCircle(x + tileSize / 2f + 5f, y + tileSize / 2f - 2f, 3f, eye)
                }
                6 -> { // Enemigo
                    canvas.drawRoundRect(RectF(x + pad + 4f, y + pad + 4f, x + tileSize - pad - 4f, y + tileSize - pad - 4f), 4f, 4f, enemyPaint)
                    val eye = paintOf("#FFFFFF")
                    canvas.drawCircle(x + tileSize / 2f - 5f, y + tileSize / 2f - 2f, 3f, eye)
                    canvas.drawCircle(x + tileSize / 2f + 5f, y + tileSize / 2f - 2f, 3f, eye)
                    val pupil = paintOf("#000000")
                    canvas.drawCircle(x + tileSize / 2f - 5f, y + tileSize / 2f - 2f, 1.5f, pupil)
                    canvas.drawCircle(x + tileSize / 2f + 5f, y + tileSize / 2f - 2f, 1.5f, pupil)
                }
            }

            // Explosiones
            for (exp in explosions) {
                if (exp.r == r && exp.c == c) {
                    val alpha = (exp.timer / 0.5f * 200).toInt().coerceIn(0, 200)
                    val expPaint = Paint().apply { color = Color.parseColor("#FF5252"); this.alpha = alpha; isAntiAlias = false }
                    canvas.drawCircle(x + tileSize / 2f, y + tileSize / 2f, tileSize / 2f * (1f - exp.timer / 0.5f) + 4f, expPaint)
                }
            }

            // Power-ups
            for (pu in powerUps) {
                if (pu.r == r && pu.c == c) {
                    canvas.drawCircle(x + tileSize / 2f, y + tileSize / 2f, 8f, powerupPaint)
                    val label = when (pu.type) { 0 -> "B"; 1 -> "R"; else -> "S" }
                    val labelPaint = paintOf("#FFFFFF")
                    labelPaint.textSize = 10f
                    labelPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(label, x + tileSize / 2f, y + tileSize / 2f + 4f, labelPaint)
                }
            }
        }

        // HUD
        hudPaint.textSize = 18f
        canvas.drawText("SCORE: $scoreVal", 10f, 25f, hudPaint)
        canvas.drawText("BEST: $best", w - 10f - hudPaint.measureText("BEST: $best"), 25f, hudPaint)
        canvas.drawText("NIVEL: $level", w / 2f - 40f, 25f, hudPaint.apply { textAlign = Paint.Align.CENTER })
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("BOMBAS: $activeBombs/$maxBombs  ·  RANGO: $bombRange", w / 2f - 80f, 50f, smallPaint.apply { textAlign = Paint.Align.CENTER; textSize = 14f })
        smallPaint.textAlign = Paint.Align.CENTER

        // Instrucciones
        smallPaint.textSize = 12f
        smallPaint.color = Color.parseColor("#B8B8C8")
        canvas.drawText("D-pad: mover  ·  A: colocar bomba  ·  Destruye todos los enemigos", w / 2f, h - 10f, smallPaint)

        // Overlay de nivel completo
        if (state == State.LEVEL_COMPLETE) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            titlePaint.textSize = 42f
            canvas.drawText("¡NIVEL COMPLETO!", w / 2f, h / 2f - 30f, titlePaint)
            smallPaint.textSize = 20f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA SIGUIENTE NIVEL", w / 2f, h / 2f + 50f, greenPaint)
        }

        // Overlay de game over
        if (state == State.GAME_OVER) {
            val dim = Paint().apply { color = Color.parseColor("#000000"); alpha = 160; isAntiAlias = false }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dim)
            redPaint.textSize = 48f
            canvas.drawText("GAME OVER", w / 2f, h / 2f - 30f, redPaint)
            smallPaint.textSize = 22f
            canvas.drawText("Puntuación: $scoreVal", w / 2f, h / 2f + 10f, smallPaint)
            if (scoreVal >= best && scoreVal > 0) {
                titlePaint.textSize = 22f
                canvas.drawText("¡NUEVO RÉCORD!", w / 2f, h / 2f + 40f, titlePaint)
            }
            greenPaint.textSize = 20f
            canvas.drawText("PULSA START PARA JUGAR OTRA VEZ", w / 2f, h / 2f + 80f, greenPaint)
        }
    }

    private fun explode(r: Int, c: Int) {
        explosions.add(Explosion(r, c, 0.5f))
        // Propagar en 4 direcciones
        for (d in 0..3) {
            var cr = r; var cc = c
            for (i in 1..bombRange) {
                cr += dr(d); cc += dc(d)
                if (cr !in 0 until rows || cc !in 0 until cols) break
                if (grid[cr][cc] == 1) break  // pared indestructible
                explosions.add(Explosion(cr, cc, 0.5f))
                if (grid[cr][cc] == 2) break  // bloque destructible (se detiene)
            }
        }
    }

    private fun dr(d: Int) = when (d) { 0 -> -1; 2 -> 1; else -> 0 }
    private fun dc(d: Int) = when (d) { 3 -> -1; 1 -> 1; else -> 0 }

    override val isGameOver: Boolean get() = state == State.GAME_OVER
}
